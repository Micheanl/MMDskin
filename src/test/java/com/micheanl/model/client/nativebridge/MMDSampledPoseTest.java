package com.micheanl.model.client.nativebridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class MMDSampledPoseTest {
    @Test
    void exposesMatrixScalars() {
        MMDSampledPose pose = new MMDSampledPose(new float[] {
                1.0F, 0.0F, 0.0F, 0.0F,
                0.0F, 1.0F, 0.0F, 0.0F,
                0.0F, 0.0F, 1.0F, 0.0F,
                2.0F, 3.0F, 4.0F, 1.0F
        });

        assertEquals(1, pose.boneCount());
        assertEquals(2.0F, pose.matrix(0, 12));
        assertEquals(3.0F, pose.matrix(0, 13));
        assertEquals(4.0F, pose.matrix(0, 14));
    }

    @Test
    void copiesArrays() {
        float[] matrices = new float[16];
        matrices[0] = 1.0F;

        MMDSampledPose pose = new MMDSampledPose(matrices);
        matrices[0] = 2.0F;
        float[] copy = pose.skinningMatrices();
        copy[0] = 3.0F;

        assertArrayEquals(new float[] {
                1.0F, 0.0F, 0.0F, 0.0F,
                0.0F, 0.0F, 0.0F, 0.0F,
                0.0F, 0.0F, 0.0F, 0.0F,
                0.0F, 0.0F, 0.0F, 0.0F
        }, pose.skinningMatrices());
    }

    @Test
    void rejectsInvalidMatrixArrays() {
        assertThrows(IllegalArgumentException.class, () -> new MMDSampledPose(new float[15]));
    }

    @Test
    void comparesArrayContent() {
        MMDSampledPose left = new MMDSampledPose(new float[16]);
        float[] matrices = new float[16];
        matrices[15] = 1.0F;
        MMDSampledPose right = new MMDSampledPose(matrices);

        assertNotEquals(left, right);
    }
}
