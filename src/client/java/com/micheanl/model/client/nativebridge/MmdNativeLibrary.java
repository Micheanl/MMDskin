package com.micheanl.model.client.nativebridge;

import java.util.Locale;

public final class MmdNativeLibrary {
    private MmdNativeLibrary() {
    }

    public static String platformPath(String osName, String osArch) {
        String os = normalizeOs(osName);
        String arch = normalizeArch(osArch);
        String file = switch (os) {
            case "windows" -> "mmdskin_native.dll";
            case "linux" -> "libmmdskin_native.so";
            case "macos" -> "libmmdskin_native.dylib";
            default -> throw new IllegalArgumentException("Unsupported OS: " + osName);
        };
        return "natives/" + os + "-" + arch + "/" + file;
    }

    private static String normalizeOs(String osName) {
        String value = osName.toLowerCase(Locale.ROOT);
        if (value.contains("win")) {
            return "windows";
        }
        if (value.contains("linux")) {
            return "linux";
        }
        if (value.contains("mac") || value.contains("darwin")) {
            return "macos";
        }
        throw new IllegalArgumentException("Unsupported OS: " + osName);
    }

    private static String normalizeArch(String osArch) {
        String value = osArch.toLowerCase(Locale.ROOT);
        return switch (value) {
            case "x86_64", "amd64" -> "x86_64";
            case "aarch64", "arm64" -> "aarch64";
            default -> throw new IllegalArgumentException("Unsupported architecture: " + osArch);
        };
    }
}
