package com.micheanl.model.client.render;

import com.micheanl.model.client.nativebridge.MMDModelMesh;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class MMDMeshEmitterTest {
    @Test
    void normalizesMeshToPlayerHeight() {
        MMDModelMesh mesh = new MMDModelMesh(
                new float[] {
                        -1.0F, 0.0F, -1.0F,
                        1.0F, 20.0F, 1.0F,
                        0.0F, 10.0F, 0.0F
                },
                new float[] {
                        0.0F, 1.0F, 0.0F,
                        0.0F, 1.0F, 0.0F,
                        0.0F, 1.0F, 0.0F
                },
                new float[] {
                        0.0F, 0.0F,
                        1.0F, 1.0F,
                        0.5F, 0.5F
                },
                new int[] {0, 1, 2},
                new int[] {0},
                new int[] {3},
                new float[] {1.0F}
        );

        MMDMeshEmitter.Transform transform = MMDMeshEmitter.Transform.player(mesh);

        assertEquals(0.09F, transform.scale(), 0.000001F);
        assertEquals(0.0F, transform.centerX());
        assertEquals(0.0F, transform.minY());
        assertEquals(0.0F, transform.centerZ());
    }

    @Test
    void emitsIndexedTrianglesWithMaterialAlpha() {
        MMDModelMesh mesh = new MMDModelMesh(
                new float[] {
                        -1.0F, 0.0F, -1.0F,
                        1.0F, 20.0F, 1.0F,
                        0.0F, 10.0F, 0.0F
                },
                new float[] {
                        0.0F, 1.0F, 0.0F,
                        0.0F, 1.0F, 0.0F,
                        0.0F, 1.0F, 0.0F
                },
                new float[] {
                        0.0F, 0.0F,
                        1.0F, 1.0F,
                        0.5F, 0.5F
                },
                new int[] {2, 0, 1},
                new int[] {0},
                new int[] {3},
                new float[] {0.5F}
        );
        CapturingVertexConsumer consumer = new CapturingVertexConsumer();

        MMDMeshEmitter.emit(mesh, new Matrix4f(), consumer, MMDMeshEmitter.Transform.player(mesh), 0.8F);

        assertEquals(3, consumer.vertices.size());
        assertEquals(0.0F, consumer.vertices.get(0).x);
        assertEquals(0.9F, consumer.vertices.get(0).y);
        assertEquals(0.0F, consumer.vertices.get(0).z);
        assertEquals(102, consumer.vertices.get(0).alpha);
    }

    private static final class CapturingVertexConsumer implements VertexConsumer {
        private final List<Vertex> vertices = new ArrayList<>();

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.vertices.add(new Vertex(x, y, z, 0));
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            int last = this.vertices.size() - 1;
            Vertex vertex = this.vertices.get(last);
            this.vertices.set(last, new Vertex(vertex.x, vertex.y, vertex.z, a));
            return this;
        }

        @Override
        public VertexConsumer setColor(int color) {
            int alpha = color >>> 24 & 255;
            int red = color >>> 16 & 255;
            int green = color >>> 8 & 255;
            int blue = color & 255;
            return setColor(red, green, blue, alpha);
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            return this;
        }
    }

    private record Vertex(float x, float y, float z, int alpha) {
    }
}
