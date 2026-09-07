package net.njw.justdragoneggs.event;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.njw.justdragoneggs.JustDragonEggs;
import net.njw.justdragoneggs.dragon.DamageMethod;
import net.njw.justdragoneggs.dragon.DragonBattleRecord;
import net.njw.justdragoneggs.dragon.DragonCombatTracker;
import net.njw.justdragoneggs.dragon.OtherDamageMethod;
import net.njw.justdragoneggs.state.DragonWorldData;

@EventBusSubscriber(modid = JustDragonEggs.MODID)
public final class DragonCombatEvents {
    private static final Map<UUID, DragonCombatTracker> ACTIVE = new HashMap<>();
    private static final Map<UUID, PendingDeath> PENDING_DEATHS = new HashMap<>();
    private static final Map<UUID, RecentPlayerAction> CRYSTAL_ATTACKERS = new HashMap<>();
    private static final Map<UUID, Float> HEALTH_BEFORE_TICK = new HashMap<>();
    private static final Deque<ExplosionTrigger> BAD_RESPAWN_TRIGGERS = new ArrayDeque<>();
    private static final long ACTION_TTL = 2;
    private static final double BAD_RESPAWN_SOURCE_DISTANCE_SQR = 0.01;

    private DragonCombatEvents() {}

    @SubscribeEvent
    public static void onDragonDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof EnderDragon dragon) || !(dragon.level() instanceof ServerLevel level)) return;
        double damage = event.getHealthDamage();
        DragonCombatTracker tracker = ACTIVE.computeIfAbsent(dragon.getUUID(), DragonCombatTracker::new);
        if (damage > 0) {
            Attribution attribution = resolveAttribution(level, event.getSource());
            if (attribution != null) tracker.addPlayerDamage(attribution.playerUuid(), attribution.playerName(), attribution.method(), attribution.itemId(), damage);
            else tracker.addOtherDamage(resolveOtherMethod(event.getSource()), damage);
        }
        PendingDeath pending = PENDING_DEATHS.remove(dragon.getUUID());
        if (pending != null) finishBattle(level, dragon, tracker, pending);
    }

    @SubscribeEvent
    public static void onDragonTickPre(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof EnderDragon dragon && dragon.level() instanceof ServerLevel) HEALTH_BEFORE_TICK.put(dragon.getUUID(), dragon.getHealth());
    }

    @SubscribeEvent
    public static void onDragonTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof EnderDragon dragon) || !(dragon.level() instanceof ServerLevel)) return;
        Float before = HEALTH_BEFORE_TICK.remove(dragon.getUUID());
        if (before == null) return;
        double healing = dragon.getHealth() - before;
        if (healing > 0) ACTIVE.computeIfAbsent(dragon.getUUID(), DragonCombatTracker::new).addHealing(healing);
    }

    @SubscribeEvent
    public static void onDragonDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon) || !(dragon.level() instanceof ServerLevel level)) return;
        HEALTH_BEFORE_TICK.remove(dragon.getUUID());
        Attribution killerAttribution = resolveAttribution(level, event.getSource());
        Optional<UUID> killerUuid = killerAttribution == null ? Optional.empty() : Optional.of(killerAttribution.playerUuid());
        Optional<String> killerName = killerAttribution == null ? Optional.empty() : Optional.of(killerAttribution.playerName());
        PENDING_DEATHS.put(dragon.getUUID(), new PendingDeath(killerUuid, killerName, System.currentTimeMillis()));
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Player player = event.getEntity();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        DamageMethod method;

        if (state.getBlock() instanceof BedBlock) {
            if (state.getValue(BedBlock.PART) != BedPart.HEAD) {
                pos = pos.relative(state.getValue(BedBlock.FACING));
                state = level.getBlockState(pos);
                if (!(state.getBlock() instanceof BedBlock)) return;
            }
            if (!level.environmentAttributes().getValue(EnvironmentAttributes.BED_RULE, pos).explodes()) return;
            method = DamageMethod.BED;
        } else if (state.is(Blocks.RESPAWN_ANCHOR)) {
            int charge = state.getValue(RespawnAnchorBlock.CHARGE);
            if (charge == 0 || RespawnAnchorBlock.canSetSpawn(level, pos)) return;
            if (charge < RespawnAnchorBlock.MAX_CHARGES && (player.getMainHandItem().is(Items.GLOWSTONE) || player.getOffhandItem().is(Items.GLOWSTONE))) return;
            method = DamageMethod.RESPAWN_ANCHOR;
        } else {
            return;
        }

        rememberBadRespawnTrigger(new ExplosionTrigger(level.dimension(), pos, player.getUUID(), player.getName().getString(), method, level.getGameTime()));
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof EndCrystal crystal) || !(event.getEntity().level() instanceof ServerLevel level)) return;
        Player player = event.getEntity();
        CRYSTAL_ATTACKERS.put(crystal.getUUID(), new RecentPlayerAction(player.getUUID(), player.getName().getString(), level.getGameTime()));
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof EndCrystal crystal)) return;
        Projectile projectile = event.getProjectile();
        if (!(projectile.level() instanceof ServerLevel level) || !(projectile.getOwner() instanceof Player player)) return;
        CRYSTAL_ATTACKERS.put(crystal.getUUID(), new RecentPlayerAction(player.getUUID(), player.getName().getString(), level.getGameTime()));
    }

    private static void finishBattle(ServerLevel level, EnderDragon dragon, DragonCombatTracker tracker, PendingDeath pending) {
        ACTIVE.remove(dragon.getUUID());
        HEALTH_BEFORE_TICK.remove(dragon.getUUID());
        DragonWorldData data = DragonWorldData.get(level);
        int dragonNumber = data.nextDragonNumber();
        DragonBattleRecord record = tracker.finish(dragonNumber, pending.killerUuid(), pending.killerName(), pending.killedAt(), dragon.getMaxHealth());
        data.addRecord(record);
        JustDragonEggs.LOGGER.info("Saved Ender Dragon battle #{}: killer={}, playerDamage={}, otherDamage={}, healing={}, balance={}", dragonNumber, pending.killerName().orElse("unknown"), record.totalPlayerDamage(), record.totalOtherDamage(), record.totalHealing(), record.healthBalance());
    }

    private static Attribution resolveAttribution(ServerLevel level, DamageSource source) {
        long tick = level.getGameTime();
        if (source.is(DamageTypes.BAD_RESPAWN_POINT)) {
            ExplosionTrigger trigger = findBadRespawnTrigger(level, source, tick);
            if (trigger != null) return new Attribution(trigger.playerUuid(), trigger.playerName(), trigger.method(), Optional.empty());
        }

        Entity direct = source.getDirectEntity();
        Entity causing = source.getEntity();

        if (direct instanceof EndCrystal crystal) {
            CRYSTAL_ATTACKERS.entrySet().removeIf(entry -> tick - entry.getValue().tick() > ACTION_TTL);
            RecentPlayerAction action = CRYSTAL_ATTACKERS.get(crystal.getUUID());
            if (action != null && tick - action.tick() >= 0 && tick - action.tick() <= ACTION_TTL) return new Attribution(action.playerUuid(), action.playerName(), DamageMethod.END_CRYSTAL, Optional.empty());
            return null;
        }

        if (direct instanceof PrimedTnt tnt) {
            if (causing instanceof Player player) return attribution(player, DamageMethod.TNT);
            if (tnt.getOwner() instanceof Player player) return attribution(player, DamageMethod.TNT);
            return null;
        }

        if (source.is(DamageTypes.THORNS) && causing instanceof Player player) return attribution(player, DamageMethod.THORNS);
        if (direct instanceof FireworkRocketEntity && causing instanceof Player player) return attribution(player, DamageMethod.FIREWORK);
        if (direct instanceof Projectile projectile) {
            if (projectile.getOwner() instanceof Player player) return attribution(player, DamageMethod.PROJECTILE, weaponItemId(source));
            if (causing instanceof Player player) return attribution(player, DamageMethod.PROJECTILE, weaponItemId(source));
        }
        if (causing instanceof Player player) {
            if (direct == player && source.is(DamageTypeTags.IS_PLAYER_ATTACK)) return attribution(player, DamageMethod.DIRECT, weaponItemId(source));
            return attribution(player, DamageMethod.OTHER_PLAYER);
        }
        return null;
    }

    private static OtherDamageMethod resolveOtherMethod(DamageSource source) {
        if (source.is(DamageTypes.BAD_RESPAWN_POINT)) return OtherDamageMethod.BAD_RESPAWN_POINT;
        Entity direct = source.getDirectEntity();
        Entity causing = source.getEntity();
        if (direct instanceof PrimedTnt) return OtherDamageMethod.TNT;
        if (direct instanceof EndCrystal) return OtherDamageMethod.END_CRYSTAL;
        if (source.is(DamageTypeTags.IS_EXPLOSION)) return OtherDamageMethod.EXPLOSION;
        if (causing instanceof LivingEntity) return OtherDamageMethod.MOB;
        if (causing == null && direct == null) return OtherDamageMethod.ENVIRONMENT;
        return OtherDamageMethod.UNKNOWN;
    }

    private static Optional<Identifier> weaponItemId(DamageSource source) {
        ItemStack item = source.getWeaponItem();
        if (item == null || item.isEmpty()) return Optional.empty();
        return Optional.of(BuiltInRegistries.ITEM.getKey(item.getItem()));
    }

    private static Attribution attribution(Player player, DamageMethod method) {
        return attribution(player, method, Optional.empty());
    }

    private static Attribution attribution(Player player, DamageMethod method, Optional<Identifier> itemId) {
        return new Attribution(player.getUUID(), player.getName().getString(), method, itemId);
    }

    private static void rememberBadRespawnTrigger(ExplosionTrigger trigger) {
        BAD_RESPAWN_TRIGGERS.removeIf(existing -> existing.dimension().equals(trigger.dimension()) && existing.pos().equals(trigger.pos()) && existing.playerUuid().equals(trigger.playerUuid()) && existing.tick() == trigger.tick());
        BAD_RESPAWN_TRIGGERS.addLast(trigger);
        while (BAD_RESPAWN_TRIGGERS.size() > 32) BAD_RESPAWN_TRIGGERS.removeFirst();
    }

    private static ExplosionTrigger findBadRespawnTrigger(ServerLevel level, DamageSource source, long tick) {
        BAD_RESPAWN_TRIGGERS.removeIf(trigger -> tick - trigger.tick() > ACTION_TTL);
        Vec3 sourcePos = source.getSourcePosition();
        if (sourcePos == null) return null;
        ExplosionTrigger match = null;
        for (ExplosionTrigger trigger : BAD_RESPAWN_TRIGGERS) {
            long age = tick - trigger.tick();
            if (age < 0 || age > ACTION_TTL || !trigger.dimension().equals(level.dimension())) continue;
            if (trigger.pos().getCenter().distanceToSqr(sourcePos) > BAD_RESPAWN_SOURCE_DISTANCE_SQR) continue;
            if (match != null && (!match.playerUuid().equals(trigger.playerUuid()) || match.method() != trigger.method())) return null;
            match = trigger;
        }
        return match;
    }

    private record Attribution(UUID playerUuid, String playerName, DamageMethod method, Optional<Identifier> itemId) {}
    private record PendingDeath(Optional<UUID> killerUuid, Optional<String> killerName, long killedAt) {}
    private record RecentPlayerAction(UUID playerUuid, String playerName, long tick) {}
    private record ExplosionTrigger(ResourceKey<Level> dimension, BlockPos pos, UUID playerUuid, String playerName, DamageMethod method, long tick) {}
}
