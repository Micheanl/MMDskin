package com.micheanl.model.client.nativebridge;

import java.util.Objects;

public final class MMDNativeModel implements AutoCloseable {
    @FunctionalInterface
    interface Destroyer {
        NativeStatus destroy(long handle);
    }

    private final Destroyer destroyer;
    private long handle;

    MMDNativeModel(long handle, Destroyer destroyer) {
        if (handle <= 0) {
            throw new IllegalArgumentException("Invalid native model handle");
        }
        this.handle = handle;
        this.destroyer = Objects.requireNonNull(destroyer, "destroyer");
    }

    public long handle() {
        long value = this.handle;
        if (value == 0) {
            throw new IllegalStateException("Native model is closed");
        }
        return value;
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
