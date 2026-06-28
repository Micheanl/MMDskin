package com.micheanl.model.client.nativebridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class MMDModelMeshTest {
    @Test
    void copiesInputAndOutputArrays() {
        float[] positions = {0.0F, 1.0F, 2.0F};
        MMDModelMesh mesh = new MMDModelMesh(
                positions,
                new float[] {0.0F, 1.0F, 0.0F},
                new float[] {0.25F, 0.75F},
                new int[] {0, 0, 0},
                new int[] {0},
                new int[] {3},
                new float[] {1.0F}
        );

        positions[0] = 9.0F;
        float[] copied = mesh.positions();
        copied[1] = 8.0F;

        assertArrayEquals(new float[] {0.0F, 1.0F, 2.0F}, mesh.positions());
    }

    @Test
    void rejectsMismatchedArrays() {
        assertThrows(IllegalArgumentException.class, () -> new MMDModelMesh(
                new float[] {0.0F, 1.0F, 2.0F},
                new float[] {0.0F, 1.0F},
                new float[] {0.25F, 0.75F},
                new int[] {0, 0, 0},
                new int[] {0},
                new int[] {3},
                new float[] {1.0F}
        ));
    }

    @Test
    void comparesArrayContent() {
        MMDModelMesh left = new MMDModelMesh(
                new float[] {0.0F, 1.0F, 2.0F},
                new float[] {0.0F, 1.0F, 0.0F},
                new float[] {0.25F, 0.75F},
                new int[] {0, 0, 0},
                new int[] {0},
                new int[] {3},
                new float[] {1.0F}
        );
        MMDModelMesh right = new MMDModelMesh(
                new float[] {0.0F, 1.0F, 2.0F},
                new float[] {0.0F, 1.0F, 0.0F},
                new float[] {0.25F, 0.75F},
                new int[] {0, 0, 0},
                new int[] {0},
                new int[] {3},
                new float[] {0.5F}
        );

        assertNotEquals(left, right);
    }
}
