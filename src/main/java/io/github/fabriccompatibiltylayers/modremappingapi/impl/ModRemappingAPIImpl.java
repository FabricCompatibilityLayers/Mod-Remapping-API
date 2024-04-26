package io.github.fabriccompatibiltylayers.modremappingapi.impl;

import fr.catcore.wfvaio.FabricVariants;
import fr.catcore.wfvaio.WhichFabricVariantAmIOn;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ModRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.compatibility.V0ModRemapper;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ModRemappingAPIImpl {

    private static final String v0EntrypointName = "mod-remapper-api:modremapper";
    private static final String v1EntrypointName = "mod-remapper-api:modremapper_v1";
    private static final List<ModRemapper> modRemappers = new ArrayList<>();
    public static final boolean BABRIC = WhichFabricVariantAmIOn.getVariant() == FabricVariants.BABRIC || WhichFabricVariantAmIOn.getVariant() == FabricVariants.BABRIC_NEW_FORMAT;

    public static boolean remapClassEdits = false;

    private static boolean init = false;
    private static boolean initializing = false;

    public static void init() {
        if (!init && !initializing) {
            initializing = true;

            FabricLoader.getInstance().getConfigDir().toFile().mkdirs();
            remapClassEdits = new File(FabricLoader.getInstance().getConfigDir().toFile(), ".remapclassedits").exists();

            FabricLoader.getInstance()
                    .getEntrypoints(v0EntrypointName, fr.catcore.modremapperapi.api.ModRemapper.class)
                    .stream()
                    .map(V0ModRemapper::new)
                    .forEach(modRemappers::add);

            modRemappers.addAll(FabricLoader.getInstance().getEntrypoints(v1EntrypointName, ModRemapper.class));

            ModDiscoverer.init(modRemappers, remapClassEdits);

            modRemappers.forEach(ModRemapper::afterRemap);

            initializing = false;
            init = true;
        }
    }
}
