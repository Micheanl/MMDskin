package com.micheanl.model.client.nativebridge;

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

    private final Destroyer destroyer;
    private final KindReader kindReader;
    private final SummaryReader summaryReader;
    private long handle;

    MMDNativeModel(long handle, Destroyer destroyer) {
        this(handle, destroyer, MMDNative::modelKind);
    }

    MMDNativeModel(long handle, Destroyer destroyer, KindReader kindReader) {
        this(handle, destroyer, kindReader, MMDNative::modelSummary);
    }

    MMDNativeModel(long handle, Destroyer destroyer, KindReader kindReader, SummaryReader summaryReader) {
        if (handle <= 0) {
            throw new IllegalArgumentException("Invalid native model handle");
        }
        this.handle = handle;
        this.destroyer = Objects.requireNonNull(destroyer, "destroyer");
        this.kindReader = Objects.requireNonNull(kindReader, "kindReader");
        this.summaryReader = Objects.requireNonNull(summaryReader, "summaryReader");
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
