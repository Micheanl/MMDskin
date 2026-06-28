package com.micheanl.model.client.nativebridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class MMDModelSkeletonTest {
    @Test
    void copiesInputAndOutputArrays() {
        int[] parents = {-1, 0};
        MMDModelSkeleton skeleton = new MMDModelSkeleton(
                parents,
                new float[] {0.0F, 1.0F, 2.0F, 3.0F, 4.0F, 5.0F},
                new int[] {0, 1, 0, 0},
                new float[] {0.75F, 0.25F, 0.0F, 0.0F}
        );

        parents[1] = 9;
        int[] copied = skeleton.parentIndices();
        copied[0] = 8;

        assertArrayEquals(new int[] {-1, 0}, skeleton.parentIndices());
    }

    @Test
    void exposesBoneAndSkinningScalars() {
        MMDModelSkeleton skeleton = new MMDModelSkeleton(
                new int[] {-1, 0},
                new float[] {0.0F, 1.0F, 2.0F, 3.0F, 4.0F, 5.0F},
                new int[] {0, 1, 0, 0, 1, 0, 0, 0},
                new float[] {0.75F, 0.25F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F}
        );

        assertEquals(2, skeleton.boneCount());
        assertEquals(-1, skeleton.parentIndex(0));
        assertEquals(0, skeleton.parentIndex(1));
        assertEquals(3.0F, skeleton.positionX(1));
        assertEquals(4.0F, skeleton.positionY(1));
        assertEquals(5.0F, skeleton.positionZ(1));
        assertEquals(2, skeleton.skinningVertexCount());
        assertEquals(1, skeleton.skinIndex(0, 1));
        assertEquals(0.25F, skeleton.skinWeight(0, 1));
    }

    @Test
    void rejectsMismatchedArrays() {
        assertThrows(IllegalArgumentException.class, () -> new MMDModelSkeleton(
                new int[] {-1, 0},
                new float[] {0.0F, 1.0F, 2.0F},
                new int[] {0, 1, 0, 0},
                new float[] {1.0F, 0.0F, 0.0F, 0.0F}
        ));
        assertThrows(IllegalArgumentException.class, () -> new MMDModelSkeleton(
                new int[] {-1},
                new float[] {0.0F, 1.0F, 2.0F},
                new int[] {0, 1, 0},
                new float[] {1.0F, 0.0F, 0.0F}
        ));
    }

    @Test
    void comparesArrayContent() {
        MMDModelSkeleton left = new MMDModelSkeleton(
                new int[] {-1},
                new float[] {0.0F, 1.0F, 2.0F},
                new int[] {0, 0, 0, 0},
                new float[] {1.0F, 0.0F, 0.0F, 0.0F}
        );
        MMDModelSkeleton right = new MMDModelSkeleton(
                new int[] {-1},
                new float[] {0.0F, 1.0F, 2.0F},
                new int[] {0, 0, 0, 0},
                new float[] {0.5F, 0.5F, 0.0F, 0.0F}
        );

        assertNotEquals(left, right);
    }
}
