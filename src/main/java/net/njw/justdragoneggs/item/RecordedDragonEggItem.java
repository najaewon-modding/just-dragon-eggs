package net.njw.justdragoneggs.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
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
    public RecordedDragonEggItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, display, builder, tooltipFlag);
        DragonBattleRecord record = stack.get(ModContent.BATTLE_RECORD.get());
        if (record == null) return;
        Component killer = record.killerName().<Component>map(Component::literal).orElseGet(() -> Component.translatable("screen.njw_just_dragon_eggs.unknown"));
        MutableComponent line = Component.literal("#" + record.dragonNumber() + " ").append(killer).withStyle(ChatFormatting.GRAY);
        builder.accept(line);
    }
}
