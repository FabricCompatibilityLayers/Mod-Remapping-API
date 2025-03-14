package io.github.fabriccompatibiltylayers.modremappingapi.impl.utils;

import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.RemapLibrary;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@ApiStatus.Internal
public class CacheUtils {
    public static final Path BASE_FOLDER = FabricLoader.getInstance().getGameDir().resolve("mod-remapping-api");
    public static final Path MAIN_FOLDER = BASE_FOLDER
            .resolve(VersionHelper.MINECRAFT_VERSION)
            .resolve(VersionHelper.MOD_VERSION);
    public static final Path LIBRARY_FOLDER = MAIN_FOLDER.resolve("libs");

    public static Path getCachePath(String pathName) {
        return MAIN_FOLDER.resolve(pathName);
    }

    public static Path getLibraryPath(String pathName) {
        return LIBRARY_FOLDER.resolve(pathName);
    }

    @ApiStatus.Internal
    public static Map<Path, Path> computeLibraryPaths(Collection<Path> sourcePaths, String target) {
        return sourcePaths.stream().collect(Collectors.toMap(p -> p,
                p -> CacheUtils.getLibraryPath(target).resolve(p.toFile().getName())));
    }

    @ApiStatus.Internal
    public static Map<RemapLibrary, Path> computeExtraLibraryPaths(Collection<RemapLibrary> sourcePaths, String target) {
        return sourcePaths.stream()
                .collect(Collectors.toMap(p -> p,
                p -> CacheUtils.getLibraryPath(target).resolve(p.fileName)))
                .entrySet()
                .stream()
                .filter(entry -> !Files.exists(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    static {
        BASE_FOLDER.toFile().mkdirs();
        MAIN_FOLDER.toFile().mkdirs();
        LIBRARY_FOLDER.toFile().mkdirs();
    }
}
