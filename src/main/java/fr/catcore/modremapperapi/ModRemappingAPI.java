package fr.catcore.modremapperapi;

import fr.catcore.modremapperapi.api.ModRemapper;
import fr.catcore.modremapperapi.utils.FakeModManager;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ModRemappingAPI implements PreLaunchEntrypoint {

    public static final List<ModRemapper> MOD_REMAPPERS = new ArrayList<>();
    private static final String entrypointName = "mod-remapper-api:modremapper";

    public static boolean remapModClasses = true;

    @Override
    public void onPreLaunch() {
        FabricLoader.getInstance().getConfigDir().toFile().mkdirs();
        remapModClasses = new File(FabricLoader.getInstance().getConfigDir().toFile(), "modremapper").exists();

        MOD_REMAPPERS.addAll(FabricLoader.getInstance().getEntrypoints(entrypointName, ModRemapper.class));
        FakeModManager.init();
    }
}
