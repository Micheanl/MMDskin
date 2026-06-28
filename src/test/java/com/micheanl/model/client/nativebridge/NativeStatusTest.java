package com.micheanl.model.client.nativebridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class NativeStatusTest {
    @Test
    void mapsKnownNativeCodes() {
        assertEquals(NativeStatus.OK, NativeStatus.fromCode(0));
        assertEquals(NativeStatus.INVALID_ARGUMENT, NativeStatus.fromCode(1));
        assertEquals(NativeStatus.NOT_FOUND, NativeStatus.fromCode(2));
        assertEquals(NativeStatus.INTERNAL_ERROR, NativeStatus.fromCode(3));
    }

    @Test
    void rejectsUnknownNativeCode() {
        assertThrows(IllegalArgumentException.class, () -> NativeStatus.fromCode(99));
    }
}
