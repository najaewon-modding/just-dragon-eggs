package net.njw.justdragoneggs.event;

import java.util.HashSet;
import java.util.Set;
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
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.njw.justdragoneggs.JustDragonEggs;
import net.njw.justdragoneggs.block.entity.RecordedDragonEggBlockEntity;
import net.njw.justdragoneggs.dragon.DragonBattleRecord;
import net.njw.justdragoneggs.registry.ModContent;
import net.njw.justdragoneggs.state.DragonWorldData;

@EventBusSubscriber(modid = JustDragonEggs.MODID)
public final class DragonEggSpawnEvents {
    private static final Set<UUID> VANILLA_EGG_EXPECTED = new HashSet<>();

    private DragonEggSpawnEvents() {}

    @SubscribeEvent
    public static void onDragonDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon) || !(dragon.level() instanceof ServerLevel level)) return;
        EnderDragonFight fight = level.getDragonFight();
        if (fight != null && !fight.hasPreviouslyKilledDragon()) VANILLA_EGG_EXPECTED.add(dragon.getUUID());
        else VANILLA_EGG_EXPECTED.remove(dragon.getUUID());
    }

    @SubscribeEvent
    public static void onDragonLeaveLevel(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon) || !(event.getLevel() instanceof ServerLevel level)) return;
        DragonBattleRecord record = DragonWorldData.get(level).records().stream().filter(candidate -> candidate.dragonUuid().equals(dragon.getUUID())).findFirst().orElse(null);
        if (record == null) {
            VANILLA_EGG_EXPECTED.remove(dragon.getUUID());
            return;
        }

        boolean vanillaEggExpected = VANILLA_EGG_EXPECTED.remove(dragon.getUUID());
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, EndPodiumFeature.getLocation(dragon.getFightOrigin()));
        BlockPos eggPos = top;
        if (vanillaEggExpected) {
            if (level.getBlockState(top.below()).is(Blocks.DRAGON_EGG)) eggPos = top.below();
            else if (level.getBlockState(top).is(Blocks.DRAGON_EGG)) eggPos = top;
        }

        if (!level.setBlockAndUpdate(eggPos, ModContent.RECORDED_DRAGON_EGG.get().defaultBlockState())) {
            JustDragonEggs.LOGGER.warn("Failed to create recorded dragon egg for battle #{} at {}", record.dragonNumber(), eggPos);
            return;
        }
        if (level.getBlockEntity(eggPos) instanceof RecordedDragonEggBlockEntity egg) {
            egg.setRecord(record);
            JustDragonEggs.LOGGER.info("Created recorded dragon egg for battle #{} at {}", record.dragonNumber(), eggPos);
        } else {
            JustDragonEggs.LOGGER.warn("Recorded dragon egg for battle #{} has no block entity at {}", record.dragonNumber(), eggPos);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        VANILLA_EGG_EXPECTED.clear();
    }
}
