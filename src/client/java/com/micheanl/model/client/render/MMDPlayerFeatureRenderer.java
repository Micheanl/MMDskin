package com.micheanl.model.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;

import java.util.List;

public final class MMDPlayerFeatureRenderer extends RenderTypeFeatureRenderer<MMDPlayerSubmit> {
    @Override
    protected void buildGroup(FeatureFrameContext context, List<MMDPlayerSubmit> submits) {
        VertexConsumer consumer = getVertexBuilder(MMDRenderTypes.model());
        for (MMDPlayerSubmit submit : submits) {
            MMDMeshEmitter.emit(submit.mesh(), submit.pose(), consumer, submit.transform(), submit.alpha());
        }
    }
}
