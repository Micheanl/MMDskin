package com.micheanl.model.client.render;

public final class MMDPlayerRenderState {
    private static volatile boolean enabled;

    private MMDPlayerRenderState() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }
}
