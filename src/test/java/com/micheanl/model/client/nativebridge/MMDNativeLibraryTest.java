package com.micheanl.model.client.nativebridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class MMDNativeLibraryTest {
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
}
