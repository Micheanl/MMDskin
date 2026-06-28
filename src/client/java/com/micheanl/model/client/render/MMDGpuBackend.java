package com.micheanl.model.client.render;

import com.mojang.blaze3d.systems.DeviceInfo;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.vulkan.VulkanDevice;

public final class MMDGpuBackend {
    private final Kind kind;
    private final DrawMode drawMode;
    private final boolean persistentMapping;

    private MMDGpuBackend(Kind kind, DrawMode drawMode, boolean persistentMapping) {
        this.kind = kind;
        this.drawMode = drawMode;
        this.persistentMapping = persistentMapping;
    }

    public static MMDGpuBackend current() {
        GpuDevice device = RenderSystem.getDevice();
        GpuDeviceBackend backend = device.backend;
        return from(backend, device.getDeviceInfo());
    }

    static MMDGpuBackend from(DeviceInfo info) {
        return from(null, info);
    }

    private static MMDGpuBackend from(GpuDeviceBackend backend, DeviceInfo info) {
        Kind kind = kind(backend, info.backendName());
        DrawMode drawMode = info.features().drawIndirect() ? DrawMode.INDIRECT : DrawMode.DIRECT;
        return new MMDGpuBackend(kind, drawMode, info.features().persistentMapping());
    }

    public Kind kind() {
        return this.kind;
    }

    DrawMode drawMode() {
        return this.drawMode;
    }

    boolean persistentMapping() {
        return this.persistentMapping;
    }

    private static Kind kind(GpuDeviceBackend backend, String backendName) {
        if (backend instanceof VulkanDevice || "Vulkan".equalsIgnoreCase(backendName)) {
            return Kind.VULKAN;
        }
        if (backend instanceof GlDevice || backendName != null && backendName.toLowerCase(java.util.Locale.ROOT).contains("opengl")) {
            return Kind.OPENGL;
        }
        return Kind.UNKNOWN;
    }

    public enum Kind {
        OPENGL,
        VULKAN,
        UNKNOWN
    }

    enum DrawMode {
        DIRECT,
        INDIRECT
    }
}
