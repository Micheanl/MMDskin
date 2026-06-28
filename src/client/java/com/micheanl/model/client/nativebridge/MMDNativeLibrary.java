package com.micheanl.model.client.nativebridge;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

public final class MMDNativeLibrary {
    private static final State STATE = new State();

    private MMDNativeLibrary() {
    }

    public static void load() {
        load(
                STATE,
                platformPath(System.getProperty("os.name"), System.getProperty("os.arch")),
                MMDNativeLibrary.class.getClassLoader(),
                cacheRoot(),
                System::load
        );
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

    static Path extract(String resourcePath, ClassLoader resources, Path cacheRoot) {
        Objects.requireNonNull(resourcePath, "resourcePath");
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(cacheRoot, "cacheRoot");
        byte[] bytes;
        try (InputStream stream = resources.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalStateException("Missing native library resource: " + resourcePath);
            }
            bytes = stream.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Native library extraction failed", exception);
        }
        String hash = sha256(bytes);
        String fileName = Path.of(resourcePath).getFileName().toString();
        Path directory = cacheRoot.resolve(hash);
        Path target = directory.resolve(fileName);
        try {
            Files.createDirectories(directory);
            if (!Files.exists(target) || Files.size(target) != bytes.length) {
                Files.write(target, bytes);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Native library extraction failed", exception);
        }
        return target;
    }

    static void load(State state, String resourcePath, ClassLoader resources, Path cacheRoot, Consumer<String> loader) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(loader, "loader");
        synchronized (state) {
            if (state.loaded) {
                return;
            }
            Path extracted = extract(resourcePath, resources, cacheRoot);
            loader.accept(extracted.toAbsolutePath().toString());
            state.loaded = true;
        }
    }

    static final class State {
        private boolean loaded;
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

    private static Path cacheRoot() {
        return Path.of(System.getProperty("java.io.tmpdir"), "mmdskin", "natives");
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
