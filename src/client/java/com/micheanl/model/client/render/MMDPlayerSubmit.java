package com.micheanl.model.client.render;

import com.micheanl.model.client.mmd.MMDAnimationRuntime;
import com.micheanl.model.client.nativebridge.MMDModelMesh;
import com.micheanl.model.client.nativebridge.MMDModelSkeleton;
import com.micheanl.model.client.nativebridge.MMDSampledPose;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import org.joml.Matrix4f;

public record MMDPlayerSubmit(
        Matrix4f pose,
        MMDModelMesh mesh,
        MMDModelSkeleton skeleton,
        MMDSampledPose sampledPose,
        MMDMeshEmitter.Transform transform,
        float alpha,
        MMDAnimationRuntime.AnimationEntry animation
) implements SubmitNode {
    public static final FeatureRendererType<MMDPlayerSubmit> TYPE = FeatureRendererType.create("MMD Player");

    public MMDPlayerSubmit(PoseStack.Pose pose, MMDModelMesh mesh, MMDModelSkeleton skeleton, MMDSampledPose sampledPose, MMDMeshEmitter.Transform transform, float alpha, MMDAnimationRuntime.AnimationEntry animation) {
        this(new Matrix4f(pose.pose()), mesh, skeleton, sampledPose, transform, alpha, animation);
    }

    @Override
    public FeatureRendererType<MMDPlayerSubmit> featureType() {
        return TYPE;
    }
}
