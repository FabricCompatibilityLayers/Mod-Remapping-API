package fr.catcore.modremapperapi.utils;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.spongepowered.include.com.google.common.collect.Maps;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class FakeModManager {

    private static Method createBuiltin;

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

    /**
     * Get mod map from Fabric loader with reflections.
     * If fails it will return empty map.
     *
     * @param loader {@link FabricLoader} loader instance.
     * @return {@link Map} of {@link String} key and {@link ModContainer} value.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, ModContainer> getModMap(FabricLoader loader) {
        try {
            Field field = loader.getClass().getDeclaredField("modMap");
            field.setAccessible(true);
            return (Map<String, ModContainer>) field.get(loader);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    /**
     * Get mod list from Fabric loader with reflections.
     * If fails it will return empty list.
     *
     * @param loader {@link FabricLoader} loader instance.
     * @return {@link List} of {@link ModContainer}
     */
    @SuppressWarnings("unchecked")
    private static List<ModContainer> getModList(FabricLoader loader) {
        try {
            Field field = loader.getClass().getDeclaredField("mods");
            field.setAccessible(true);
            return (List<ModContainer>) field.get(loader);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
            return Collections.EMPTY_LIST;
        }
    }
}
