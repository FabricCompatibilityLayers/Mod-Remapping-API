package fr.catcore.modremapperapi.utils;

import fr.catcore.modremappingapi.impl.LoaderUtils;
import net.legacyfabric.fabric.api.logger.v1.Logger;

import java.io.File;

public class Constants {
    @Deprecated
    public static final File MAIN_FOLDER = LoaderUtils.getMainFolder().toFile();
    @Deprecated
    public static final File VERSIONED_FOLDER = fr.catcore.modremappingapi.api.v1.Constants.CACHE_FOLDER.toFile();
    public static final File EXTRA_MAPPINGS_FILE = new File(VERSIONED_FOLDER, "extra_mappings.tiny");
    public static final File REMAPPED_MAPPINGS_FILE = new File(VERSIONED_FOLDER, "remapped_mappings.tiny");

    @Deprecated
    public static final File LIB_FOLDER = fr.catcore.modremappingapi.api.v1.Constants.LIB_FOLDER.toFile();
    @Deprecated
    public static final Logger MAIN_LOGGER = fr.catcore.modremappingapi.api.v1.Constants.MAIN_LOGGER;
}
