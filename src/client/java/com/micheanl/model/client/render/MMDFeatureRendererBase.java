package com.micheanl.model.client.render;

import com.micheanl.model.client.nativebridge.MMDModelMesh;
import com.micheanl.model.client.nativebridge.MMDModelSkeleton;
import com.micheanl.model.client.nativebridge.MMDSampledPose;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRenderer;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

abstract class MMDFeatureRendererBase<Submit extends SubmitNode> implements FeatureRenderer<Submit> {
    private final List<Group> groups = new ArrayList<>();
    private final MMDGpuMeshCache gpuMeshes = new MMDGpuMeshCache();
    private MMDGpuSkinning gpuSkinning;
    private MMDGpuDrawUniforms drawUniforms;

    protected abstract MMDModelMesh mesh(Submit submit);

    protected abstract MMDModelSkeleton skeleton(Submit submit);

    protected abstract MMDSampledPose sampledPose(Submit submit);

    protected abstract Matrix4f pose(Submit submit);

    protected abstract MMDMeshEmitter.Transform transform(Submit submit);

    protected abstract float alpha(Submit submit);

    @Override
    public final void beginPrepare(FeatureFrameContext context) {
        this.gpuMeshes.beginFrame();
    }

    @Override
    public final void prepareGroup(FeatureFrameContext context, List<Submit> submits, boolean strictlyOrdered) {
        Group group = new Group();
        for (Submit submit : submits) {
            if (canUseGpu(submit)) {
                group.addGpu(context, submit);
            } else {
                group.addCpu(context, submit);
            }
        }
        this.groups.add(group);
    }

    @Override
    public final void executeGroup(FeatureFrameContext context, int groupIndex, List<Submit> submits, boolean strictlyOrdered) {
        Group group = this.groups.get(groupIndex);
        for (CpuDraw draw : group.cpuDraws) {
            StagedVertexBuffer.ExecuteInfo info = context.stagedVertexBuffer().getExecuteInfo(draw.draw);
            if (info != null) {
                draw.renderType.drawFromBuffer(info);
            }
        }
        for (GpuDraw draw : group.gpuDraws) {
            drawGpu(draw.renderType, draw.mesh, draw.bones, draw.materialDraws);
        }
    }

    @Override
    public final void finishExecute(FeatureFrameContext context) {
        this.groups.clear();
        this.gpuMeshes.endFrame();
        if (this.gpuSkinning != null) {
            this.gpuSkinning.endFrame();
        }
        if (this.drawUniforms != null) {
            this.drawUniforms.endFrame();
        }
    }

    @Override
    public final void close() {
        this.gpuMeshes.close();
        if (this.gpuSkinning != null) {
            this.gpuSkinning.close();
            this.gpuSkinning = null;
        }
        if (this.drawUniforms != null) {
            this.drawUniforms.close();
            this.drawUniforms = null;
        }
    }

    private boolean canUseGpu(Submit submit) {
        MMDModelSkeleton skeleton = skeleton(submit);
        MMDSampledPose sampledPose = sampledPose(submit);
        return skeleton != null
                && sampledPose != null
                && sampledPose.boneCount() <= MMDGpuSkinning.MAX_BONES
                && skeleton.skinningVertexCount() >= mesh(submit).vertexCount();
    }

    private static void drawGpu(PreparedRenderType renderType, MMDGpuMesh mesh, GpuBufferSlice bones, List<MaterialDraw> materialDraws) {
        RenderTarget renderTarget = renderType.outputTarget().getRenderTarget();
        var colorTexture = RenderSystem.outputColorTextureOverride != null ? RenderSystem.outputColorTextureOverride : renderTarget.getColorTextureView();
        var depthTexture = renderTarget.useDepth
                ? (RenderSystem.outputDepthTextureOverride != null ? RenderSystem.outputDepthTextureOverride : renderTarget.getDepthTextureView())
                : null;

        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> "MMD skinning draw with " + renderType.pipeline(), colorTexture, Optional.empty(), depthTexture, OptionalDouble.empty())) {
            renderPass.setPipeline(renderType.pipeline());
            ScissorState scissorState = renderType.scissorState();
            if (scissorState.enabled()) {
                renderPass.enableScissor(scissorState.x(), scissorState.y(), scissorState.width(), scissorState.height());
            }
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", renderType.dynamicTransforms());
            renderPass.setUniform("MmdBones", bones);
            renderPass.setVertexBuffer(0, mesh.vertexBuffer().slice());
            for (PreparedRenderType.Texture texture : renderType.textures()) {
                renderPass.bindTexture(texture.name(), texture.textureView(), texture.sampler());
            }
            renderPass.setIndexBuffer(mesh.indexBuffer(), mesh.indexType());
            MMDGpuBackend backend = MMDGpuBackend.current();
            if (backend.drawMode() == MMDGpuBackend.DrawMode.INDIRECT) {
                for (MaterialDraw draw : materialDraws) {
                    renderPass.setUniform("MmdDraw", draw.uniform());
                    renderPass.drawIndexedIndirect(mesh.indirectBuffer().slice((long) draw.material() * 20L, 20L), 1);
                }
            } else {
                for (MaterialDraw draw : materialDraws) {
                    renderPass.setUniform("MmdDraw", draw.uniform());
                    renderPass.drawIndexed(draw.range().indexCount(), 1, draw.range().firstIndex(), 0, 0);
                }
            }
        }
    }

    private final class Group {
        private final List<CpuDraw> cpuDraws = new ArrayList<>();
        private final List<GpuDraw> gpuDraws = new ArrayList<>();

        void addCpu(FeatureFrameContext context, Submit submit) {
            RenderType renderType = MMDRenderTypes.model();
            PreparedRenderType prepared = renderType.prepare();
            StagedVertexBuffer.Draw draw = context.stagedVertexBuffer().appendDraw(renderType.format(), renderType.primitiveTopology());
            VertexConsumer consumer = context.stagedVertexBuffer().getVertexBuilder(draw);
            MMDMeshEmitter.emit(mesh(submit), skeleton(submit), sampledPose(submit), pose(submit), consumer, transform(submit), alpha(submit));
            this.cpuDraws.add(new CpuDraw(draw, prepared));
        }

        void addGpu(FeatureFrameContext context, Submit submit) {
            RenderType renderType = MMDRenderTypes.gpuModel();
            PreparedRenderType prepared = renderType.prepare();
            MMDModelMesh mesh = mesh(submit);
            MMDGpuMesh gpuMesh = gpuMeshes.get(mesh, skeleton(submit));
            gpuMeshes.keep(mesh);
            if (gpuSkinning == null) {
                gpuSkinning = new MMDGpuSkinning();
            }
            if (drawUniforms == null) {
                drawUniforms = new MMDGpuDrawUniforms();
            }
            GpuBufferSlice bones = gpuSkinning.write(pose(submit), sampledPose(submit), transform(submit));
            List<MaterialDraw> materialDraws = new ArrayList<>(gpuMesh.materialRanges().length);
            MMDGpuMesh.MaterialRange[] ranges = gpuMesh.materialRanges();
            for (int material = 0; material < ranges.length; material++) {
                MMDGpuMesh.MaterialRange range = ranges[material];
                if (range.indexCount() > 0) {
                    materialDraws.add(new MaterialDraw(material, range, drawUniforms.write(range.alpha() * alpha(submit))));
                }
            }
            this.gpuDraws.add(new GpuDraw(gpuMesh, prepared, bones, materialDraws));
        }
    }

    private record CpuDraw(StagedVertexBuffer.Draw draw, PreparedRenderType renderType) {
    }

    private record GpuDraw(MMDGpuMesh mesh, PreparedRenderType renderType, GpuBufferSlice bones, List<MaterialDraw> materialDraws) {
    }

    private record MaterialDraw(int material, MMDGpuMesh.MaterialRange range, GpuBufferSlice uniform) {
    }
}
