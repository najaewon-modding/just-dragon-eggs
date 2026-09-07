package net.njw.justdragoneggs.item;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.njw.justdragoneggs.dragon.DragonBattleRecord;
import net.njw.justdragoneggs.registry.ModContent;

public final class RecordedDragonEggItem extends BlockItem {
    private static final int COLOR_1 = 0xFFFFC94A;
    private static final int COLOR_2 = 0xFFF2F5F8;
    private static final int COLOR_3 = 0xFFD8894A;
    private static final int COLOR_4 = 0xFFD0D0D0;
    private static final int COLOR_5 = 0xFF9F9F9F;
    private static final int COLOR_NAME = 0xFFFFFFFF;

    public RecordedDragonEggItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, display, builder, tooltipFlag);
        DragonBattleRecord record = stack.get(ModContent.BATTLE_RECORD.get());
        if (record == null) return;
        Component killer = record.killerName().<Component>map(Component::literal).orElseGet(() -> Component.translatable("screen.njw_just_dragon_eggs.unknown"));
        MutableComponent line = Component.literal("#" + record.dragonNumber()).withColor(dragonNumberColor(record.dragonNumber())).append(Component.literal(" ")).append(killer.copy().withColor(COLOR_NAME));
        builder.accept(line);
    }

    private static int dragonNumberColor(int number) {
        if (number == 1) return COLOR_1;
        if (number == 2) return COLOR_2;
        if (number == 3) return COLOR_3;
        if (number <= 10) return COLOR_4;
        return COLOR_5;
    }
}
