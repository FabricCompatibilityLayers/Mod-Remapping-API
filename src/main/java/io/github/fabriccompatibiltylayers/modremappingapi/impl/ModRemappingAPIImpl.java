package io.github.fabriccompatibiltylayers.modremappingapi.impl;

import fr.catcore.wfvaio.FabricVariants;
import fr.catcore.wfvaio.WhichFabricVariantAmIOn;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.context.ModRemapperContext;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.context.v1.ModRemapperV1Context;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;

public class ModRemappingAPIImpl {
    private static ModRemapperContext CURRENT_CONTEXT = null;
    public static final boolean BABRIC = WhichFabricVariantAmIOn.getVariant() == FabricVariants.BABRIC || WhichFabricVariantAmIOn.getVariant() == FabricVariants.BABRIC_NEW_FORMAT;

    public static boolean remapClassEdits = false;

    private static boolean init = false;
    private static boolean initializing = false;

    public static void init() {
        if (!init && !initializing) {
            initializing = true;

            FabricLoader.getInstance().getConfigDir().toFile().mkdirs();
            remapClassEdits = new File(FabricLoader.getInstance().getConfigDir().toFile(), ".remapclassedits").exists();

            CURRENT_CONTEXT = new ModRemapperV1Context();
            CURRENT_CONTEXT.gatherRemappers();

            CURRENT_CONTEXT.init();
            CURRENT_CONTEXT.discoverMods(remapClassEdits);

            CURRENT_CONTEXT.afterRemap();

            initializing = false;
            init = true;
        }
    }

    public static ModRemapperContext getCurrentContext() {
        return CURRENT_CONTEXT;
    }
}
