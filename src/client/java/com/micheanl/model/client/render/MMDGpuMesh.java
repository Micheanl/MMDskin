package com.micheanl.model.client.render;

import com.micheanl.model.client.nativebridge.MMDModelMesh;
import com.micheanl.model.client.nativebridge.MMDModelSkeleton;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class MMDGpuMesh implements AutoCloseable {
    private static final int RGB = 230;
    private static final int VERTEX_SIZE = DefaultVertexFormat.ENTITY.getVertexSize();

    private final GpuBuffer vertexBuffer;
    private final GpuBuffer indexBuffer;
    private final GpuBuffer indirectBuffer;
    private final IndexType indexType;
    private final MaterialRange[] materialRanges;
    private final int indexCount;

    private MMDGpuMesh(GpuBuffer vertexBuffer, GpuBuffer indexBuffer, GpuBuffer indirectBuffer, IndexType indexType, MaterialRange[] materialRanges, int indexCount) {
        this.vertexBuffer = vertexBuffer;
        this.indexBuffer = indexBuffer;
        this.indirectBuffer = indirectBuffer;
        this.indexType = indexType;
        this.materialRanges = materialRanges;
        this.indexCount = indexCount;
    }

    static MMDGpuMesh upload(MMDModelMesh mesh, MMDModelSkeleton skeleton) {
        StaticData data = buildStaticData(mesh, skeleton);
        GpuBuffer vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "MMD Vertex", GpuBuffer.USAGE_VERTEX, data.vertices());
        GpuBuffer indexBuffer = RenderSystem.getDevice().createBuffer(() -> "MMD Index", GpuBuffer.USAGE_INDEX, data.indices());
        GpuBuffer indirectBuffer = RenderSystem.getDevice().createBuffer(() -> "MMD Indirect", GpuBuffer.USAGE_INDIRECT_PARAMETERS, data.indirectCommands());
        return new MMDGpuMesh(vertexBuffer, indexBuffer, indirectBuffer, data.indexType(), data.materialRanges(), data.indexCount());
    }

    static StaticData buildStaticData(MMDModelMesh mesh, MMDModelSkeleton skeleton) {
        int vertexCount = mesh.vertexCount();
        IndexType indexType = IndexType.least(vertexCount);
        ByteBuffer vertices = ByteBuffer.allocateDirect(vertexCount * VERTEX_SIZE).order(ByteOrder.nativeOrder());
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            putVertex(vertices, mesh, skeleton, vertex);
        }
        vertices.flip();
        ByteBuffer indices = ByteBuffer.allocateDirect(mesh.indexCount() * indexType.bytes).order(ByteOrder.nativeOrder());
        for (int offset = 0; offset < mesh.indexCount(); offset++) {
            int index = mesh.index(offset);
            if (indexType == IndexType.SHORT) {
                indices.putShort((short) index);
            } else {
                indices.putInt(index);
            }
        }
        indices.flip();
        MaterialRange[] ranges = materialRanges(mesh);
        ByteBuffer indirect = ByteBuffer.allocateDirect(Math.max(1, ranges.length) * 20).order(ByteOrder.nativeOrder());
        for (MaterialRange range : ranges) {
            indirect.putInt(range.indexCount());
            indirect.putInt(1);
            indirect.putInt(range.firstIndex());
            indirect.putInt(0);
            indirect.putInt(0);
        }
        indirect.flip();
        return new StaticData(vertices, indices, indirect, indexType, ranges, mesh.indexCount());
    }

    GpuBuffer vertexBuffer() {
        return this.vertexBuffer;
    }

    GpuBuffer indexBuffer() {
        return this.indexBuffer;
    }

    GpuBuffer indirectBuffer() {
        return this.indirectBuffer;
    }

    IndexType indexType() {
        return this.indexType;
    }

    MaterialRange[] materialRanges() {
        return this.materialRanges;
    }

    int indexCount() {
        return this.indexCount;
    }

    @Override
    public void close() {
        this.vertexBuffer.close();
        this.indexBuffer.close();
        this.indirectBuffer.close();
    }

    private static void putVertex(ByteBuffer buffer, MMDModelMesh mesh, MMDModelSkeleton skeleton, int vertex) {
        buffer.putFloat(mesh.positionX(vertex));
        buffer.putFloat(mesh.positionY(vertex));
        buffer.putFloat(mesh.positionZ(vertex));
        buffer.put((byte) RGB);
        buffer.put((byte) RGB);
        buffer.put((byte) RGB);
        buffer.put((byte) 255);
        buffer.putFloat(mesh.u(vertex));
        buffer.putFloat(mesh.v(vertex));
        buffer.putShort((short) bone(skeleton, vertex, 0));
        buffer.putShort((short) bone(skeleton, vertex, 1));
        buffer.putShort((short) bone(skeleton, vertex, 2));
        buffer.putShort((short) bone(skeleton, vertex, 3));
        buffer.put(packSnorm(skeleton.skinWeight(vertex, 0) * 2.0F - 1.0F));
        buffer.put(packSnorm(skeleton.skinWeight(vertex, 1) * 2.0F - 1.0F));
        buffer.put(packSnorm(skeleton.skinWeight(vertex, 2) * 2.0F - 1.0F));
        buffer.put((byte) 0);
    }

    private static byte packSnorm(float value) {
        float clamped = Math.max(-1.0F, Math.min(1.0F, value));
        return (byte) ((int) (clamped * 127.0F) & 255);
    }

    private static int bone(MMDModelSkeleton skeleton, int vertex, int influence) {
        int bone = skeleton.skinIndex(vertex, influence);
        if (bone < 0) {
            return 0;
        }
        return Math.min(bone, MMDGpuSkinning.MAX_BONES - 1);
    }

    private static MaterialRange[] materialRanges(MMDModelMesh mesh) {
        MaterialRange[] ranges = new MaterialRange[mesh.materialCount()];
        for (int material = 0; material < ranges.length; material++) {
            int start = Math.min(mesh.materialStart(material), mesh.indexCount());
            int end = Math.min(start + mesh.materialCount(material), mesh.indexCount());
            ranges[material] = new MaterialRange(start, Math.max(0, end - start), mesh.materialAlpha(material));
        }
        return ranges;
    }

    record MaterialRange(int firstIndex, int indexCount, float alpha) {
    }

    record StaticData(ByteBuffer vertices, ByteBuffer indices, ByteBuffer indirectCommands, IndexType indexType, MaterialRange[] materialRanges, int indexCount) {
    }
}
