package com.micheanl.model.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import org.joml.Matrix4f;

public record MMDPlayerSubmit(Matrix4f pose, AvatarRenderState state) implements SubmitNode {
    public static final FeatureRendererType<MMDPlayerSubmit> TYPE = FeatureRendererType.create("MMD Player");

    public MMDPlayerSubmit(PoseStack.Pose pose, AvatarRenderState state) {
        this(new Matrix4f(pose.pose()), state);
    }

    @Override
    public FeatureRendererType<MMDPlayerSubmit> featureType() {
        return TYPE;
    }
}
