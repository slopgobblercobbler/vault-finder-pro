package com.slopgobbler.vaultfinder;

import com.slopgobbler.vaultfinder.config.VaultFinderConfig;
import com.slopgobbler.vaultfinder.tracker.StorageCache;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class VaultFinderMod implements ClientModInitializer {
    public static final String MOD_ID = "vaultfinder";
    private static final StorageCache CACHE = new StorageCache();

    public static StorageCache getCache() {
        return CACHE;
    }

    public static void initialize() {
        System.out.println("[VaultFinder Pro] Initializing client-side container radar & storage indexer...");
        VaultFinderConfig.get().load();
    }

    @Override
    public void onInitializeClient() {
        initialize();
    }
}
