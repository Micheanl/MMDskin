package com.micheanl.model.client.imgui;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class MMDImGuiKeyMapping {
    private static KeyMapping open;

    private MMDImGuiKeyMapping() {
    }

    public static void register() {
        open = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.mmdskin.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                KeyMapping.Category.MISC
        ));
        ClientTickEvents.END_CLIENT_TICK.register(MMDImGuiKeyMapping::tick);
    }

    private static void tick(Minecraft client) {
        while (open.consumeClick()) {
            if (client.gui.screen() instanceof MMDImGuiScreen) {
                client.gui.setScreen(null);
            } else {
                client.gui.setScreen(new MMDImGuiScreen());
            }
        }
    }
}
