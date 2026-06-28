package com.micheanl.model.client.imgui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class MMDImGuiDrawDataTest {
    @Test
    void expandsIndexedTrianglesIntoVertices() {
        MMDImGuiDrawData data = new MMDImGuiDrawData(
                32,
                64,
                100,
                200,
                new MMDImGuiDrawData.Vertex[]{
                        new MMDImGuiDrawData.Vertex(1.0F, 2.0F, 0.0F, 0.0F, 0xFF112233),
                        new MMDImGuiDrawData.Vertex(3.0F, 4.0F, 1.0F, 0.0F, 0xFF445566),
                        new MMDImGuiDrawData.Vertex(5.0F, 6.0F, 1.0F, 1.0F, 0xFF778899)
                },
                new int[]{2, 1, 0},
                new MMDImGuiDrawData.Command[]{
                        new MMDImGuiDrawData.Command(0, 3, 0, 0, 0, 32, 64)
                }
        );

        MMDImGuiDrawData.Expanded expanded = data.expand();

        assertEquals(3, expanded.vertices().length);
        assertEquals(5.0F, expanded.vertices()[0].x());
        assertEquals(3.0F, expanded.vertices()[1].x());
        assertEquals(1.0F, expanded.vertices()[2].x());
        assertEquals(1, expanded.commands().length);
        assertEquals(3, expanded.commands()[0].vertexCount());
    }
}
