package com.micheanl.model.client.nativebridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class MMDModelSummaryTest {
    @Test
    void exposesCounts() {
        MMDModelSummary summary = new MMDModelSummary(3, 9, 1, 2);

        assertEquals(3, summary.vertices());
        assertEquals(9, summary.indices());
        assertEquals(1, summary.materials());
        assertEquals(2, summary.bones());
    }

    @Test
    void rejectsNegativeCounts() {
        assertThrows(IllegalArgumentException.class, () -> new MMDModelSummary(-1, 0, 0, 0));
    }
}
