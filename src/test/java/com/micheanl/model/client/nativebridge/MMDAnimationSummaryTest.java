package com.micheanl.model.client.nativebridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class MMDAnimationSummaryTest {
    @Test
    void exposesCounts() {
        MMDAnimationSummary summary = new MMDAnimationSummary(42, 7, 6, 5, 4, 3, 2);

        assertEquals(42, summary.maxFrame());
        assertEquals(7, summary.boneFrames());
        assertEquals(6, summary.morphFrames());
        assertEquals(5, summary.cameraFrames());
        assertEquals(4, summary.lightFrames());
        assertEquals(3, summary.selfShadowFrames());
        assertEquals(2, summary.propertyFrames());
    }

    @Test
    void rejectsNegativeCounts() {
        assertThrows(IllegalArgumentException.class, () -> new MMDAnimationSummary(-1, 0, 0, 0, 0, 0, 0));
    }
}
