package com.micheanl.model.client.nativebridge;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class MMDAnimationClipTest {
    @Test
    void rejectsInvalidHandle() {
        assertThrows(IllegalArgumentException.class, () -> new MMDAnimationClip(0, handle -> NativeStatus.OK));
    }

    @Test
    void samplesPoseThroughReader() {
        MMDAnimationClip clip = new MMDAnimationClip(
                7,
                handle -> NativeStatus.OK,
                (handle, frame, boneCount) -> {
                    assertEquals(7, handle);
                    assertEquals(12.0F, frame);
                    assertEquals(3, boneCount);
                    return new MMDSampledPose(new float[48]);
                }
        );

        assertEquals(3, clip.sample(12.0F, 3).boneCount());
    }

    @Test
    void closesOnlyOnce() {
        AtomicInteger closes = new AtomicInteger();
        MMDAnimationClip clip = new MMDAnimationClip(7, handle -> {
            closes.incrementAndGet();
            return NativeStatus.OK;
        });

        clip.close();
        clip.close();

        assertEquals(1, closes.get());
        assertThrows(IllegalStateException.class, clip::handle);
    }

    @Test
    void engineLoadsAnimationThroughLoader() {
        MMDNativeEngine engine = new MMDNativeEngine(
                11,
                handle -> NativeStatus.OK,
                (engineHandle, path) -> 23,
                handle -> NativeStatus.OK,
                (modelHandle, path) -> {
                    assertEquals(23, modelHandle);
                    assertEquals(Path.of("idle.vmd").toAbsolutePath().toString(), path);
                    return 31;
                },
                handle -> NativeStatus.OK
        );

        try (MMDNativeModel model = engine.loadModel(Path.of("model.pmx"));
             MMDAnimationClip clip = model.loadAnimation(Path.of("idle.vmd"))) {
            assertEquals(31, clip.handle());
        }
    }
}
