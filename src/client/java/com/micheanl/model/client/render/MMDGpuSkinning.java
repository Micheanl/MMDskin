package com.micheanl.model.client.render;

import com.micheanl.model.client.nativebridge.MMDSampledPose;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.nio.ByteBuffer;

final class MMDGpuSkinning implements AutoCloseable {
    static final int MAX_BONES = 256;
    private static final int UNIFORM_SIZE = new Std140SizeCalculator().putMat4f().get() * MAX_BONES;

    private final MMDGpuUniforms<BoneUniform> uniforms = new MMDGpuUniforms<>("MMD Bones UBO", UNIFORM_SIZE, 8);

    GpuBufferSlice write(Matrix4fc entityPose, MMDSampledPose pose, MMDMeshEmitter.Transform transform) {
        return this.uniforms.write(new BoneUniform(entityPose, pose, transform));
    }

    void endFrame() {
        this.uniforms.endFrame();
    }

    @Override
    public void close() {
        this.uniforms.close();
    }

    private record BoneUniform(Matrix4fc entityPose, MMDSampledPose pose, MMDMeshEmitter.Transform transform) implements MMDGpuUniforms.Uniform {
        @Override
        public void write(ByteBuffer byteBuffer) {
            float[] source = this.pose.rawSkinningMatrices();
            Matrix4f coordinate = new Matrix4f(this.entityPose)
                    .scale(this.transform.scale())
                    .translate(-this.transform.centerX(), -this.transform.minY(), -this.transform.centerZ());
            Matrix4f skin = new Matrix4f();
            Matrix4f matrix = new Matrix4f();
            for (int bone = 0; bone < MAX_BONES; bone++) {
                matrix.identity();
                if (bone < this.pose.boneCount()) {
                    skin.set(source, bone * 16);
                    matrix.set(coordinate).mul(skin);
                }
                Std140Builder.intoBuffer(byteBuffer).putMat4f(matrix);
            }
        }
    }
}
