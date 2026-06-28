package com.micheanl.model.client.mmd;

import com.micheanl.model.client.nativebridge.MMDAnimationSummary;
import com.micheanl.model.client.nativebridge.MMDNative;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class MMDAnimationRuntime {
    public static final String DEFAULT_ANIMATION_RESOURCE = "/assets/mmdskin/animations/default-animation.zip";

    @FunctionalInterface
    public interface SummaryReader {
        MMDAnimationSummary read(Path path);
    }

    public record AnimationEntry(String name, Path path, MMDAnimationSummary summary, MMDPlayerAction action) {
        public AnimationEntry {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(summary, "summary");
            Objects.requireNonNull(action, "action");
        }
    }

    private MMDAnimationRuntime() {
    }

    public static Path defaultAnimationRoot() {
        return FabricLoader.getInstance().getConfigDir().resolve("mmdskin").resolve("animations").resolve("default");
    }

    public static List<AnimationEntry> installBundledDefaults(ClassLoader loader, Path target) throws IOException {
        return installBundledDefaults(loader, target, MMDNative::animationSummary);
    }

    public static List<AnimationEntry> installBundledDefaults(ClassLoader loader, Path target, SummaryReader summaryReader) throws IOException {
        try (InputStream input = loader.getResourceAsStream(DEFAULT_ANIMATION_RESOURCE.substring(1))) {
            if (input == null) {
                return List.of();
            }
            return extractDefaultAnimations(input, target, summaryReader);
        }
    }

    public static List<AnimationEntry> extractDefaultAnimations(InputStream input, Path target) throws IOException {
        return extractDefaultAnimations(input, target, MMDNative::animationSummary);
    }

    public static List<AnimationEntry> extractDefaultAnimations(InputStream input, Path target, SummaryReader summaryReader) throws IOException {
        Files.createDirectories(target);
        try (ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry = zip.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory() && isVmd(entry.getName())) {
                    Path file = target.resolve(fileName(entry.getName())).normalize();
                    if (file.getParent().equals(target)) {
                        try (OutputStream output = Files.newOutputStream(file)) {
                            zip.transferTo(output);
                        }
                    }
                }
                zip.closeEntry();
                entry = zip.getNextEntry();
            }
        }
        return index(target, summaryReader);
    }

    public static List<AnimationEntry> index(Path target) throws IOException {
        return index(target, MMDNative::animationSummary);
    }

    public static List<AnimationEntry> index(Path target, SummaryReader summaryReader) throws IOException {
        Objects.requireNonNull(summaryReader, "summaryReader");
        if (!Files.isDirectory(target)) {
            return List.of();
        }
        try (var stream = Files.list(target)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> isVmd(path.getFileName().toString()))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .map(path -> entry(path, summaryReader))
                    .toList();
        }
    }

    private static AnimationEntry entry(Path path, SummaryReader summaryReader) {
        String name = animationName(path.getFileName().toString());
        return new AnimationEntry(name, path, summaryReader.read(path), MMDPlayerAction.fromAnimationName(name));
    }

    private static boolean isVmd(String name) {
        return name.toLowerCase().endsWith(".vmd");
    }

    private static String fileName(String name) {
        int slash = name.lastIndexOf('/');
        return slash < 0 ? name : name.substring(slash + 1);
    }

    private static String animationName(String fileName) {
        return fileName.substring(0, fileName.length() - 4);
    }
}
