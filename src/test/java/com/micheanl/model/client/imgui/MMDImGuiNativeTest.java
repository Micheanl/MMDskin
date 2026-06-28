package com.micheanl.model.client.imgui;

import com.micheanl.model.client.nativebridge.MMDNativeLibrary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class MMDImGuiNativeTest {
    @Test
    void createsFrameAndReturnsDrawData() {
        MMDNativeLibrary.loadLibrary("mmdskin_imgui");

        long handle = MMDImGuiNative.create(320, 240, 1.0F);
        try {
            assertTrue(handle > 0);
            assertEquals(1, MMDImGuiNative.nativeVersion());
            MMDImGuiNative.frame(handle, true, 128, 384, 2, 10, "OPENGL");
            MMDImGuiDrawData data = MMDImGuiNative.drawData(handle);

            assertNotNull(data);
            assertTrue(data.fontWidth() > 0);
            assertTrue(data.fontHeight() > 0);
            assertTrue(data.vertices().length > 0);
            assertTrue(data.indices().length > 0);
            assertTrue(data.commands().length > 0);
        } finally {
            if (handle > 0) {
                MMDImGuiNative.destroy(handle);
            }
        }
    }
}
