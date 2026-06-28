package com.micheanl.model.client.nativebridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class MMDModelKindTest {
    @Test
    void mapsKnownNativeCodes() {
        assertEquals(MMDModelKind.PMD, MMDModelKind.fromCode(10));
        assertEquals(MMDModelKind.PMX, MMDModelKind.fromCode(11));
    }

    @Test
    void rejectsUnknownNativeCode() {
        assertThrows(IllegalArgumentException.class, () -> MMDModelKind.fromCode(99));
    }
}
