package com.micheanl.model.client.nativebridge;

public record MMDAnimationSummary(
        long maxFrame,
        long boneFrames,
        long morphFrames,
        long cameraFrames,
        long lightFrames,
        long selfShadowFrames,
        long propertyFrames
) {
    public MMDAnimationSummary {
        if (maxFrame < 0 || boneFrames < 0 || morphFrames < 0 || cameraFrames < 0 || lightFrames < 0 || selfShadowFrames < 0 || propertyFrames < 0) {
            throw new IllegalArgumentException("Animation summary counts must be non-negative");
        }
    }
}
