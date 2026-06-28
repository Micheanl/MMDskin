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
    }

    public float[] positions() {
        return this.positions.clone();
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
}
