package net.njw.justdragoneggs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.njw.justdragoneggs.JustDragonEggs;

@EventBusSubscriber(modid = JustDragonEggs.MODID, value = Dist.CLIENT)
public final class DragonEggClientEvents {
    private static final int FULL_BRIGHT = 0xF000F0;

    private DragonEggClientEvents() {}

    @SubscribeEvent
    public static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null) return;
        BlockPos pos = targetedDragonEgg(minecraft);
        if (pos == null) return;

        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        CameraRenderState camera = event.getLevelRenderState().cameraRenderState;
        double dx = pos.getX() + 0.5 - camera.pos.x;
        double dy = pos.getY() + 0.5 - camera.pos.y;
        double dz = pos.getZ() + 0.5 - camera.pos.z;
        double distanceToCameraSq = camera.pos.distanceToSqr(pos.getCenter());

        poseStack.pushPose();
        poseStack.translate(dx, dy, dz);
        collector.order(1).submitNameTag(poseStack, new Vec3(0, 0.94, 0), 0, Component.literal("# 1").withColor(DragonEggRecordScreen.COLOR_1), false, FULL_BRIGHT, distanceToCameraSq, camera);
        collector.order(1).submitNameTag(poseStack, new Vec3(0, 0.68, 0), 0, Component.literal("JWN__").withColor(DragonEggRecordScreen.COLOR_4), false, FULL_BRIGHT, distanceToCameraSq, camera);
        poseStack.popPose();
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
