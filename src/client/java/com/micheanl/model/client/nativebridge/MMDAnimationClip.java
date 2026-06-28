package com.micheanl.model.client.nativebridge;

import java.util.Objects;

public final class MMDAnimationClip implements AutoCloseable {
    @FunctionalInterface
    interface Destroyer {
        NativeStatus destroy(long handle);
    }

    @FunctionalInterface
    interface PoseReader {
        MMDSampledPose sample(long handle, float frame, int boneCount);
    }

    private final Destroyer destroyer;
    private final PoseReader poseReader;
    private long handle;

    MMDAnimationClip(long handle, Destroyer destroyer) {
        this(handle, destroyer, MMDNative::animationSample);
    }

    MMDAnimationClip(long handle, Destroyer destroyer, PoseReader poseReader) {
        if (handle <= 0) {
            throw new IllegalArgumentException("Invalid native animation handle");
        }
        this.handle = handle;
        this.destroyer = Objects.requireNonNull(destroyer, "destroyer");
        this.poseReader = Objects.requireNonNull(poseReader, "poseReader");
    }

    public long handle() {
        long value = this.handle;
        if (value == 0) {
            throw new IllegalStateException("Native animation is closed");
        }
        return value;
    }

    public MMDSampledPose sample(float frame, int boneCount) {
        if (boneCount < 0) {
            throw new IllegalArgumentException("Invalid bone count");
        }
        return this.poseReader.sample(handle(), frame, boneCount);
    }

    @Override
    public void close() {
        long value = this.handle;
        if (value == 0) {
            return;
        }
        NativeStatus status = this.destroyer.destroy(value);
        if (status != NativeStatus.OK) {
            throw new IllegalStateException("Native animation destroy failed: " + status);
        }
        this.handle = 0;
    }
}
