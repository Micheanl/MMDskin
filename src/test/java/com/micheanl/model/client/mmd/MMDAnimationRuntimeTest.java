package com.micheanl.model.client.mmd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MMDAnimationRuntimeTest {
    @TempDir
    Path tempDir;

    @Test
    void extractsAndIndexesBundledVmdAnimations() throws Exception {
        byte[] zip = zip(
                entry("walk.vmd", new byte[] {1, 2, 3}),
                entry("idle.vmd", new byte[] {4, 5}),
                entry("ignore.txt", new byte[] {9})
        );

        List<MMDAnimationRuntime.AnimationEntry> entries = MMDAnimationRuntime.extractDefaultAnimations(
                new ByteArrayInputStream(zip),
                tempDir,
                path -> new com.micheanl.model.client.nativebridge.MMDAnimationSummary(1, 1, 0, 0, 0, 0, 0)
        );

        assertEquals(2, entries.size());
        assertEquals("idle", entries.get(0).name());
        assertEquals("walk", entries.get(1).name());
        assertEquals(MMDPlayerAction.IDLE, entries.get(0).action());
        assertEquals(MMDPlayerAction.WALK, entries.get(1).action());
        assertTrue(Files.exists(tempDir.resolve("idle.vmd")));
        assertTrue(Files.exists(tempDir.resolve("walk.vmd")));
    }

    @Test
    void indexesAnimationSummaries() throws Exception {
        Files.write(tempDir.resolve("idle.vmd"), new byte[] {1, 2, 3});

        MMDAnimationRuntime.AnimationEntry entry = MMDAnimationRuntime.index(
                tempDir,
                path -> new com.micheanl.model.client.nativebridge.MMDAnimationSummary(12, 3, 2, 1, 0, 0, 0)
        ).getFirst();

        assertEquals(12, entry.summary().maxFrame());
    }

    private static ZipEntryData entry(String name, byte[] bytes) {
        return new ZipEntryData(name, bytes);
    }

    private static byte[] zip(ZipEntryData... entries) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (ZipEntryData entry : entries) {
                zip.putNextEntry(new ZipEntry(entry.name));
                zip.write(entry.bytes);
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }

    private record ZipEntryData(String name, byte[] bytes) {
    }
}
