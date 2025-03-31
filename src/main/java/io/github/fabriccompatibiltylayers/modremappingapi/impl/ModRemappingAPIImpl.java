package io.github.fabriccompatibiltylayers.modremappingapi.impl;

import fr.catcore.wfvaio.FabricVariants;
import fr.catcore.wfvaio.WhichFabricVariantAmIOn;
import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.ModRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.context.ModRemapperContext;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.context.v1.ModRemapperV1Context;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.context.v2.ModRemmaperV2Context;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ModRemappingAPIImpl {
    private static ModRemapperContext CURRENT_CONTEXT = null;
    public static final boolean BABRIC = WhichFabricVariantAmIOn.getVariant() == FabricVariants.BABRIC || WhichFabricVariantAmIOn.getVariant() == FabricVariants.BABRIC_NEW_FORMAT;

    public static boolean remapClassEdits = false;

    private static boolean init = false;
    private static boolean initializing = false;
    private static final String v2EntrypointName = "mod-remapper-api:modremapper_v2";

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

            Map<String, List<ModRemapper>> v2Remappers = FabricLoader.getInstance()
                    .getEntrypoints(v2EntrypointName, ModRemapper.class)
                    .stream().collect(Collectors.groupingBy(ModRemapper::getContextId));

            List<String> v2Keys = new ArrayList<>(v2Remappers.keySet());

            for (String contextKey : v2Keys) {
                ModRemmaperV2Context context = new ModRemmaperV2Context(contextKey, v2Remappers.get(contextKey));
                CURRENT_CONTEXT = context;

                Map<String, List<ModRemapper>> newRemappers = context.discoverMods(remapClassEdits)
                        .stream().collect(Collectors.groupingBy(ModRemapper::getContextId));

                v2Keys.addAll(newRemappers.keySet());
                v2Remappers.putAll(newRemappers);

                context.afterRemap();
            }

            initializing = false;
            init = true;
        }
    }

    public static ModRemapperContext getCurrentContext() {
        return CURRENT_CONTEXT;
    }
}
