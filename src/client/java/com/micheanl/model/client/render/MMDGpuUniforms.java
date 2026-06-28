package com.micheanl.model.client.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.util.Mth;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

final class MMDGpuUniforms<T extends MMDGpuUniforms.Uniform> implements AutoCloseable {
    private final String label;
    private final List<MappableRingBuffer> oldBuffers = new ArrayList<>();
    private final int blockSize;
    private MappableRingBuffer ringBuffer;
    private int capacity;
    private int nextBlock;

    MMDGpuUniforms(String label, int uniformSize, int initialCapacity) {
        this.label = label;
        int alignment = com.mojang.blaze3d.systems.RenderSystem.getDevice().getDeviceInfo().limits().minUniformOffsetAlignment();
        this.blockSize = Mth.roundToward(uniformSize, alignment);
        this.capacity = Mth.smallestEncompassingPowerOfTwo(initialCapacity);
        this.ringBuffer = new MappableRingBuffer(() -> label, 130, this.blockSize * this.capacity);
    }

    GpuBufferSlice write(T uniform) {
        if (this.nextBlock >= this.capacity) {
            resize(this.capacity * 2);
        }
        int offset = this.nextBlock * this.blockSize;
        try (GpuBufferSlice.MappedView view = this.ringBuffer.currentBuffer().slice(offset, this.blockSize).map(false, true)) {
            uniform.write(view.data());
        }
        this.nextBlock++;
        return this.ringBuffer.currentBuffer().slice(offset, this.blockSize);
    }

    void endFrame() {
        this.nextBlock = 0;
        this.ringBuffer.rotate();
        for (MappableRingBuffer oldBuffer : this.oldBuffers) {
            oldBuffer.close();
        }
        this.oldBuffers.clear();
    }

    @Override
    public void close() {
        for (MappableRingBuffer oldBuffer : this.oldBuffers) {
            oldBuffer.close();
        }
        this.oldBuffers.clear();
        this.ringBuffer.close();
    }

    private void resize(int newCapacity) {
        this.oldBuffers.add(this.ringBuffer);
        this.capacity = newCapacity;
        this.nextBlock = 0;
        this.ringBuffer = new MappableRingBuffer(() -> this.label, 130, this.blockSize * this.capacity);
    }

    interface Uniform {
        void write(ByteBuffer byteBuffer);
    }
}
