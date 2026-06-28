package com.micheanl.model.client.render;

import com.micheanl.model.client.nativebridge.MMDModelMesh;
import com.micheanl.model.client.nativebridge.MMDModelSkeleton;
import com.micheanl.model.client.nativebridge.MMDSampledPose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class MMDMeshEmitter {
    private static final float PLAYER_HEIGHT = 1.8F;
    private static final int RGB = 230;

    private MMDMeshEmitter() {
    }

    public static void emit(MMDModelMesh mesh, Matrix4f pose, VertexConsumer consumer, Transform transform, float alphaScale) {
        emit(mesh, null, null, pose, consumer, transform, alphaScale);
    }

    public static void emit(MMDModelMesh mesh, MMDModelSkeleton skeleton, MMDSampledPose sampledPose, Matrix4f pose, VertexConsumer consumer, Transform transform, float alphaScale) {
        Vector3f position = new Vector3f();
        Vector3f skinned = new Vector3f();
        int materialCount = mesh.materialCount();
        for (int material = 0; material < materialCount; material++) {
            int start = mesh.materialStart(material);
            int end = Math.min(start + mesh.materialCount(material), mesh.indexCount());
            int alpha = alpha(mesh.materialAlpha(material) * alphaScale);
            for (int offset = start; offset < end; offset++) {
                int vertex = mesh.index(offset);
                float x = mesh.positionX(vertex);
                float y = mesh.positionY(vertex);
                float z = mesh.positionZ(vertex);
                if (skeleton != null && sampledPose != null && vertex < skeleton.skinningVertexCount()) {
                    skin(mesh, skeleton, sampledPose, vertex, skinned);
                    x = skinned.x();
                    y = skinned.y();
                    z = skinned.z();
                }
                x = (x - transform.centerX) * transform.scale;
                y = (y - transform.minY) * transform.scale;
                z = (z - transform.centerZ) * transform.scale;
                pose.transformPosition(x, y, z, position);
                consumer.addVertex(position.x(), position.y(), position.z()).setColor(RGB, RGB, RGB, alpha);
            }
        }
    }

    private static void skin(MMDModelMesh mesh, MMDModelSkeleton skeleton, MMDSampledPose sampledPose, int vertex, Vector3f target) {
        float sourceX = mesh.positionX(vertex);
        float sourceY = mesh.positionY(vertex);
        float sourceZ = mesh.positionZ(vertex);
        float x = 0.0F;
        float y = 0.0F;
        float z = 0.0F;
        float totalWeight = 0.0F;
        float[] matrices = sampledPose.rawSkinningMatrices();
        for (int influence = 0; influence < 4; influence++) {
            float weight = skeleton.skinWeight(vertex, influence);
            if (weight == 0.0F) {
                continue;
            }
            totalWeight += weight;
            int bone = skeleton.skinIndex(vertex, influence);
            int matrix = bone * 16;
            if (bone < 0 || matrix + 15 >= matrices.length) {
                continue;
            }
            x += (matrices[matrix] * sourceX + matrices[matrix + 4] * sourceY + matrices[matrix + 8] * sourceZ + matrices[matrix + 12]) * weight;
            y += (matrices[matrix + 1] * sourceX + matrices[matrix + 5] * sourceY + matrices[matrix + 9] * sourceZ + matrices[matrix + 13]) * weight;
            z += (matrices[matrix + 2] * sourceX + matrices[matrix + 6] * sourceY + matrices[matrix + 10] * sourceZ + matrices[matrix + 14]) * weight;
        }
        if (totalWeight == 0.0F) {
            target.set(sourceX, sourceY, sourceZ);
            return;
        }
        target.set(x, y, z);
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
