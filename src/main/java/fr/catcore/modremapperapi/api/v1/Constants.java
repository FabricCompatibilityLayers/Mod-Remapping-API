package fr.catcore.modremapperapi.api.v1;

import fr.catcore.modremapperapi.impl.LoaderUtils;
import net.legacyfabric.fabric.api.logger.v1.Logger;

import java.nio.file.Path;

public class Constants {
    public static final Path CACHE_FOLDER = LoaderUtils.getVersionedFolder();
    public static final Path LIB_FOLDER = CACHE_FOLDER.resolve("libs");
    public static final Logger MAIN_LOGGER = Logger.get("ModRemappingAPI");

    static {
        CACHE_FOLDER.toFile().mkdirs();
        LIB_FOLDER.toFile().mkdirs();
    }
}
