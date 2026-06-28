package com.micheanl.model.client.nativebridge;

import java.util.Arrays;

public final class MMDModelMesh {
    private final float[] positions;
    private final float[] normals;
    private final float[] uvs;
    private final int[] indices;
    private final int[] materialStarts;
    private final int[] materialCounts;
    private final float[] materialAlphas;
    private final float minX;
    private final float maxX;
    private final float minY;
    private final float maxY;
    private final float minZ;
    private final float maxZ;

    public MMDModelMesh(
            float[] positions,
            float[] normals,
            float[] uvs,
            int[] indices,
            int[] materialStarts,
            int[] materialCounts,
            float[] materialAlphas
    ) {
        this.positions = positions.clone();
        this.normals = normals.clone();
        this.uvs = uvs.clone();
        this.indices = indices.clone();
        this.materialStarts = materialStarts.clone();
        this.materialCounts = materialCounts.clone();
        this.materialAlphas = materialAlphas.clone();
        if (this.positions.length % 3 != 0 || this.normals.length != this.positions.length) {
            throw new IllegalArgumentException("Invalid model mesh vertex arrays");
        }
        if (this.uvs.length / 2 != this.positions.length / 3 || this.indices.length % 3 != 0) {
            throw new IllegalArgumentException("Invalid model mesh index arrays");
        }
        if (this.materialStarts.length != this.materialCounts.length || this.materialStarts.length != this.materialAlphas.length) {
            throw new IllegalArgumentException("Invalid model mesh material arrays");
        }
        float[] bounds = bounds(this.positions);
        this.minX = bounds[0];
        this.maxX = bounds[1];
        this.minY = bounds[2];
        this.maxY = bounds[3];
        this.minZ = bounds[4];
        this.maxZ = bounds[5];
    }

    public float[] positions() {
        return this.positions.clone();
    }

    public int vertexCount() {
        return this.positions.length / 3;
    }

    public int indexCount() {
        return this.indices.length;
    }

    public int materialCount() {
        return this.materialStarts.length;
    }

    public float positionX(int vertex) {
        return this.positions[vertex * 3];
    }

    public float positionY(int vertex) {
        return this.positions[vertex * 3 + 1];
    }

    public float positionZ(int vertex) {
        return this.positions[vertex * 3 + 2];
    }

    public float minX() {
        return this.minX;
    }

    public float maxX() {
        return this.maxX;
    }

    public float minY() {
        return this.minY;
    }

    public float maxY() {
        return this.maxY;
    }

    public float minZ() {
        return this.minZ;
    }

    public float maxZ() {
        return this.maxZ;
    }

    public float normalX(int vertex) {
        return this.normals[vertex * 3];
    }

    public float normalY(int vertex) {
        return this.normals[vertex * 3 + 1];
    }

    public float normalZ(int vertex) {
        return this.normals[vertex * 3 + 2];
    }

    public float u(int vertex) {
        return this.uvs[vertex * 2];
    }

    public float v(int vertex) {
        return this.uvs[vertex * 2 + 1];
    }

    public int index(int offset) {
        return this.indices[offset];
    }

    public int materialStart(int material) {
        return this.materialStarts[material];
    }

    public int materialCount(int material) {
        return this.materialCounts[material];
    }

    public float materialAlpha(int material) {
        return this.materialAlphas[material];
    }

    public float[] normals() {
        return this.normals.clone();
    }

    public float[] uvs() {
        return this.uvs.clone();
    }

    public int[] indices() {
        return this.indices.clone();
    }

    public int[] materialStarts() {
        return this.materialStarts.clone();
    }

    public int[] materialCounts() {
        return this.materialCounts.clone();
    }

    public float[] materialAlphas() {
        return this.materialAlphas.clone();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MMDModelMesh mesh)) {
            return false;
        }
        return Arrays.equals(this.positions, mesh.positions)
                && Arrays.equals(this.normals, mesh.normals)
                && Arrays.equals(this.uvs, mesh.uvs)
                && Arrays.equals(this.indices, mesh.indices)
                && Arrays.equals(this.materialStarts, mesh.materialStarts)
                && Arrays.equals(this.materialCounts, mesh.materialCounts)
                && Arrays.equals(this.materialAlphas, mesh.materialAlphas);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(this.positions);
        result = 31 * result + Arrays.hashCode(this.normals);
        result = 31 * result + Arrays.hashCode(this.uvs);
        result = 31 * result + Arrays.hashCode(this.indices);
        result = 31 * result + Arrays.hashCode(this.materialStarts);
        result = 31 * result + Arrays.hashCode(this.materialCounts);
        result = 31 * result + Arrays.hashCode(this.materialAlphas);
        return result;
    }

    private static float[] bounds(float[] positions) {
        if (positions.length == 0) {
            return new float[] {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F};
        }
        float minX = positions[0];
        float maxX = minX;
        float minY = positions[1];
        float maxY = minY;
        float minZ = positions[2];
        float maxZ = minZ;
        for (int offset = 3; offset < positions.length; offset += 3) {
            float x = positions[offset];
            float y = positions[offset + 1];
            float z = positions[offset + 2];
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
        }
        return new float[] {minX, maxX, minY, maxY, minZ, maxZ};
    }
}
