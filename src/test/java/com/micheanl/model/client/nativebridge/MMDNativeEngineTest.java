package com.micheanl.model.client.nativebridge;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class MMDNativeEngineTest {
    @Test
    void rejectsInvalidHandle() {
        assertThrows(IllegalArgumentException.class, () -> new MMDNativeEngine(0, handle -> NativeStatus.OK));
    }

    @Test
    void exposesOpenHandle() {
        try (MMDNativeEngine engine = new MMDNativeEngine(7, handle -> NativeStatus.OK)) {
            assertEquals(7, engine.handle());
        }
    }

    @Test
    void closesOnlyOnce() {
        AtomicInteger closes = new AtomicInteger();
        MMDNativeEngine engine = new MMDNativeEngine(7, handle -> {
            closes.incrementAndGet();
            assertEquals(7, handle);
            return NativeStatus.OK;
        });

        engine.close();
        engine.close();

        assertEquals(1, closes.get());
        assertThrows(IllegalStateException.class, engine::handle);
    }

    @Test
    void failsWhenNativeDestroyFails() {
        MMDNativeEngine engine = new MMDNativeEngine(7, handle -> NativeStatus.NOT_FOUND);

        assertThrows(IllegalStateException.class, engine::close);
    }
}
