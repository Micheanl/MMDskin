package com.micheanl.model.client.mmd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ModelIndexerTest {
    @TempDir
    Path tempDir;

    @Test
    void indexesSupportedModelFilesOnly() throws Exception {
        Files.write(tempDir.resolve("a.pmx"), new byte[] {1, 2, 3});
        Files.write(tempDir.resolve("b.pmd"), new byte[] {4, 5});
        Files.write(tempDir.resolve("ignore.txt"), new byte[] {9});

        List<ModelIndexEntry> entries = ModelIndexer.index(tempDir);

        assertEquals(2, entries.size());
        assertEquals("a.pmx", entries.get(0).path().getFileName().toString());
        assertEquals("b.pmd", entries.get(1).path().getFileName().toString());
    }

    @Test
    void hashesContentWithSha256() throws Exception {
        Files.write(tempDir.resolve("a.pmx"), new byte[] {1, 2, 3});

        ModelHash hash = ModelIndexer.index(tempDir).getFirst().hash();

        assertEquals("039058c6f2c0cb492c533b0a4d14ef77cc0f78abccced5287d84a1a2011cfb81", hash.value());
    }

    @Test
    void extractsSupportedModelFromZip() throws Exception {
        Files.write(tempDir.resolve("model.zip"), zip("nested/a.pmx", new byte[] {1, 2, 3}));

        List<ModelIndexEntry> entries = ModelIndexer.index(tempDir);

        assertEquals(1, entries.size());
        assertEquals("a.pmx", entries.getFirst().path().getFileName().toString());
        assertTrue(entries.getFirst().path().startsWith(tempDir.resolve(".cache")));
    }

    private static byte[] zip(String name, byte[] bytes) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry(name));
            zip.write(bytes);
            zip.closeEntry();
        }
        return output.toByteArray();
    }
}
