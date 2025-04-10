package io.github.fabriccompatibilitylayers.modremappingapi.impl;

import fr.catcore.wfvaio.FabricVariants;
import fr.catcore.wfvaio.WhichFabricVariantAmIOn;
import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.ModRemapper;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.context.ModRemapperContext;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.compatibility.v1.ModRemapperV1Context;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.context.ModRemmaperV2Context;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.ApiStatus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApiStatus.Internal
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

            while (!v2Keys.isEmpty()) {
                String contextKey = v2Keys.remove(0);
                ModRemmaperV2Context context = new ModRemmaperV2Context(contextKey, v2Remappers.get(contextKey));
                CURRENT_CONTEXT = context;

                CURRENT_CONTEXT.init();

                Map<String, List<ModRemapper>> newRemappers = context.discoverMods(remapClassEdits)
                        .stream().collect(Collectors.groupingBy(ModRemapper::getContextId));

                v2Keys.addAll(newRemappers.keySet());

                newRemappers.forEach((k, v) -> v2Remappers.computeIfAbsent(k, k2 -> new ArrayList<>()).addAll(v));

                context.afterRemap();
            }

            v2Remappers.values().forEach(l -> l.forEach(ModRemapper::afterAllRemappings));

            initializing = false;
            init = true;
        }
    }

    public static ModRemapperContext<?> getCurrentContext() {
        return CURRENT_CONTEXT;
    }
}
