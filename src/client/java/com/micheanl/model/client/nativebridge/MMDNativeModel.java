package com.micheanl.model.client.nativebridge;

import java.nio.file.Path;
import java.util.Objects;

public final class MMDNativeModel implements AutoCloseable {
    @FunctionalInterface
    interface Destroyer {
        NativeStatus destroy(long handle);
    }

    @FunctionalInterface
    interface KindReader {
        MMDModelKind kind(long handle);
    }

    @FunctionalInterface
    interface SummaryReader {
        MMDModelSummary summary(long handle);
    }

    @FunctionalInterface
    interface MeshReader {
        MMDModelMesh mesh(long handle);
    }

    @FunctionalInterface
    interface SkeletonReader {
        MMDModelSkeleton skeleton(long handle);
    }

    @FunctionalInterface
    interface AnimationLoader {
        long load(long model, String path);
    }

    private final Destroyer destroyer;
    private final AnimationLoader animationLoader;
    private final MMDAnimationClip.Destroyer animationDestroyer;
    private final KindReader kindReader;
    private final SummaryReader summaryReader;
    private final MeshReader meshReader;
    private final SkeletonReader skeletonReader;
    private long handle;

    MMDNativeModel(long handle, Destroyer destroyer) {
        this(handle, destroyer, MMDNative::animationLoad, MMDNative::animationDestroy);
    }

    MMDNativeModel(long handle, Destroyer destroyer, AnimationLoader animationLoader, MMDAnimationClip.Destroyer animationDestroyer) {
        this(handle, destroyer, MMDNative::modelKind, animationLoader, animationDestroyer);
    }

    MMDNativeModel(long handle, Destroyer destroyer, KindReader kindReader) {
        this(handle, destroyer, kindReader, MMDNative::animationLoad, MMDNative::animationDestroy);
    }

    MMDNativeModel(long handle, Destroyer destroyer, KindReader kindReader, AnimationLoader animationLoader, MMDAnimationClip.Destroyer animationDestroyer) {
        this(handle, destroyer, kindReader, MMDNative::modelSummary, animationLoader, animationDestroyer);
    }

    MMDNativeModel(long handle, Destroyer destroyer, KindReader kindReader, SummaryReader summaryReader) {
        this(handle, destroyer, kindReader, summaryReader, MMDNative::animationLoad, MMDNative::animationDestroy);
    }

    MMDNativeModel(long handle, Destroyer destroyer, KindReader kindReader, SummaryReader summaryReader, AnimationLoader animationLoader, MMDAnimationClip.Destroyer animationDestroyer) {
        this(handle, destroyer, kindReader, summaryReader, MMDNative::modelMesh, animationLoader, animationDestroyer);
    }

    MMDNativeModel(long handle, Destroyer destroyer, KindReader kindReader, SummaryReader summaryReader, MeshReader meshReader) {
        this(handle, destroyer, kindReader, summaryReader, meshReader, MMDNative::animationLoad, MMDNative::animationDestroy);
    }

    MMDNativeModel(long handle, Destroyer destroyer, KindReader kindReader, SummaryReader summaryReader, MeshReader meshReader, AnimationLoader animationLoader, MMDAnimationClip.Destroyer animationDestroyer) {
        this(handle, destroyer, kindReader, summaryReader, meshReader, MMDNative::modelSkeleton, animationLoader, animationDestroyer);
    }

    MMDNativeModel(long handle, Destroyer destroyer, KindReader kindReader, SummaryReader summaryReader, MeshReader meshReader, SkeletonReader skeletonReader) {
        this(handle, destroyer, kindReader, summaryReader, meshReader, skeletonReader, MMDNative::animationLoad, MMDNative::animationDestroy);
    }

    MMDNativeModel(
            long handle,
            Destroyer destroyer,
            KindReader kindReader,
            SummaryReader summaryReader,
            MeshReader meshReader,
            SkeletonReader skeletonReader,
            AnimationLoader animationLoader,
            MMDAnimationClip.Destroyer animationDestroyer
    ) {
        if (handle <= 0) {
            throw new IllegalArgumentException("Invalid native model handle");
        }
        this.handle = handle;
        this.destroyer = Objects.requireNonNull(destroyer, "destroyer");
        this.animationLoader = Objects.requireNonNull(animationLoader, "animationLoader");
        this.animationDestroyer = Objects.requireNonNull(animationDestroyer, "animationDestroyer");
        this.kindReader = Objects.requireNonNull(kindReader, "kindReader");
        this.summaryReader = Objects.requireNonNull(summaryReader, "summaryReader");
        this.meshReader = Objects.requireNonNull(meshReader, "meshReader");
        this.skeletonReader = Objects.requireNonNull(skeletonReader, "skeletonReader");
    }

    public long handle() {
        long value = this.handle;
        if (value == 0) {
            throw new IllegalStateException("Native model is closed");
        }
        return value;
    }

    public MMDModelKind kind() {
        return this.kindReader.kind(handle());
    }

    public MMDModelSummary summary() {
        return this.summaryReader.summary(handle());
    }

    public MMDModelMesh mesh() {
        return this.meshReader.mesh(handle());
    }

    public MMDModelSkeleton skeleton() {
        return this.skeletonReader.skeleton(handle());
    }

    public MMDAnimationClip loadAnimation(Path path) {
        Objects.requireNonNull(path, "path");
        long animation = this.animationLoader.load(handle(), path.toAbsolutePath().toString());
        return new MMDAnimationClip(animation, this.animationDestroyer);
    }

    @Override
    public void close() {
        long value = this.handle;
        if (value == 0) {
            return;
        }
        NativeStatus status = this.destroyer.destroy(value);
        if (status != NativeStatus.OK) {
            throw new IllegalStateException("Native model destroy failed: " + status);
        }
        this.handle = 0;
    }
}
