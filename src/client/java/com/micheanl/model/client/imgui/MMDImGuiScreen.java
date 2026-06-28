package com.micheanl.model.client.imgui;

import com.micheanl.model.client.mmd.MMDModelRuntime;
import com.micheanl.model.client.render.MMDGpuBackend;
import com.micheanl.model.client.render.MMDPlayerRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.io.IOException;

public final class MMDImGuiScreen extends Screen {
    private final MMDImGuiController controller;
    private final MMDImGuiRenderer renderer = new MMDImGuiRenderer();
    private long handle;

    public MMDImGuiScreen() {
        super(Component.literal("MMD Skin"));
        this.controller = new MMDImGuiController(MMDPlayerRenderState::isEnabled, MMDPlayerRenderState::setEnabled, this::reloadModel);
    }

    @Override
    protected void init() {
        super.init();
        if (this.handle == 0) {
            this.handle = MMDImGuiNative.create(this.width, this.height, (float) Minecraft.getInstance().getWindow().getGuiScale());
        } else {
            MMDImGuiNative.resize(this.handle, this.width, this.height, (float) Minecraft.getInstance().getWindow().getGuiScale());
        }
        this.renderer.ensureFontTexture(this.handle);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        if (this.handle != 0) {
            MMDImGuiNative.resize(this.handle, width, height, (float) Minecraft.getInstance().getWindow().getGuiScale());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress) {
        super.extractRenderState(graphics, mouseX, mouseY, tickProgress);
        if (this.handle == 0) {
            return;
        }
        MMDModelRuntime runtime = MMDModelRuntime.instance();
        var renderData = runtime.renderData();
        int vertices = renderData.map(data -> data.mesh().vertexCount()).orElse(0);
        int indices = renderData.map(data -> data.mesh().indexCount()).orElse(0);
        int materials = renderData.map(data -> data.mesh().materialCount()).orElse(0);
        int actions = MMDImGuiNative.frame(
                this.handle,
                MMDPlayerRenderState.isEnabled(),
                vertices,
                indices,
                materials,
                runtime.animations().size(),
                MMDGpuBackend.current().kind().name()
        );
        this.controller.applyActions(actions);
        if (this.controller.consumeCloseRequested()) {
            this.onClose();
            return;
        }
        this.renderer.extract(graphics, MMDImGuiNative.drawData(this.handle));
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (this.handle != 0) {
            MMDImGuiNative.mouseMove(this.handle, (float) mouseX, (float) mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.handle != 0) {
            MMDImGuiNative.mouseMove(this.handle, (float) event.x(), (float) event.y());
            MMDImGuiNative.mouseButton(this.handle, event.button(), true);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.handle != 0) {
            MMDImGuiNative.mouseMove(this.handle, (float) event.x(), (float) event.y());
            MMDImGuiNative.mouseButton(this.handle, event.button(), false);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.handle != 0) {
            MMDImGuiNative.mouseMove(this.handle, (float) event.x(), (float) event.y());
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.handle != 0) {
            MMDImGuiNative.mouseMove(this.handle, (float) mouseX, (float) mouseY);
            MMDImGuiNative.mouseWheel(this.handle, (float) horizontalAmount, (float) verticalAmount);
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (super.keyPressed(event)) {
            return true;
        }
        if (this.handle != 0) {
            MMDImGuiNative.key(this.handle, event.key(), event.scancode(), event.modifiers(), true);
        }
        return true;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (this.handle != 0) {
            MMDImGuiNative.key(this.handle, event.key(), event.scancode(), event.modifiers(), false);
        }
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.handle != 0 && event.isAllowedChatCharacter()) {
            MMDImGuiNative.character(this.handle, event.codepoint());
        }
        return true;
    }

    @Override
    public void removed() {
        this.renderer.close();
        if (this.handle != 0) {
            MMDImGuiNative.destroy(this.handle);
            this.handle = 0;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    private void reloadModel() {
        try {
            MMDModelRuntime.instance().reload(MMDModelRuntime.defaultModelRoot());
        } catch (IOException e) {
            throw new IllegalStateException("MMD model reload failed", e);
        }
    }
}
