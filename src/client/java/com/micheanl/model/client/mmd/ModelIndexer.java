package com.micheanl.model.client.mmd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ModelIndexer {
    private ModelIndexer() {
    }

    public static List<ModelIndexEntry> index(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        extractZipModels(root);
        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(ModelIndexer::isSupportedModel)
                    .sorted(Comparator.comparing(Path::toString))
                    .map(ModelIndexer::entry)
                    .toList();
        }
    }

    private static boolean isSupportedModel(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".pmx") || name.endsWith(".pmd");
    }

    private static void extractZipModels(Path root) throws IOException {
        List<Path> zips = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(root)) {
            stream
                    .filter(Files::isRegularFile)
                    .filter(ModelIndexer::isZip)
                    .forEach(zips::add);
        }
        Path cache = root.resolve(".cache").resolve("models");
        for (Path zip : zips) {
            extractZipModel(zip, cache.resolve(hash(zip).value()));
        }
    }

    private static boolean isZip(Path path) {
        return path.getFileName().toString().toLowerCase().endsWith(".zip");
    }

    private static void extractZipModel(Path zipPath, Path target) throws IOException {
        Files.createDirectories(target);
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry = zip.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory() && isSupportedModel(Path.of(entry.getName()))) {
                    Path file = target.resolve(fileName(entry.getName())).normalize();
                    if (file.getParent().equals(target)) {
                        try (OutputStream output = Files.newOutputStream(file)) {
                            zip.transferTo(output);
                        }
                        return;
                    }
                }
                zip.closeEntry();
                entry = zip.getNextEntry();
            }
        }
    }

    private static String fileName(String name) {
        int slash = name.lastIndexOf('/');
        return slash < 0 ? name : name.substring(slash + 1);
    }

    private static ModelIndexEntry entry(Path path) {
        try {
            return new ModelIndexEntry(path, hash(path), Files.size(path));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ModelHash hash(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = new DigestInputStream(Files.newInputStream(path), digest)) {
                input.transferTo(OutputStream.nullOutputStream());
            }
            return new ModelHash(HexFormat.of().formatHex(digest.digest()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
