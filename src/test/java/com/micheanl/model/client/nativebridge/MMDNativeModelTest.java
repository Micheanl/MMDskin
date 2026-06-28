package com.micheanl.model.client.nativebridge;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class MMDNativeModelTest {
    @Test
    void rejectsInvalidHandle() {
        assertThrows(IllegalArgumentException.class, () -> new MMDNativeModel(0, handle -> NativeStatus.OK));
    }

    @Test
    void exposesOpenHandle() {
        try (MMDNativeModel model = new MMDNativeModel(7, handle -> NativeStatus.OK)) {
            assertEquals(7, model.handle());
        }
    }

    @Test
    void exposesModelKind() {
        try (MMDNativeModel model = new MMDNativeModel(7, handle -> NativeStatus.OK, handle -> MMDModelKind.PMX)) {
            assertEquals(MMDModelKind.PMX, model.kind());
        }
    }

    @Test
    void exposesModelSummary() {
        MMDModelSummary summary = new MMDModelSummary(3, 9, 1, 2);

        try (MMDNativeModel model = new MMDNativeModel(
                7,
                handle -> NativeStatus.OK,
                handle -> MMDModelKind.PMX,
                handle -> summary
        )) {
            assertEquals(summary, model.summary());
        }
    }

    @Test
    void closesOnlyOnce() {
        AtomicInteger closes = new AtomicInteger();
        MMDNativeModel model = new MMDNativeModel(7, handle -> {
            closes.incrementAndGet();
            assertEquals(7, handle);
            return NativeStatus.OK;
        });

        model.close();
        model.close();

        assertEquals(1, closes.get());
        assertThrows(IllegalStateException.class, model::handle);
    }

    @Test
    void engineLoadsModelThroughLoader() {
        MMDNativeEngine engine = new MMDNativeEngine(11, handle -> NativeStatus.OK, (engineHandle, path) -> {
            assertEquals(11, engineHandle);
            assertEquals(Path.of("model.pmx").toAbsolutePath().toString(), path);
            return 23;
        }, handle -> NativeStatus.OK);

        try (MMDNativeModel model = engine.loadModel(Path.of("model.pmx"))) {
            assertEquals(23, model.handle());
        }
    }
}
