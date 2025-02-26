package io.github.fabriccompatibiltylayers.modremappingapi.impl.utils;

import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Path;

@ApiStatus.Internal
public class CacheUtils {
    public static final Path BASE_FOLDER = FabricLoader.getInstance().getGameDir().resolve("mod-remapping-api");
    public static final Path MAIN_FOLDER = BASE_FOLDER
            .resolve(VersionHelper.MINECRAFT_VERSION)
            .resolve(VersionHelper.MOD_VERSION);
    public static final Path LIBRARY_FOLDER = BASE_FOLDER.resolve("libs");

    public static Path getCachePath(String pathName) {
        return MAIN_FOLDER.resolve(pathName);
    }

    public static Path getLibraryPath(String pathName) {
        return LIBRARY_FOLDER.resolve(pathName);
    }

    static {
        BASE_FOLDER.toFile().mkdirs();
        MAIN_FOLDER.toFile().mkdirs();
        LIBRARY_FOLDER.toFile().mkdirs();
    }
}
