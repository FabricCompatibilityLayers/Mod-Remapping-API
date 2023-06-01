package fr.catcore.modremapperapi;

import fr.catcore.modremapperapi.api.ModRemapper;
import fr.catcore.modremapperapi.utils.FakeModManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ModRemappingAPI {

    public static final List<ModRemapper> MOD_REMAPPERS = new ArrayList<>();
    private static final String entrypointName = "mod-remapper-api:modremapper";

    public static boolean remapModClasses = true;

    public static final boolean BABRIC = FabricLoader.getInstance().getModContainer("fabricloader")
            .get().getMetadata().getVersion().getFriendlyString().contains("babric");

    private static boolean init = false;
    private static boolean initializing = false;

    public static void init() {
        if (!init && !initializing) {
            initializing = true;

            FabricLoader.getInstance().getConfigDir().toFile().mkdirs();
            remapModClasses = new File(FabricLoader.getInstance().getConfigDir().toFile(), "modremapper").exists();

            MOD_REMAPPERS.addAll(FabricLoader.getInstance().getEntrypoints(entrypointName, ModRemapper.class));
            FakeModManager.init();

            for (ModRemapper remapper : MOD_REMAPPERS) remapper.afterRemap();

            initializing = false;
            init = true;
        }
    }
}
