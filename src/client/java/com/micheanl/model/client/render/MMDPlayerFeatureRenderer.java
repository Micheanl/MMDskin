package com.micheanl.model.client.render;

import com.micheanl.model.client.nativebridge.MMDModelMesh;
import com.micheanl.model.client.nativebridge.MMDModelSkeleton;
import com.micheanl.model.client.nativebridge.MMDSampledPose;
import org.joml.Matrix4f;

public final class MMDPlayerFeatureRenderer extends MMDFeatureRendererBase<MMDPlayerSubmit> {
    @Override
    protected MMDModelMesh mesh(MMDPlayerSubmit submit) {
        return submit.mesh();
    }

    @Override
    protected MMDModelSkeleton skeleton(MMDPlayerSubmit submit) {
        return submit.skeleton();
    }

    @Override
    protected MMDSampledPose sampledPose(MMDPlayerSubmit submit) {
        return submit.sampledPose();
    }

    @Override
    protected Matrix4f pose(MMDPlayerSubmit submit) {
        return submit.pose();
    }

    @Override
    protected MMDMeshEmitter.Transform transform(MMDPlayerSubmit submit) {
        return submit.transform();
    }

    @Override
    protected float alpha(MMDPlayerSubmit submit) {
        return submit.alpha();
    }
}
