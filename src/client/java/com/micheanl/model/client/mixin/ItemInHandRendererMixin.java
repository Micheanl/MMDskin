package com.micheanl.model.client.mixin;

import com.micheanl.model.client.render.MMDPlayerHandRenderer;
import com.micheanl.model.client.render.MMDPlayerRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
abstract class ItemInHandRendererMixin {
    @Inject(method = "renderPlayerArm", at = @At("HEAD"), cancellable = true)
    private void renderMMDPlayerArm(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            float inverseArmHeight,
            float attackValue,
            HumanoidArm arm,
            CallbackInfo callbackInfo
    ) {
        if (!MMDPlayerRenderState.isEnabled()) {
            return;
        }
        if (MMDPlayerHandRenderer.submit(poseStack, submitNodeCollector, lightCoords, arm)) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "renderMapHand", at = @At("HEAD"), cancellable = true)
    private void renderMMDMapHand(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            HumanoidArm arm,
            CallbackInfo callbackInfo
    ) {
        if (!MMDPlayerRenderState.isEnabled()) {
            return;
        }
        poseStack.pushPose();
        mmdskin$applyMapHandTransform(poseStack, arm);
        boolean submitted = MMDPlayerHandRenderer.submit(poseStack, submitNodeCollector, lightCoords, arm);
        poseStack.popPose();
        if (submitted) {
            callbackInfo.cancel();
        }
    }

    @Unique
    private static void mmdskin$applyMapHandTransform(PoseStack poseStack, HumanoidArm arm) {
        float invert = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(92.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(invert * -41.0F));
        poseStack.translate(invert * 0.3F, -1.1F, 0.45F);
    }
}
