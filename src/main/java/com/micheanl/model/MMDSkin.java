package com.micheanl.model;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

public class MMDSkin implements ModInitializer {
    public static final String MOD_ID = "mmdskin";

    @Override
    public void onInitialize() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
