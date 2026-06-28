package com.micheanl.model.client.render;

import com.micheanl.model.client.nativebridge.MMDModelMesh;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import org.joml.Matrix4f;

public record MMDPlayerSubmit(Matrix4f pose, MMDModelMesh mesh, MMDMeshEmitter.Transform transform, float alpha) implements SubmitNode {
    public static final FeatureRendererType<MMDPlayerSubmit> TYPE = FeatureRendererType.create("MMD Player");

    public MMDPlayerSubmit(PoseStack.Pose pose, MMDModelMesh mesh, MMDMeshEmitter.Transform transform, float alpha) {
        this(new Matrix4f(pose.pose()), mesh, transform, alpha);
    }

    @Override
    public FeatureRendererType<MMDPlayerSubmit> featureType() {
        return TYPE;
    }
}
