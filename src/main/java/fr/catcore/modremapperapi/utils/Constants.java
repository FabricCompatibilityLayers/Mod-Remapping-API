package fr.catcore.modremapperapi.utils;

import net.fabricmc.loader.api.FabricLoader;
import net.legacyfabric.fabric.api.logger.v1.Logger;

import java.io.File;

public class Constants {
    public static final File MAIN_FOLDER = new File(FabricLoader.getInstance().getGameDir().toFile(), "mod-remapping-api");
    public static final File VERSIONED_FOLDER = new File(
            new File(MAIN_FOLDER,
                FabricLoader.getInstance().getModContainer("minecraft").get().getMetadata().getVersion().getFriendlyString()
            ), FabricLoader.getInstance().getModContainer("mod-remapping-api").get().getMetadata().getVersion().getFriendlyString()
    );
    public static final File EXTRA_MAPPINGS_FILE = new File(VERSIONED_FOLDER, "extra_mappings.tiny");
    public static final File REMAPPED_MAPPINGS_FILE = new File(VERSIONED_FOLDER, "remapped_mappings.tiny");

    public static final File LIB_FOLDER = new File(VERSIONED_FOLDER, "libs");
    public static final Logger MAIN_LOGGER = Logger.get("ModRemappingAPI");

    static {
        MAIN_FOLDER.mkdirs();
        VERSIONED_FOLDER.mkdirs();
        LIB_FOLDER.mkdirs();
    }
}
