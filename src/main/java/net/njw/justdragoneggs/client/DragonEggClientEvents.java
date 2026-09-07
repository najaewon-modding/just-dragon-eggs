package net.njw.justdragoneggs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.njw.justdragoneggs.JustDragonEggs;

@EventBusSubscriber(modid = JustDragonEggs.MODID, value = Dist.CLIENT)
public final class DragonEggClientEvents {
    private DragonEggClientEvents() {}

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null) return;
        BlockPos pos = targetedDragonEgg(minecraft);
        if (pos == null) return;
        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        int centerX = minecraft.getWindow().getGuiScaledWidth() / 2;
        int centerY = minecraft.getWindow().getGuiScaledHeight() / 2;
        graphics.centeredText(minecraft.font, Component.literal("# 1"), centerX, centerY - 42, DragonEggRecordScreen.COLOR_1);
        graphics.centeredText(minecraft.font, Component.literal("JWN__"), centerX, centerY - 30, 0xFFFFFFFF);
    }

    @SubscribeEvent
    public static void onUseItem(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) return;
        if (targetedDragonEgg(minecraft) == null) return;
        minecraft.setScreen(new DragonEggRecordScreen());
        event.setSwingHand(false);
        event.setCanceled(true);
    }

    private static BlockPos targetedDragonEgg(Minecraft minecraft) {
        HitResult hit = minecraft.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return null;
        BlockPos pos = blockHit.getBlockPos();
        return minecraft.level.getBlockState(pos).is(Blocks.DRAGON_EGG) ? pos : null;
    }
}
