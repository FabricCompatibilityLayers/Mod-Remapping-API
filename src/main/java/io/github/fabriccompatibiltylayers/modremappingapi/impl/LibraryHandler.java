package io.github.fabriccompatibiltylayers.modremappingapi.impl;

import fr.catcore.modremapperapi.utils.Constants;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ModRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.RemapLibrary;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.CacheUtils;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.FileUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.tinyremapper.TinyRemapper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LibraryHandler {
    private static final Map<RemapLibrary, Path> remapLibraries = new HashMap<>();

    public static void gatherRemapLibraries(List<ModRemapper> remappers, String sourceNamespace) {
        try {
            for (ModRemapper remapper : remappers) {
                List<RemapLibrary> libraries = new ArrayList<>();

                remapper.addRemapLibraries(libraries, FabricLoader.getInstance().getEnvironmentType());

                Map<RemapLibrary, Path> temp = CacheUtils.computeExtraLibraryPaths(libraries, sourceNamespace);

                for (Map.Entry<RemapLibrary, Path> entry : temp.entrySet()) {
                    RemapLibrary library = entry.getKey();
                    Path path = entry.getValue();

                    if (Files.exists(path)) continue;

                    if (!library.url.isEmpty()) {
                        Constants.MAIN_LOGGER.info("Downloading remapping library '" + library.fileName + "' from url '" + library.url + "'");
                        FileUtils.downloadFile(library.url, path);
                        FileUtils.removeEntriesFromZip(path, library.toExclude);
                        Constants.MAIN_LOGGER.info("Remapping library ready for use.");
                    } else if (library.path != null) {
                        Constants.MAIN_LOGGER.info("Extracting remapping library '" + library.fileName + "' from mod jar.");
                        FileUtils.copyZipFile(library.path, path);
                        Constants.MAIN_LOGGER.info("Remapping library ready for use.");
                    }
                }

                remapLibraries.putAll(temp);
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static void addLibrariesToRemapClasspath(TinyRemapper remapper) {
        for (Path libPath : remapLibraries.values()) {
            if (Files.exists(libPath)) {
                remapper.readClassPathAsync(libPath);
            } else {
                Constants.MAIN_LOGGER.info("Library " + libPath + " does not exist.");
            }
        }
    }
}
