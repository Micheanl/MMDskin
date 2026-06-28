package com.micheanl.model.client.render;

import com.micheanl.model.client.nativebridge.MMDModelMesh;
import com.micheanl.model.client.nativebridge.MMDModelSkeleton;
import com.mojang.blaze3d.IndexType;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class MMDGpuMeshTest {
    @Test
    void buildsStaticEntityVerticesAndExplicitIndices() {
        MMDModelMesh mesh = new MMDModelMesh(
                new float[] {
                        0.0F, 0.0F, 0.0F,
                        1.0F, 0.0F, 0.0F,
                        0.0F, 1.0F, 0.0F
                },
                new float[] {
                        0.0F, 1.0F, 0.0F,
                        0.0F, 1.0F, 0.0F,
                        0.0F, 1.0F, 0.0F
                },
                new float[] {
                        0.25F, 0.75F,
                        0.0F, 0.0F,
                        0.0F, 0.0F
                },
                new int[] {2, 0, 1},
                new int[] {0},
                new int[] {3},
                new float[] {0.5F}
        );
        MMDModelSkeleton skeleton = new MMDModelSkeleton(
                new int[] {-1, 0, 1, 2},
                new float[12],
                new int[] {
                        1, 2, 3, 0,
                        0, 0, 0, 0,
                        0, 0, 0, 0
                },
                new float[] {
                        0.5F, 0.25F, 0.125F, 0.125F,
                        1.0F, 0.0F, 0.0F, 0.0F,
                        1.0F, 0.0F, 0.0F, 0.0F
                }
        );

        MMDGpuMesh.StaticData data = MMDGpuMesh.buildStaticData(mesh, skeleton);
        ByteBuffer vertices = data.vertices().duplicate().order(ByteOrder.nativeOrder());
        ByteBuffer indices = data.indices().duplicate().order(ByteOrder.nativeOrder());

        assertEquals(36 * 3, vertices.remaining());
        assertEquals(2 * 3, indices.remaining());
        assertEquals(IndexType.SHORT, data.indexType());
        assertEquals(3, data.indexCount());
        assertEquals(0.0F, vertices.getFloat(0));
        assertEquals(-1, vertices.get(15));
        assertEquals(0.25F, vertices.getFloat(16));
        assertEquals(0.75F, vertices.getFloat(20));
        assertEquals(1, vertices.getShort(24));
        assertEquals(2, vertices.getShort(26));
        assertEquals(3, vertices.getShort(28));
        assertEquals(0, vertices.getShort(30));
        assertEquals(0, vertices.get(32));
        assertEquals(-63, vertices.get(33));
        assertEquals(-95, vertices.get(34));
        assertEquals(2, indices.getShort(0));
        assertEquals(0, indices.getShort(2));
        assertEquals(1, indices.getShort(4));
        assertEquals(1, data.materialRanges().length);
        assertEquals(0, data.materialRanges()[0].firstIndex());
        assertEquals(3, data.materialRanges()[0].indexCount());
        assertEquals(0.5F, data.materialRanges()[0].alpha());
    }

    @Test
    void keepsIndirectCommandSlotsAlignedWithMaterials() {
        MMDModelMesh mesh = new MMDModelMesh(
                new float[] {
                        0.0F, 0.0F, 0.0F,
                        1.0F, 0.0F, 0.0F,
                        0.0F, 1.0F, 0.0F
                },
                new float[] {
                        0.0F, 1.0F, 0.0F,
                        0.0F, 1.0F, 0.0F,
                        0.0F, 1.0F, 0.0F
                },
                new float[] {
                        0.0F, 0.0F,
                        0.0F, 0.0F,
                        0.0F, 0.0F
                },
                new int[] {0, 1, 2},
                new int[] {0, 0},
                new int[] {0, 3},
                new float[] {1.0F, 0.25F}
        );
        MMDModelSkeleton skeleton = new MMDModelSkeleton(
                new int[] {-1},
                new float[3],
                new int[] {
                        0, 0, 0, 0,
                        0, 0, 0, 0,
                        0, 0, 0, 0
                },
                new float[] {
                        1.0F, 0.0F, 0.0F, 0.0F,
                        1.0F, 0.0F, 0.0F, 0.0F,
                        1.0F, 0.0F, 0.0F, 0.0F
                }
        );

        MMDGpuMesh.StaticData data = MMDGpuMesh.buildStaticData(mesh, skeleton);
        ByteBuffer commands = data.indirectCommands().duplicate().order(ByteOrder.nativeOrder());

        assertEquals(0, commands.getInt(0));
        assertEquals(3, commands.getInt(20));
        assertEquals(0, commands.getInt(28));
    }

    @Test
    void clampsInvalidBoneIndicesForShaderSafety() {
        MMDModelMesh mesh = new MMDModelMesh(
                new float[] {
                        0.0F, 0.0F, 0.0F,
                        1.0F, 0.0F, 0.0F,
                        0.0F, 1.0F, 0.0F
                },
                new float[] {
                        0.0F, 1.0F, 0.0F,
                        0.0F, 1.0F, 0.0F,
                        0.0F, 1.0F, 0.0F
                },
                new float[] {
                        0.0F, 0.0F,
                        0.0F, 0.0F,
                        0.0F, 0.0F
                },
                new int[] {0, 1, 2},
                new int[] {0},
                new int[] {3},
                new float[] {1.0F}
        );
        MMDModelSkeleton skeleton = new MMDModelSkeleton(
                new int[] {-1},
                new float[3],
                new int[] {
                        -1, 999, 0, 0,
                        0, 0, 0, 0,
                        0, 0, 0, 0
                },
                new float[] {
                        1.0F, 0.0F, 0.0F, 0.0F,
                        1.0F, 0.0F, 0.0F, 0.0F,
                        1.0F, 0.0F, 0.0F, 0.0F
                }
        );

        ByteBuffer vertices = MMDGpuMesh.buildStaticData(mesh, skeleton).vertices().duplicate().order(ByteOrder.nativeOrder());

        assertEquals(0, vertices.getShort(24));
        assertEquals(255, vertices.getShort(26));
    }
}
