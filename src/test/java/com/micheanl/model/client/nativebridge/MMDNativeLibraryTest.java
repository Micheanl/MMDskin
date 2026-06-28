package com.micheanl.model.client.nativebridge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MMDNativeLibraryTest {
    @TempDir
    Path tempDir;

    @Test
    void mapsWindowsX64Path() {
        assertEquals("natives/windows-x86_64/mmdskin_native.dll", MMDNativeLibrary.platformPath("Windows 11", "amd64"));
    }

    @Test
    void mapsLinuxArm64Path() {
        assertEquals("natives/linux-aarch64/libmmdskin_native.so", MMDNativeLibrary.platformPath("Linux", "aarch64"));
    }

    @Test
    void mapsMacArm64Path() {
        assertEquals("natives/macos-aarch64/libmmdskin_native.dylib", MMDNativeLibrary.platformPath("Mac OS X", "aarch64"));
    }

    @Test
    void rejectsUnsupportedOs() {
        assertThrows(IllegalArgumentException.class, () -> MMDNativeLibrary.platformPath("Solaris", "amd64"));
    }

    @Test
    void extractsNativeLibraryToHashNamedCacheFile() throws Exception {
        ClassLoader resources = classLoader("natives/linux-x86_64/libmmdskin_native.so", new byte[]{1, 2, 3});

        Path extracted = MMDNativeLibrary.extract(
                "natives/linux-x86_64/libmmdskin_native.so",
                resources,
                tempDir
        );

        assertTrue(Files.exists(extracted));
        assertEquals("libmmdskin_native.so", extracted.getFileName().toString());
        assertEquals(64, extracted.getParent().getFileName().toString().length());
        assertEquals(3, Files.size(extracted));
    }

    @Test
    void rejectsMissingNativeResource() {
        ClassLoader resources = classLoader("other", new byte[]{1});

        assertThrows(IllegalStateException.class, () -> MMDNativeLibrary.extract(
                "natives/linux-x86_64/libmmdskin_native.so",
                resources,
                tempDir
        ));
    }

    @Test
    void loadsNativeLibraryOnlyOnce() {
        List<String> loaded = new ArrayList<>();
        MMDNativeLibrary.State state = new MMDNativeLibrary.State();
        ClassLoader resources = classLoader("natives/linux-x86_64/libmmdskin_native.so", new byte[]{1, 2, 3});

        MMDNativeLibrary.load(
                state,
                "natives/linux-x86_64/libmmdskin_native.so",
                resources,
                tempDir,
                loaded::add
        );
        MMDNativeLibrary.load(
                state,
                "natives/linux-x86_64/libmmdskin_native.so",
                resources,
                tempDir,
                loaded::add
        );

        assertEquals(1, loaded.size());
        assertTrue(loaded.getFirst().endsWith("libmmdskin_native.so"));
    }

    private static ClassLoader classLoader(String path, byte[] bytes) {
        return new ClassLoader(null) {
            @Override
            public InputStream getResourceAsStream(String name) {
                if (path.equals(name)) {
                    return new ByteArrayInputStream(bytes);
                }
                return null;
            }
        };
    }
}
