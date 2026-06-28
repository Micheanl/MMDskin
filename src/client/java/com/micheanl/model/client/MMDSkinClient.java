package com.micheanl.model.client;

import com.micheanl.model.client.nativebridge.MMDNativeLibrary;
import net.fabricmc.api.ClientModInitializer;

public class MMDSkinClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MMDNativeLibrary.load();
    }
}
