package com.micheanl.model.client.nativebridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class MMDNativeLibraryImGuiTest {
    @Test
    void resolvesLinuxImGuiLibraryPath() {
        assertEquals("natives/linux-x86_64/libmmdskin_imgui.so", MMDNativeLibrary.platformPath("Linux", "amd64", "mmdskin_imgui"));
    }

    @Test
    void resolvesWindowsImGuiLibraryPath() {
        assertEquals("natives/windows-aarch64/mmdskin_imgui.dll", MMDNativeLibrary.platformPath("Windows 11", "aarch64", "mmdskin_imgui"));
    }
}
