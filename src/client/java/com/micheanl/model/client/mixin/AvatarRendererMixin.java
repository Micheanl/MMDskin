package com.micheanl.model.client.mixin;

import com.micheanl.model.client.mmd.MMDModelRuntime;
import com.micheanl.model.client.mmd.MMDPlayerAnimationSelector;
import com.micheanl.model.client.render.MMDMeshEmitter;
import com.micheanl.model.client.render.MMDPlayerRenderState;
import com.micheanl.model.client.render.MMDPlayerSubmit;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.FabricOrderedSubmitNodeCollector;
import net.fabricmc.fabric.api.client.rendering.v1.SubmitRenderPhases;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(LivingEntityRenderer.class)
abstract class AvatarRendererMixin {
    @Redirect(
            method = "submit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"
            )
    )
    private void submitMMDPlayerBody(
            SubmitNodeCollector collector,
            Model<?> model,
            Object modelState,
            PoseStack poseStack,
            RenderType renderType,
            int lightCoords,
            int overlayCoords,
            int tintedColor,
            TextureAtlasSprite sprite,
            int outlineColor,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        if (!MMDPlayerRenderState.isEnabled() || !(modelState instanceof AvatarRenderState)) {
            submitVanilla(collector, model, modelState, poseStack, renderType, lightCoords, overlayCoords, tintedColor, sprite, outlineColor, crumblingOverlay);
            return;
        }
        Optional<MMDModelRuntime.ModelRenderData> data = MMDModelRuntime.instance().renderData();
        if (data.isEmpty()) {
            submitVanilla(collector, model, modelState, poseStack, renderType, lightCoords, overlayCoords, tintedColor, sprite, outlineColor, crumblingOverlay);
            return;
        }
        AvatarRenderState avatarState = (AvatarRenderState) modelState;
        ((FabricOrderedSubmitNodeCollector) collector).submitCustom(
                SubmitRenderPhases.SOLID,
                new MMDPlayerSubmit(
                        poseStack.last(),
                        data.get().mesh(),
                        data.get().transform(),
                        alpha(tintedColor),
                        MMDPlayerAnimationSelector.select(avatarState, MMDModelRuntime.instance().animations()).orElse(null)
                )
        );
        if (outlineColor != 0) {
            submitVanilla(collector, model, modelState, poseStack, renderType, lightCoords, overlayCoords, tintedColor, sprite, outlineColor, crumblingOverlay);
        }
    }

    private static void submitVanilla(
            SubmitNodeCollector collector,
            Model<?> model,
            Object modelState,
            PoseStack poseStack,
            RenderType renderType,
            int lightCoords,
            int overlayCoords,
            int tintedColor,
            TextureAtlasSprite sprite,
            int outlineColor,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        submitVanillaTyped(collector, model, modelState, poseStack, renderType, lightCoords, overlayCoords, tintedColor, sprite, outlineColor, crumblingOverlay);
    }

    @SuppressWarnings("unchecked")
    private static <S> void submitVanillaTyped(
            SubmitNodeCollector collector,
            Model<?> model,
            Object modelState,
            PoseStack poseStack,
            RenderType renderType,
            int lightCoords,
            int overlayCoords,
            int tintedColor,
            TextureAtlasSprite sprite,
            int outlineColor,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        collector.submitModel((Model<? super S>) model, (S) modelState, poseStack, renderType, lightCoords, overlayCoords, tintedColor, sprite, outlineColor, crumblingOverlay);
    }

    private static float alpha(int color) {
        return (color >>> 24 & 255) / 255.0F;
    }
}
