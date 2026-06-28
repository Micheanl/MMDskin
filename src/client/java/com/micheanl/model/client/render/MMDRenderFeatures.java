package com.micheanl.model.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.FeatureRendererRegistry;

public final class MMDRenderFeatures {
    private MMDRenderFeatures() {
    }

    public static void register() {
        FeatureRendererRegistry.register(MMDPlayerSubmit.TYPE, MMDPlayerFeatureRenderer::new);
        FeatureRendererRegistry.register(MMDPlayerHandSubmit.TYPE, MMDPlayerHandFeatureRenderer::new);
    }
}
