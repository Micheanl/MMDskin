package com.micheanl.model.client.render;

import com.micheanl.model.client.nativebridge.MMDModelMesh;
import com.micheanl.model.client.nativebridge.MMDModelSkeleton;
import com.micheanl.model.client.nativebridge.MMDSampledPose;
import org.joml.Matrix4f;

public final class MMDPlayerHandFeatureRenderer extends MMDFeatureRendererBase<MMDPlayerHandSubmit> {
    @Override
    protected MMDModelMesh mesh(MMDPlayerHandSubmit submit) {
        return submit.mesh();
    }

    @Override
    protected MMDModelSkeleton skeleton(MMDPlayerHandSubmit submit) {
        return submit.skeleton();
    }

    @Override
    protected MMDSampledPose sampledPose(MMDPlayerHandSubmit submit) {
        return submit.sampledPose();
    }

    @Override
    protected Matrix4f pose(MMDPlayerHandSubmit submit) {
        return submit.pose();
    }

    @Override
    protected MMDMeshEmitter.Transform transform(MMDPlayerHandSubmit submit) {
        return submit.transform();
    }

    @Override
    protected float alpha(MMDPlayerHandSubmit submit) {
        return 1.0F;
    }
}
