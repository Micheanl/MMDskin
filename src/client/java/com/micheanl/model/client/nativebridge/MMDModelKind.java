package com.micheanl.model.client.nativebridge;

public enum MMDModelKind {
    PMD(10),
    PMX(11);

    private final int code;

    MMDModelKind(int code) {
        this.code = code;
    }

    public int code() {
        return this.code;
    }

    public static MMDModelKind fromCode(int code) {
        for (MMDModelKind kind : values()) {
            if (kind.code == code) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown native model kind: " + code);
    }
}
