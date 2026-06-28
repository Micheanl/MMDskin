package com.micheanl.model.client.nativebridge;

public final class MMDNative {
    private MMDNative() {
    }

    public static native int nativeVersion();

    private static native long engineCreateRaw();

    private static native int engineDestroyRaw(long handle);

    private static native int modelLoadRaw(long engine, String path, long[] outModel);

    private static native int modelDestroyRaw(long handle);

    public static MMDNativeEngine engineCreate() {
        return new MMDNativeEngine(engineCreateRaw(), MMDNative::engineDestroy);
    }

    public static NativeStatus engineDestroy(long handle) {
        return NativeStatus.fromCode(engineDestroyRaw(handle));
    }

    static long modelLoad(long engine, String path) {
        long[] model = new long[1];
        NativeStatus status = NativeStatus.fromCode(modelLoadRaw(engine, path, model));
        if (status != NativeStatus.OK) {
            throw new IllegalStateException("Native model load failed: " + status);
        }
        return model[0];
    }

    static NativeStatus modelDestroy(long handle) {
        return NativeStatus.fromCode(modelDestroyRaw(handle));
    }
}
