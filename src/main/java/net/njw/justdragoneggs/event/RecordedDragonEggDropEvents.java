package net.njw.justdragoneggs.event;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.njw.justdragoneggs.JustDragonEggs;
import net.njw.justdragoneggs.dragon.DragonBattleRecord;
import net.njw.justdragoneggs.registry.ModContent;

@EventBusSubscriber(modid = JustDragonEggs.MODID)
public final class RecordedDragonEggDropEvents {
    private static final Deque<PendingDrop> PENDING = new ArrayDeque<>();

    private RecordedDragonEggDropEvents() {}

    public static void rememberFallingDrop(Level level, BlockPos pos, DragonBattleRecord record) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        PENDING.addLast(new PendingDrop(serverLevel.dimension(), pos.immutable(), serverLevel.getGameTime(), record));
        while (PENDING.size() > 32) PENDING.removeFirst();
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ItemEntity item)) return;
        ItemStack original = item.getItem();
        if (!original.is(ModContent.RECORDED_DRAGON_EGG_ITEM.get()) || original.get(ModContent.BATTLE_RECORD.get()) != null) return;
        long tick = level.getGameTime();
        PENDING.removeIf(drop -> tick - drop.tick() > 2);
        Iterator<PendingDrop> iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            PendingDrop drop = iterator.next();
            if (!drop.dimension().equals(level.dimension()) || Math.abs(tick - drop.tick()) > 2 || drop.pos().getCenter().distanceToSqr(item.position()) > 4.0) continue;
            ItemStack stack = original.copy();
            stack.set(ModContent.BATTLE_RECORD.get(), drop.record());
            item.setItem(stack);
            iterator.remove();
            return;
        }
    }

    private record PendingDrop(ResourceKey<Level> dimension, BlockPos pos, long tick, DragonBattleRecord record) {}
}
