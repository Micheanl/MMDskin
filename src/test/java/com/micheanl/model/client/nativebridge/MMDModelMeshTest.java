package com.micheanl.model.client.nativebridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void exposesScalarMeshDataWithoutCopies() {
        MMDModelMesh mesh = new MMDModelMesh(
                new float[] {0.0F, 1.0F, 2.0F, 3.0F, 4.0F, 5.0F},
                new float[] {0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F},
                new float[] {0.25F, 0.75F, 0.5F, 1.0F},
                new int[] {1, 0, 1},
                new int[] {0},
                new int[] {3},
                new float[] {0.5F}
        );

        assertEquals(2, mesh.vertexCount());
        assertEquals(3, mesh.indexCount());
        assertEquals(1, mesh.materialCount());
        assertEquals(3.0F, mesh.positionX(1));
        assertEquals(4.0F, mesh.positionY(1));
        assertEquals(5.0F, mesh.positionZ(1));
        assertEquals(0.0F, mesh.minX());
        assertEquals(3.0F, mesh.maxX());
        assertEquals(1.0F, mesh.minY());
        assertEquals(4.0F, mesh.maxY());
        assertEquals(2.0F, mesh.minZ());
        assertEquals(5.0F, mesh.maxZ());
        assertEquals(0.0F, mesh.normalX(1));
        assertEquals(0.0F, mesh.normalY(1));
        assertEquals(1.0F, mesh.normalZ(1));
        assertEquals(0.5F, mesh.u(1));
        assertEquals(1.0F, mesh.v(1));
        assertEquals(1, mesh.index(0));
        assertEquals(0, mesh.materialStart(0));
        assertEquals(3, mesh.materialCount(0));
        assertEquals(0.5F, mesh.materialAlpha(0));
    }
}
