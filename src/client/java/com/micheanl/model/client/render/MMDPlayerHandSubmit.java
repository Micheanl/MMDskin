package com.micheanl.model.client.render;

import com.micheanl.model.client.mmd.MMDAnimationRuntime;
import com.micheanl.model.client.nativebridge.MMDModelMesh;
import com.micheanl.model.client.nativebridge.MMDModelSkeleton;
import com.micheanl.model.client.nativebridge.MMDSampledPose;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;

public record MMDPlayerHandSubmit(
        Matrix4f pose,
        MMDModelMesh mesh,
        MMDModelSkeleton skeleton,
        MMDSampledPose sampledPose,
        MMDMeshEmitter.Transform transform,
        HumanoidArm arm,
        int lightCoords,
        MMDAnimationRuntime.AnimationEntry animation
) implements SubmitNode {
    public static final FeatureRendererType<MMDPlayerHandSubmit> TYPE = FeatureRendererType.create("MMD Player Hand");

    @Override
    public FeatureRendererType<MMDPlayerHandSubmit> featureType() {
        return TYPE;
    }
}
