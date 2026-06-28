package com.micheanl.model.client.mmd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

public final class ModelIndexer {
    private ModelIndexer() {
    }

    public static List<ModelIndexEntry> index(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
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
