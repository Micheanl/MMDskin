package com.micheanl.model.client.nativebridge;

public record MMDModelSummary(long vertices, long indices, long materials, long bones) {
    public MMDModelSummary {
        if (vertices < 0 || indices < 0 || materials < 0 || bones < 0) {
            throw new IllegalArgumentException("Model summary counts must be non-negative");
        }
    }
}
