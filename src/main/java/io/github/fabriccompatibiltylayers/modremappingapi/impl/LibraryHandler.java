package io.github.fabriccompatibiltylayers.modremappingapi.impl;

import fr.catcore.modremapperapi.utils.Constants;
import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.CacheHandler;
import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.RemapLibrary;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.FileUtils;
import net.fabricmc.tinyremapper.TinyRemapper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class LibraryHandler {
    private Map<RemapLibrary, Path> remapLibraries = new HashMap<>();

    private String sourceNamespace;
    private CacheHandler cacheHandler;

    public LibraryHandler() {}

    public void init(String sourceNamespace, CacheHandler cacheHandler) {
        this.sourceNamespace = sourceNamespace;
        this.cacheHandler = cacheHandler;

        Path sourceLibraryPath = this.cacheHandler.resolveLibrary(this.sourceNamespace);

        if (!Files.exists(sourceLibraryPath)) {
            try {
                Files.createDirectories(sourceLibraryPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Map<RemapLibrary, Path> computeExtraLibraryPaths(Collection<RemapLibrary> sourcePaths, String target) {
        return sourcePaths.stream()
                .collect(Collectors.toMap(p -> p,
                        p -> this.cacheHandler.resolveLibrary(target).resolve(p.getFileName())));
    }

    public void cacheLibraries(List<RemapLibrary> libraries) {
        remapLibraries = computeExtraLibraryPaths(libraries, sourceNamespace);

        try {
            for (Map.Entry<RemapLibrary, Path> entry : remapLibraries.entrySet()) {
                RemapLibrary library = entry.getKey();
                Path path = entry.getValue();

                if (Files.exists(path)) continue;

                if (library.getURL() != null && !library.getURL().isEmpty()) {
                    Constants.MAIN_LOGGER.info("Downloading remapping library '" + library.getFileName() + "' from url '" + library.getURL() + "'");
                    FileUtils.downloadFile(library.getURL(), path);
                    FileUtils.removeEntriesFromZip(path, library.getToExclude());
                    Constants.MAIN_LOGGER.info("Remapping library ready for use.");
                } else if (library.getPath() != null) {
                    Constants.MAIN_LOGGER.info("Extracting remapping library '" + library.getFileName() + "' from mod jar.");
                    FileUtils.copyZipFile(library.getPath(), path);
                    Constants.MAIN_LOGGER.info("Remapping library ready for use.");
                }
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void addLibrariesToRemapClasspath(TinyRemapper remapper) {
        for (Path libPath : remapLibraries.values()) {
            if (Files.exists(libPath)) {
                remapper.readClassPathAsync(libPath);
            } else {
                Constants.MAIN_LOGGER.info("Library " + libPath + " does not exist.");
            }
        }
    }
}
