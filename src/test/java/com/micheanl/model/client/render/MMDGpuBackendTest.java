package com.micheanl.model.client.render;

import com.mojang.blaze3d.systems.DeviceFeatures;
import com.mojang.blaze3d.systems.DeviceInfo;
import com.mojang.blaze3d.systems.DeviceLimits;
import com.mojang.blaze3d.systems.DeviceType;
import com.mojang.blaze3d.systems.HintsAndWorkarounds;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class MMDGpuBackendTest {
    @Test
    void prefersIndirectDrawWhenBackendSupportsIt() {
        MMDGpuBackend backend = MMDGpuBackend.from(deviceInfo("Vulkan", true, true, true));

        assertEquals(MMDGpuBackend.Kind.VULKAN, backend.kind());
        assertEquals(MMDGpuBackend.DrawMode.INDIRECT, backend.drawMode());
    }

    @Test
    void fallsBackToDirectDrawWithoutIndirectSupport() {
        MMDGpuBackend backend = MMDGpuBackend.from(deviceInfo("OpenGL", false, false, true));

        assertEquals(MMDGpuBackend.Kind.OPENGL, backend.kind());
        assertEquals(MMDGpuBackend.DrawMode.DIRECT, backend.drawMode());
    }

    private static DeviceInfo deviceInfo(String backendName, boolean drawIndirect, boolean multiDrawIndirect, boolean persistentMapping) {
        return new DeviceInfo(
                "gpu",
                "vendor",
                "driver",
                true,
                backendName,
                1.0F,
                new DeviceLimits(16, 256, 8192, 1L << 30, 65535, 8),
                new DeviceFeatures(true, true, false, multiDrawIndirect, drawIndirect, true, persistentMapping),
                Set.of(),
                new HintsAndWorkarounds(false, false),
                DeviceType.DISCRETE
        );
    }
}
