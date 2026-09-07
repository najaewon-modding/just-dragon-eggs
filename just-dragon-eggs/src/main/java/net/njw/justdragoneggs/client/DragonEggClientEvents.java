package net.njw.justdragoneggs.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.njw.justdragoneggs.JustDragonEggs;

@EventBusSubscriber(modid = JustDragonEggs.MODID, value = Dist.CLIENT)
public final class DragonEggClientEvents {
    private DragonEggClientEvents() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().is(Items.DRAGON_EGG)) return;
        event.getToolTip().add(Component.literal("#1").withStyle(ChatFormatting.LIGHT_PURPLE));
        event.getToolTip().add(Component.literal("JWN__").withStyle(ChatFormatting.GRAY));
    }

    @SubscribeEvent
    public static void onUseItem(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.getItemInHand(event.getHand()).is(Items.DRAGON_EGG)) return;
        if (minecraft.screen == null) minecraft.setScreen(new DragonEggRecordScreen());
        event.setSwingHand(false);
        event.setCanceled(true);
    }
}
