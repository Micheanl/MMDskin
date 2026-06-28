package com.micheanl.model.client.imgui;

import com.micheanl.model.MMDSkin;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.RenderPipelines;

public final class MMDImGuiRenderTypes {
    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation(MMDSkin.id("pipeline/imgui"))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
                    .build()
    );

    private MMDImGuiRenderTypes() {
    }

    public static RenderPipeline pipeline() {
        return PIPELINE;
    }
}
