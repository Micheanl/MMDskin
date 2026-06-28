package com.micheanl.model.client.nativebridge;

public final class MMDNative {
    private MMDNative() {
    }

    public static native int nativeVersion();

    private static native long engineCreateRaw();

    private static native int engineDestroyRaw(long handle);

    private static native int modelLoadRaw(long engine, String path, long[] outModel);

    private static native int modelDestroyRaw(long handle);

    private static native int modelKindRaw(long handle);

    private static native int modelSummaryRaw(long handle, long[] outSummary);

    private static native int modelMeshCountsRaw(long handle, long[] outCounts);

    private static native int modelMeshReadRaw(
            long handle,
            float[] positions,
            float[] normals,
            float[] uvs,
            int[] indices,
            int[] materialStarts,
            int[] materialCounts,
            float[] materialAlphas
    );

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

    static MMDModelKind modelKind(long handle) {
        return MMDModelKind.fromCode(modelKindRaw(handle));
    }

    static MMDModelSummary modelSummary(long handle) {
        long[] summary = new long[4];
        NativeStatus status = NativeStatus.fromCode(modelSummaryRaw(handle, summary));
        if (status != NativeStatus.OK) {
            throw new IllegalStateException("Native model summary failed: " + status);
        }
        return new MMDModelSummary(summary[0], summary[1], summary[2], summary[3]);
    }

    static MMDModelMesh modelMesh(long handle) {
        long[] counts = new long[5];
        NativeStatus status = NativeStatus.fromCode(modelMeshCountsRaw(handle, counts));
        if (status != NativeStatus.OK) {
            throw new IllegalStateException("Native model mesh counts failed: " + status);
        }
        float[] positions = new float[checkedArrayLength(counts[0])];
        float[] normals = new float[checkedArrayLength(counts[1])];
        float[] uvs = new float[checkedArrayLength(counts[2])];
        int[] indices = new int[checkedArrayLength(counts[3])];
        int[] materialStarts = new int[checkedArrayLength(counts[4])];
        int[] materialCounts = new int[materialStarts.length];
        float[] materialAlphas = new float[materialStarts.length];
        status = NativeStatus.fromCode(modelMeshReadRaw(
                handle,
                positions,
                normals,
                uvs,
                indices,
                materialStarts,
                materialCounts,
                materialAlphas
        ));
        if (status != NativeStatus.OK) {
            throw new IllegalStateException("Native model mesh read failed: " + status);
        }
        return new MMDModelMesh(positions, normals, uvs, indices, materialStarts, materialCounts, materialAlphas);
    }

    private static int checkedArrayLength(long value) {
        if (value < 0 || value > Integer.MAX_VALUE) {
            throw new IllegalStateException("Native array is too large: " + value);
        }
        return (int) value;
    }
}
