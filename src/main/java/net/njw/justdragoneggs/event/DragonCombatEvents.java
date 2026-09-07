package net.njw.justdragoneggs.event;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
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
    private static final Map<UUID, RecentPlayerAction> CRYSTAL_ATTACKERS = new HashMap<>();
    private static final Map<UUID, Float> HEALTH_BEFORE_TICK = new HashMap<>();
    private static final Deque<ExplosionTrigger> BAD_RESPAWN_TRIGGERS = new ArrayDeque<>();
    private static final long ACTION_TTL = 2;
    private static final double BAD_RESPAWN_MAX_DISTANCE_SQR = 64.0;

    private DragonCombatEvents() {}

    @SubscribeEvent
    public static void onDragonDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof EnderDragon dragon) || !(dragon.level() instanceof ServerLevel level)) return;
        double damage = event.getHealthDamage();
        if (damage <= 0) return;
        DragonCombatTracker tracker = ACTIVE.computeIfAbsent(dragon.getUUID(), DragonCombatTracker::new);
        Attribution attribution = resolveAttribution(level, dragon, event.getSource());
        if (attribution != null) tracker.addPlayerDamage(attribution.playerUuid(), attribution.playerName(), attribution.method(), damage);
        else tracker.addOtherDamage(resolveOtherMethod(event.getSource()), damage);
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
        DragonCombatTracker tracker = ACTIVE.remove(dragon.getUUID());
        if (tracker == null) tracker = new DragonCombatTracker(dragon.getUUID());

        Attribution killerAttribution = resolveAttribution(level, dragon, event.getSource());
        Optional<UUID> killerUuid = Optional.empty();
        Optional<String> killerName = Optional.empty();
        if (killerAttribution != null) {
            killerUuid = Optional.of(killerAttribution.playerUuid());
            killerName = Optional.of(killerAttribution.playerName());
        } else {
            LivingEntity killCredit = dragon.getKillCredit();
            if (killCredit instanceof Player player) {
                killerUuid = Optional.of(player.getUUID());
                killerName = Optional.of(player.getName().getString());
            }
        }

        DragonWorldData data = DragonWorldData.get(level);
        int dragonNumber = data.nextDragonNumber();
        DragonBattleRecord record = tracker.finish(dragonNumber, killerUuid, killerName, System.currentTimeMillis());
        data.addRecord(record);
        JustDragonEggs.LOGGER.info("Saved Ender Dragon battle #{}: killer={}, playerDamage={}, otherDamage={}, healing={}", dragonNumber, killerName.orElse("unknown"), record.totalPlayerDamage(), record.totalOtherDamage(), record.totalHealing());
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Block block = level.getBlockState(event.getPos()).getBlock();
        DamageMethod method = block instanceof BedBlock ? DamageMethod.BED : block == Blocks.RESPAWN_ANCHOR ? DamageMethod.RESPAWN_ANCHOR : null;
        if (method == null) return;
        Player player = event.getEntity();
        rememberBadRespawnTrigger(new ExplosionTrigger(level.dimension(), event.getPos(), player.getUUID(), player.getName().getString(), method, level.getGameTime()));
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

    private static Attribution resolveAttribution(ServerLevel level, EnderDragon dragon, DamageSource source) {
        long tick = level.getGameTime();
        if (source.is(DamageTypes.BAD_RESPAWN_POINT)) {
            ExplosionTrigger trigger = findBadRespawnTrigger(level, dragon, tick);
            if (trigger != null) return new Attribution(trigger.playerUuid(), trigger.playerName(), trigger.method());
        }

        Entity direct = source.getDirectEntity();
        Entity causing = source.getEntity();

        if (direct instanceof EndCrystal crystal) {
            RecentPlayerAction action = CRYSTAL_ATTACKERS.get(crystal.getUUID());
            if (action != null && tick - action.tick() >= 0 && tick - action.tick() <= ACTION_TTL) return new Attribution(action.playerUuid(), action.playerName(), DamageMethod.END_CRYSTAL);
            if (causing instanceof Player player) return attribution(player, DamageMethod.END_CRYSTAL);
            return null;
        }

        if (direct instanceof PrimedTnt tnt) {
            if (causing instanceof Player player) return attribution(player, DamageMethod.TNT);
            if (tnt.getOwner() instanceof Player player) return attribution(player, DamageMethod.TNT);
            return null;
        }

        if (source.is(DamageTypes.THORNS) && causing instanceof Player player) return attribution(player, DamageMethod.THORNS);
        if (direct instanceof FireworkRocketEntity && causing instanceof Player player) return attribution(player, DamageMethod.FIREWORK);
        if (direct instanceof ThrownTrident && causing instanceof Player player) return attribution(player, DamageMethod.TRIDENT);
        if (direct instanceof AbstractArrow && causing instanceof Player player) return attribution(player, DamageMethod.ARROW);
        if (direct instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();
            if (owner instanceof Player player) return attribution(player, DamageMethod.PROJECTILE);
            if (causing instanceof Player player) return attribution(player, DamageMethod.PROJECTILE);
        }
        if (causing instanceof Player player) return attribution(player, direct == player ? DamageMethod.MELEE : DamageMethod.OTHER_PLAYER);
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

    private static Attribution attribution(Player player, DamageMethod method) {
        return new Attribution(player.getUUID(), player.getName().getString(), method);
    }

    private static void rememberBadRespawnTrigger(ExplosionTrigger trigger) {
        BAD_RESPAWN_TRIGGERS.removeIf(existing -> existing.dimension().equals(trigger.dimension()) && existing.pos().equals(trigger.pos()) && existing.playerUuid().equals(trigger.playerUuid()) && existing.tick() == trigger.tick());
        BAD_RESPAWN_TRIGGERS.addLast(trigger);
        while (BAD_RESPAWN_TRIGGERS.size() > 32) BAD_RESPAWN_TRIGGERS.removeFirst();
    }

    private static ExplosionTrigger findBadRespawnTrigger(ServerLevel level, EnderDragon dragon, long tick) {
        BAD_RESPAWN_TRIGGERS.removeIf(trigger -> tick - trigger.tick() > ACTION_TTL);
        ExplosionTrigger best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ExplosionTrigger trigger : BAD_RESPAWN_TRIGGERS) {
            long age = tick - trigger.tick();
            if (age < 0 || age > ACTION_TTL || !trigger.dimension().equals(level.dimension())) continue;
            double distance = trigger.pos().getCenter().distanceToSqr(dragon.position());
            if (distance <= BAD_RESPAWN_MAX_DISTANCE_SQR && distance < bestDistance) {
                best = trigger;
                bestDistance = distance;
            }
        }
        return best;
    }

    private record Attribution(UUID playerUuid, String playerName, DamageMethod method) {}
    private record RecentPlayerAction(UUID playerUuid, String playerName, long tick) {}
    private record ExplosionTrigger(ResourceKey<Level> dimension, BlockPos pos, UUID playerUuid, String playerName, DamageMethod method, long tick) {}
}
