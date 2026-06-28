package com.micheanl.model.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;

import java.util.List;

public final class MMDPlayerHandFeatureRenderer extends RenderTypeFeatureRenderer<MMDPlayerHandSubmit> {
    @Override
    protected void buildGroup(FeatureFrameContext context, List<MMDPlayerHandSubmit> submits) {
        VertexConsumer consumer = getVertexBuilder(MMDRenderTypes.model());
        for (MMDPlayerHandSubmit submit : submits) {
            MMDMeshEmitter.emit(submit.mesh(), submit.pose(), consumer, submit.transform(), 1.0F);
        }
    }
}
