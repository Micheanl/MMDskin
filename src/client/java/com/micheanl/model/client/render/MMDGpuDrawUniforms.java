package com.micheanl.model.client.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;

import java.nio.ByteBuffer;

final class MMDGpuDrawUniforms implements AutoCloseable {
    private static final int UNIFORM_SIZE = new Std140SizeCalculator().putVec4().get();

    private final MMDGpuUniforms<DrawUniform> uniforms = new MMDGpuUniforms<>("MMD Draw UBO", UNIFORM_SIZE, 16);

    GpuBufferSlice write(float alpha) {
        return this.uniforms.write(new DrawUniform(alpha));
    }

    void endFrame() {
        this.uniforms.endFrame();
    }

    @Override
    public void close() {
        this.uniforms.close();
    }

    private record DrawUniform(float alpha) implements MMDGpuUniforms.Uniform {
        @Override
        public void write(ByteBuffer byteBuffer) {
            Std140Builder.intoBuffer(byteBuffer).putVec4(this.alpha, 0.0F, 0.0F, 0.0F);
        }
    }
}
