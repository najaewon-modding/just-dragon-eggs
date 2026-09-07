package net.njw.justdragoneggs.event;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.njw.justdragoneggs.JustDragonEggs;
import net.njw.justdragoneggs.dragon.DragonBattleRecord;
import net.njw.justdragoneggs.state.DragonWorldData;

@EventBusSubscriber(modid = JustDragonEggs.MODID)
public final class DragonEggSpawnEvents {
    private static final Map<UUID, Boolean> VANILLA_EGG_EXPECTED = new HashMap<>();

    private DragonEggSpawnEvents() {}

    @SubscribeEvent
    public static void onDragonDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon) || !(dragon.level() instanceof ServerLevel level)) return;
        EnderDragonFight fight = level.getDragonFight();
        VANILLA_EGG_EXPECTED.put(dragon.getUUID(), fight != null && !fight.hasPreviouslyKilledDragon());
    }

    @SubscribeEvent
    public static void onDragonLeaveLevel(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon) || !(event.getLevel() instanceof ServerLevel level)) return;
        DragonBattleRecord record = DragonWorldData.get(level).records().stream().filter(candidate -> candidate.dragonUuid().equals(dragon.getUUID())).findFirst().orElse(null);
        if (record == null) {
            VANILLA_EGG_EXPECTED.remove(dragon.getUUID());
            return;
        }

        boolean vanillaEggExpected = VANILLA_EGG_EXPECTED.remove(dragon.getUUID()) == Boolean.TRUE;
        if (vanillaEggExpected) {
            JustDragonEggs.LOGGER.info("Using vanilla dragon egg for battle #{}", record.dragonNumber());
            return;
        }

        BlockPos eggPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, EndPodiumFeature.getLocation(dragon.getFightOrigin()));
        if (level.setBlockAndUpdate(eggPos, Blocks.DRAGON_EGG.defaultBlockState())) {
            JustDragonEggs.LOGGER.info("Spawned dragon egg for battle #{} at {}", record.dragonNumber(), eggPos);
        } else {
            JustDragonEggs.LOGGER.warn("Failed to spawn dragon egg for battle #{} at {}", record.dragonNumber(), eggPos);
        }
    }
}
