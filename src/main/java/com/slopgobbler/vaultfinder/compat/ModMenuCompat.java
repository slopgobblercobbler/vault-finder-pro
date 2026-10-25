package com.slopgobbler.vaultfinder.compat;

import com.slopgobbler.vaultfinder.gui.VaultFinderConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * ModMenu integration entrypoint for VaultFinder Pro.
 * Exposes the in-game configuration screen factory.
 */
@Environment(EnvType.CLIENT)
public class ModMenuCompat implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return VaultFinderConfigScreen::new;
    }
}
