package com.micheanl.model.client.render;

import com.micheanl.model.client.nativebridge.MMDModelMesh;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class MMDMeshEmitter {
    private static final float PLAYER_HEIGHT = 1.8F;
    private static final int RGB = 230;

    private MMDMeshEmitter() {
    }

    public static void emit(MMDModelMesh mesh, Matrix4f pose, VertexConsumer consumer, Transform transform, float alphaScale) {
        Vector3f position = new Vector3f();
        int materialCount = mesh.materialCount();
        for (int material = 0; material < materialCount; material++) {
            int start = mesh.materialStart(material);
            int end = Math.min(start + mesh.materialCount(material), mesh.indexCount());
            int alpha = alpha(mesh.materialAlpha(material) * alphaScale);
            for (int offset = start; offset < end; offset++) {
                int vertex = mesh.index(offset);
                float x = (mesh.positionX(vertex) - transform.centerX) * transform.scale;
                float y = (mesh.positionY(vertex) - transform.minY) * transform.scale;
                float z = (mesh.positionZ(vertex) - transform.centerZ) * transform.scale;
                pose.transformPosition(x, y, z, position);
                consumer.addVertex(position.x(), position.y(), position.z()).setColor(RGB, RGB, RGB, alpha);
            }
        }
    }

    private static int alpha(float value) {
        if (value <= 0.0F) {
            return 0;
        }
        if (value >= 1.0F) {
            return 255;
        }
        return (int) (value * 255.0F);
    }

    public record Transform(float scale, float centerX, float minY, float centerZ) {
        public static Transform player(MMDModelMesh mesh) {
            float height = mesh.maxY() - mesh.minY();
            float scale = height > 0.0F ? PLAYER_HEIGHT / height : 1.0F;
            return new Transform(scale, (mesh.minX() + mesh.maxX()) * 0.5F, mesh.minY(), (mesh.minZ() + mesh.maxZ()) * 0.5F);
        }
    }
}
