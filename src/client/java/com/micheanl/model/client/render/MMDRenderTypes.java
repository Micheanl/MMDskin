package com.micheanl.model.client.render;

import com.micheanl.model.MMDSkin;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

public final class MMDRenderTypes {
    private static final BindGroupLayout BONES_LAYOUT = BindGroupLayout.builder()
            .withUniform("MmdBones", UniformType.UNIFORM_BUFFER)
            .withUniform("MmdDraw", UniformType.UNIFORM_BUFFER)
            .build();
    private static final RenderPipeline MODEL_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                    .withLocation(MMDSkin.id("pipeline/model"))
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .withDepthStencilState(DepthStencilState.DEFAULT)
                    .withCull(false)
                    .build()
    );
    private static final RenderPipeline GPU_MODEL_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                    .withBindGroupLayout(BONES_LAYOUT)
                    .withLocation(MMDSkin.id("pipeline/model_gpu"))
                    .withVertexShader(MMDSkin.id("core/mmdskin_model"))
                    .withFragmentShader(MMDSkin.id("core/mmdskin_model"))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withVertexBinding(0, DefaultVertexFormat.ENTITY)
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .withDepthStencilState(DepthStencilState.DEFAULT)
                    .withCull(false)
                    .build()
    );
    private static final RenderType MODEL = RenderType.create(
            "mmdskin_model",
            RenderSetup.builder(MODEL_PIPELINE).createRenderSetup()
    );
    private static final RenderType GPU_MODEL = RenderType.create(
            "mmdskin_model_gpu",
            RenderSetup.builder(GPU_MODEL_PIPELINE).createRenderSetup()
    );

    private MMDRenderTypes() {
    }

    public static RenderType model() {
        return MODEL;
    }

    public static RenderType gpuModel() {
        return GPU_MODEL;
    }
}
