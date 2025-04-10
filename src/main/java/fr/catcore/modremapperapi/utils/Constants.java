package fr.catcore.modremapperapi.utils;

import io.github.fabriccompatibilitylayers.modremappingapi.impl.utils.CacheUtils;
import net.legacyfabric.fabric.api.logger.v1.Logger;

import java.io.File;

public class Constants {
    @Deprecated
    public static final File MAIN_FOLDER = CacheUtils.BASE_FOLDER.toFile();
    @Deprecated
    public static final File VERSIONED_FOLDER = CacheUtils.MAIN_FOLDER.toFile();
    @Deprecated
    public static final File LIB_FOLDER = CacheUtils.LIBRARY_FOLDER.toFile();

    @Deprecated
    public static final File EXTRA_MAPPINGS_FILE = CacheUtils.getCachePath("extra_mappings.tiny").toFile();
    @Deprecated
    public static final File REMAPPED_MAPPINGS_FILE = CacheUtils.getCachePath("remapped_mappings.tiny").toFile();
    @Deprecated
    public static final File MC_MAPPINGS_FILE = CacheUtils.getCachePath("mc_mappings.tiny").toFile();
    @Deprecated
    public static final File FULL_MAPPINGS_FILE = CacheUtils.getCachePath("full_mappings.tiny").toFile();

    public static final Logger MAIN_LOGGER = Logger.get("ModRemappingAPI");
}
