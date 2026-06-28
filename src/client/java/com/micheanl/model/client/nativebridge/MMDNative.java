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

    private static native int modelSkeletonCountsRaw(long handle, long[] outCounts);

    private static native int modelSkeletonReadRaw(
            long handle,
            int[] parentIndices,
            float[] positions,
            int[] skinIndices,
            float[] skinWeights
    );

    private static native int animationSummaryRaw(String path, long[] outSummary);

    private static native int animationLoadRaw(long model, String path, long[] outAnimation);

    private static native int animationDestroyRaw(long handle);

    private static native int animationSampleRaw(long handle, float frame, float[] outSkinningMatrices);

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

    static MMDModelSkeleton modelSkeleton(long handle) {
        long[] counts = new long[4];
        NativeStatus status = NativeStatus.fromCode(modelSkeletonCountsRaw(handle, counts));
        if (status != NativeStatus.OK) {
            throw new IllegalStateException("Native model skeleton counts failed: " + status);
        }
        int[] parentIndices = new int[checkedArrayLength(counts[0])];
        float[] positions = new float[checkedArrayLength(counts[1])];
        int[] skinIndices = new int[checkedArrayLength(counts[2])];
        float[] skinWeights = new float[checkedArrayLength(counts[3])];
        status = NativeStatus.fromCode(modelSkeletonReadRaw(handle, parentIndices, positions, skinIndices, skinWeights));
        if (status != NativeStatus.OK) {
            throw new IllegalStateException("Native model skeleton read failed: " + status);
        }
        return new MMDModelSkeleton(parentIndices, positions, skinIndices, skinWeights);
    }

    public static MMDAnimationSummary animationSummary(java.nio.file.Path path) {
        long[] summary = new long[7];
        NativeStatus status = NativeStatus.fromCode(animationSummaryRaw(path.toAbsolutePath().toString(), summary));
        if (status != NativeStatus.OK) {
            throw new IllegalStateException("Native animation summary failed: " + status);
        }
        return new MMDAnimationSummary(summary[0], summary[1], summary[2], summary[3], summary[4], summary[5], summary[6]);
    }

    static long animationLoad(long model, String path) {
        long[] animation = new long[1];
        NativeStatus status = NativeStatus.fromCode(animationLoadRaw(model, path, animation));
        if (status != NativeStatus.OK) {
            throw new IllegalStateException("Native animation load failed: " + status);
        }
        return animation[0];
    }

    static NativeStatus animationDestroy(long handle) {
        return NativeStatus.fromCode(animationDestroyRaw(handle));
    }

    static MMDSampledPose animationSample(long handle, float frame, int boneCount) {
        float[] matrices = new float[checkedArrayLength(Math.multiplyExact((long) boneCount, 16L))];
        NativeStatus status = NativeStatus.fromCode(animationSampleRaw(handle, frame, matrices));
        if (status != NativeStatus.OK) {
            throw new IllegalStateException("Native animation sample failed: " + status);
        }
        return new MMDSampledPose(matrices);
    }

    private static int checkedArrayLength(long value) {
        if (value < 0 || value > Integer.MAX_VALUE) {
            throw new IllegalStateException("Native array is too large: " + value);
        }
        return (int) value;
    }
}
