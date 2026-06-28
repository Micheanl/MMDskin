package com.micheanl.model.client.imgui;

import com.micheanl.model.MMDSkin;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Objects;

public final class MMDImGuiRenderer implements AutoCloseable {
    private static final Identifier FONT_TEXTURE_ID = MMDSkin.id("imgui/font");
    private DynamicTexture fontTexture;

    public void ensureFontTexture(long handle) {
        if (this.fontTexture != null) {
            return;
        }
        int width = MMDImGuiNative.fontWidth(handle);
        int height = MMDImGuiNative.fontHeight(handle);
        byte[] pixels = MMDImGuiNative.fontPixelsRgba(handle);
        NativeImage image = new NativeImage(width, height, false);
        ByteBuffer target = image.getPixelBytes();
        for (int offset = 0; offset < pixels.length; offset += 4) {
            target.put(offset, pixels[offset]);
            target.put(offset + 1, pixels[offset + 1]);
            target.put(offset + 2, pixels[offset + 2]);
            target.put(offset + 3, pixels[offset + 3]);
        }
        this.fontTexture = new DynamicTexture(() -> "mmdskin_imgui_font", image);
        Minecraft.getInstance().getTextureManager().register(FONT_TEXTURE_ID, this.fontTexture);
    }

    public void extract(GuiGraphicsExtractor graphics, MMDImGuiDrawData data) {
        DynamicTexture texture = this.fontTexture;
        if (texture == null) {
            return;
        }
        MMDImGuiDrawData.Expanded expanded = data.expand();
        TextureSetup textureSetup = TextureSetup.singleTexture(texture.getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        for (MMDImGuiDrawData.ExpandedCommand command : expanded.commands()) {
            if (command.vertexCount() <= 0) {
                continue;
            }
            graphics.guiRenderState.addGuiElement(new ImGuiElement(
                    textureSetup,
                    new Matrix3x2f(),
                    expanded.vertices(),
                    command.vertexOffset(),
                    command.vertexCount(),
                    scissor(command, data.displayWidth(), data.displayHeight())
            ));
        }
    }

    @Override
    public void close() {
        if (this.fontTexture != null) {
            Minecraft.getInstance().getTextureManager().release(FONT_TEXTURE_ID);
            this.fontTexture = null;
        }
    }

    private static @Nullable ScreenRectangle scissor(MMDImGuiDrawData.ExpandedCommand command, int width, int height) {
        int x0 = Math.max(0, command.clipX0());
        int y0 = Math.max(0, command.clipY0());
        int x1 = Math.min(width, command.clipX1());
        int y1 = Math.min(height, command.clipY1());
        if (x1 <= x0 || y1 <= y0) {
            return null;
        }
        return new ScreenRectangle(x0, y0, x1 - x0, y1 - y0);
    }

    private record ImGuiElement(
            TextureSetup textureSetup,
            Matrix3x2f pose,
            MMDImGuiDrawData.Vertex[] vertices,
            int vertexOffset,
            int vertexCount,
            @Nullable ScreenRectangle scissorArea
    ) implements GuiElementRenderState {
        private ImGuiElement {
            Objects.requireNonNull(textureSetup, "textureSetup");
            Objects.requireNonNull(pose, "pose");
            Objects.requireNonNull(vertices, "vertices");
        }

        @Override
        public void buildVertices(VertexConsumer vertexConsumer) {
            int end = this.vertexOffset + this.vertexCount;
            for (int offset = this.vertexOffset; offset < end; offset++) {
                MMDImGuiDrawData.Vertex vertex = this.vertices[offset];
                vertexConsumer.addVertexWith2DPose(this.pose, vertex.x(), vertex.y()).setUv(vertex.u(), vertex.v()).setColor(vertex.color());
            }
        }

        @Override
        public com.mojang.blaze3d.pipeline.RenderPipeline pipeline() {
            return MMDImGuiRenderTypes.pipeline();
        }

        @Override
        public @Nullable ScreenRectangle bounds() {
            return this.scissorArea;
        }
    }
}
