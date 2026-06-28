package com.micheanl.model.client.nativebridge;

public final class MMDNative {
    private MMDNative() {
    }

    public static native int nativeVersion();

    private static native long engineCreateRaw();

    private static native int engineDestroyRaw(long handle);

    public static long engineCreate() {
        return engineCreateRaw();
    }

    public static NativeStatus engineDestroy(long handle) {
        return NativeStatus.fromCode(engineDestroyRaw(handle));
    }
}
