package fr.catcore.modremapperapi;

import fr.catcore.modremapperapi.api.ModRemapper;
import fr.catcore.modremapperapi.utils.FakeModManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ModRemappingAPI {

    public static final List<ModRemapper> MOD_REMAPPERS = new ArrayList<>();
    private static final String entrypointName = "mod-remapper-api:modremapper";
    private static final String noMixins = "mod-remapper-api:nomixins";

    public static boolean remapModClasses = true;

    public static final boolean BABRIC = FabricLoader.getInstance().getModContainer("fabricloader")
            .get().getMetadata().getVersion().getFriendlyString().contains("babric");

    private static boolean init = false;

    public static void init(boolean fromMixin) {
        if (fromMixin && !canLoadMixin()) return;

        if (!init) {
            FabricLoader.getInstance().getConfigDir().toFile().mkdirs();
            remapModClasses = new File(FabricLoader.getInstance().getConfigDir().toFile(), "modremapper").exists();

            MOD_REMAPPERS.addAll(FabricLoader.getInstance().getEntrypoints(entrypointName, ModRemapper.class));
            FakeModManager.init();
        }
    }

    private static boolean canLoadMixin() {
        AtomicBoolean value = new AtomicBoolean(true);

        FabricLoader.getInstance().getAllMods().forEach(modContainer -> {
            if (value.get() && modContainer.getMetadata().containsCustomValue(noMixins)) {
                value.set(false);
            }
        });

        return value.get();
    }
}
