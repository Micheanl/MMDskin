package com.micheanl.model.client.nativebridge;

import java.nio.file.Path;
import java.util.Objects;

public final class MMDNativeEngine implements AutoCloseable {
    @FunctionalInterface
    interface Destroyer {
        NativeStatus destroy(long handle);
    }

    @FunctionalInterface
    interface ModelLoader {
        long load(long engine, String path);
    }

    private final Destroyer destroyer;
    private final MMDNativeModel.Destroyer modelDestroyer;
    private final MMDNativeModel.AnimationLoader animationLoader;
    private final MMDAnimationClip.Destroyer animationDestroyer;
    private final ModelLoader modelLoader;
    private long handle;

    MMDNativeEngine(long handle, Destroyer destroyer) {
        this(handle, destroyer, MMDNative::modelLoad, MMDNative::modelDestroy);
    }

    MMDNativeEngine(long handle, Destroyer destroyer, ModelLoader modelLoader, MMDNativeModel.Destroyer modelDestroyer) {
        this(handle, destroyer, modelLoader, modelDestroyer, MMDNative::animationLoad, MMDNative::animationDestroy);
    }

    MMDNativeEngine(
            long handle,
            Destroyer destroyer,
            ModelLoader modelLoader,
            MMDNativeModel.Destroyer modelDestroyer,
            MMDNativeModel.AnimationLoader animationLoader,
            MMDAnimationClip.Destroyer animationDestroyer
    ) {
        if (handle <= 0) {
            throw new IllegalArgumentException("Invalid native engine handle");
        }
        this.handle = handle;
        this.destroyer = Objects.requireNonNull(destroyer, "destroyer");
        this.modelLoader = Objects.requireNonNull(modelLoader, "modelLoader");
        this.modelDestroyer = Objects.requireNonNull(modelDestroyer, "modelDestroyer");
        this.animationLoader = Objects.requireNonNull(animationLoader, "animationLoader");
        this.animationDestroyer = Objects.requireNonNull(animationDestroyer, "animationDestroyer");
    }

    public long handle() {
        long value = this.handle;
        if (value == 0) {
            throw new IllegalStateException("Native engine is closed");
        }
        return value;
    }

    public MMDNativeModel loadModel(Path path) {
        Objects.requireNonNull(path, "path");
        long model = this.modelLoader.load(handle(), path.toAbsolutePath().toString());
        return new MMDNativeModel(model, this.modelDestroyer, this.animationLoader, this.animationDestroyer);
    }

    @Override
    public void close() {
        long value = this.handle;
        if (value == 0) {
            return;
        }
        NativeStatus status = this.destroyer.destroy(value);
        if (status != NativeStatus.OK) {
            throw new IllegalStateException("Native engine destroy failed: " + status);
        }
        this.handle = 0;
    }
}
