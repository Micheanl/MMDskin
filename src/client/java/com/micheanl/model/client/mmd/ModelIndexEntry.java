package com.micheanl.model.client.mmd;

import java.nio.file.Path;

public record ModelIndexEntry(Path path, ModelHash hash, long size) {
}
