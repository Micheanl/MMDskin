package com.micheanl.model.client.nativebridge;

import java.util.Arrays;

public final class MMDSampledPose {
    private static final int MATRIX_VALUES = 16;

    private final float[] skinningMatrices;

    public MMDSampledPose(float[] skinningMatrices) {
        this.skinningMatrices = skinningMatrices.clone();
        if (this.skinningMatrices.length % MATRIX_VALUES != 0) {
            throw new IllegalArgumentException("Invalid sampled pose matrix array");
        }
    }

    public float[] skinningMatrices() {
        return this.skinningMatrices.clone();
    }

    public int boneCount() {
        return this.skinningMatrices.length / MATRIX_VALUES;
    }

    public float matrix(int bone, int offset) {
        return this.skinningMatrices[bone * MATRIX_VALUES + offset];
    }

    public float[] rawSkinningMatrices() {
        return this.skinningMatrices;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof MMDSampledPose pose && Arrays.equals(this.skinningMatrices, pose.skinningMatrices);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.skinningMatrices);
    }
}
