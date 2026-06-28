package com.micheanl.model.client;

import com.micheanl.model.client.mmd.MMDModelRuntime;
import com.micheanl.model.client.mmd.MMDAnimationRuntime;
import com.micheanl.model.client.imgui.MMDImGuiKeyMapping;
import com.micheanl.model.client.nativebridge.MMDNativeLibrary;
import com.micheanl.model.client.render.MMDRenderFeatures;
import net.fabricmc.api.ClientModInitializer;

import java.io.IOException;

public class MMDSkinClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MMDNativeLibrary.load();
        MMDNativeLibrary.loadLibrary("mmdskin_imgui");
        MMDRenderFeatures.register();
        MMDImGuiKeyMapping.register();
        try {
            MMDAnimationRuntime.installBundledDefaults(MMDSkinClient.class.getClassLoader(), MMDAnimationRuntime.defaultAnimationRoot());
            MMDModelRuntime.instance().reload(MMDModelRuntime.defaultModelRoot());
        } catch (IOException e) {
            throw new IllegalStateException("MMD model load failed", e);
        }
    }
}
