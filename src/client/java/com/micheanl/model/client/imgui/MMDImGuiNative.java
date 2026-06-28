package com.micheanl.model.client.imgui;

public final class MMDImGuiNative {
    private MMDImGuiNative() {
    }

    public static native int nativeVersion();

    public static native long create(int width, int height, float scale);

    public static native void destroy(long handle);

    public static native void resize(long handle, int width, int height, float scale);

    public static native int frame(long handle, boolean enabled, int modelVertices, int modelIndices, int modelMaterials, int animationCount, String backendName);

    public static native void mouseMove(long handle, float x, float y);

    public static native void mouseButton(long handle, int button, boolean down);

    public static native void mouseWheel(long handle, float x, float y);

    public static native void key(long handle, int key, int scancode, int modifiers, boolean down);

    public static native void character(long handle, int codepoint);

    public static native int fontWidth(long handle);

    public static native int fontHeight(long handle);

    public static native byte[] fontPixelsRgba(long handle);

    public static native MMDImGuiDrawData drawData(long handle);
}
