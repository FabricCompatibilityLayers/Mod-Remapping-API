package fr.catcore.modremapperapi.utils;

import java.util.*;

public class FakeModManager {
    private static final List<ModEntry> MODS = new ArrayList<>();

    private static boolean loaded = false;
    private static boolean loading = false;

    public static void init() {
        if (!loaded && !loading) {
            loading = true;
            ModDiscoverer.init();
            loading = false;
            loaded = true;
        }
    }

    protected static void addModEntry(ModEntry modEntry) {
        MODS.add(modEntry);
        Constants.MAIN_LOGGER.info("Added " + modEntry.getType() + " mod " + modEntry.modName + " to mod list.");
    }

    public static List<ModEntry> getMods() {
        return MODS;
    }
}
