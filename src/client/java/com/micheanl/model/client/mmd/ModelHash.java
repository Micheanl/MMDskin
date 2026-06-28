package com.micheanl.model.client.mmd;

public record ModelHash(String value) {
    public ModelHash {
        if (value.length() != 64) {
            throw new IllegalArgumentException("SHA-256 hash must be 64 hex characters");
        }
    }
}
