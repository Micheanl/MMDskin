package com.micheanl.model.client.mixin;

import com.micheanl.model.client.render.MMDPlayerRenderState;
import com.micheanl.model.client.render.MMDPlayerSubmit;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.FabricOrderedSubmitNodeCollector;
import net.fabricmc.fabric.api.client.rendering.v1.SubmitRenderPhases;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
abstract class AvatarRendererMixin extends LivingEntityRenderer<Avatar, AvatarRenderState, PlayerModel> {
    private AvatarRendererMixin() {
        super(null, null, 0.0F);
    }

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void submitMMDPlayer(
            AvatarRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera,
            CallbackInfo callbackInfo
    ) {
        if (!MMDPlayerRenderState.isEnabled()) {
            return;
        }
        ((FabricOrderedSubmitNodeCollector) submitNodeCollector).submitCustom(
                SubmitRenderPhases.SOLID,
                new MMDPlayerSubmit(poseStack.last(), state)
        );
        callbackInfo.cancel();
    }
}
