package com.micheanl.model.client.nativebridge;

import java.util.Arrays;

public final class MMDModelSkeleton {
    private static final int INFLUENCES = 4;

    private final int[] parentIndices;
    private final float[] positions;
    private final int[] skinIndices;
    private final float[] skinWeights;

    public MMDModelSkeleton(int[] parentIndices, float[] positions, int[] skinIndices, float[] skinWeights) {
        this.parentIndices = parentIndices.clone();
        this.positions = positions.clone();
        this.skinIndices = skinIndices.clone();
        this.skinWeights = skinWeights.clone();
        if (this.positions.length != this.parentIndices.length * 3) {
            throw new IllegalArgumentException("Invalid model skeleton bone arrays");
        }
        if (this.skinIndices.length != this.skinWeights.length || this.skinIndices.length % INFLUENCES != 0) {
            throw new IllegalArgumentException("Invalid model skeleton skinning arrays");
        }
    }

    public int[] parentIndices() {
        return this.parentIndices.clone();
    }

    public float[] positions() {
        return this.positions.clone();
    }

    public int[] skinIndices() {
        return this.skinIndices.clone();
    }

    public float[] skinWeights() {
        return this.skinWeights.clone();
    }

    public int boneCount() {
        return this.parentIndices.length;
    }

    public int skinningVertexCount() {
        return this.skinIndices.length / INFLUENCES;
    }

    public int parentIndex(int bone) {
        return this.parentIndices[bone];
    }

    public float positionX(int bone) {
        return this.positions[bone * 3];
    }

    public float positionY(int bone) {
        return this.positions[bone * 3 + 1];
    }

    public float positionZ(int bone) {
        return this.positions[bone * 3 + 2];
    }

    public int skinIndex(int vertex, int influence) {
        return this.skinIndices[vertex * INFLUENCES + influence];
    }

    public float skinWeight(int vertex, int influence) {
        return this.skinWeights[vertex * INFLUENCES + influence];
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MMDModelSkeleton skeleton)) {
            return false;
        }
        return Arrays.equals(this.parentIndices, skeleton.parentIndices)
                && Arrays.equals(this.positions, skeleton.positions)
                && Arrays.equals(this.skinIndices, skeleton.skinIndices)
                && Arrays.equals(this.skinWeights, skeleton.skinWeights);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(this.parentIndices);
        result = 31 * result + Arrays.hashCode(this.positions);
        result = 31 * result + Arrays.hashCode(this.skinIndices);
        result = 31 * result + Arrays.hashCode(this.skinWeights);
        return result;
    }
}
