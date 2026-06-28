package com.micheanl.model.client.nativebridge;

public enum NativeStatus {
    OK(0),
    INVALID_ARGUMENT(1),
    NOT_FOUND(2),
    INTERNAL_ERROR(3);

    private final int code;

    NativeStatus(int code) {
        this.code = code;
    }

    public int code() {
        return this.code;
    }

    public static NativeStatus fromCode(int code) {
        for (NativeStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown native status: " + code);
    }
}
