package io.github.fabriccompatibilitylayers.modremappingapi.impl.compatibility.v1;

import io.github.fabriccompatibilitylayers.modremappingapi.impl.context.InternalCacheHandler;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.utils.CacheUtils;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@ApiStatus.Internal
public class V1CacheHandler implements InternalCacheHandler {
    private final Path contextDirectory = CacheUtils.MAIN_FOLDER;
    private final Path tempDirectory = contextDirectory.resolve("temp");
    private final Path libsDirectory = CacheUtils.LIBRARY_FOLDER;

    public V1CacheHandler() {
        var directoriesToCreate = List.of(tempDirectory, libsDirectory);

        for (var directory : directoriesToCreate) {
            if (!Files.exists(directory)) {
                try {
                    Files.createDirectories(directory);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create directory: " + directory, e);
                }
            }
        }
    }

    @Override
    public Path resolveMappings(String name) {
        return this.resolveRoot(name);
    }

    @Override
    public Path resolveRoot(String name) {
        return contextDirectory.resolve(name);
    }

    @Override
    public Path getTempDirectory() {
        return tempDirectory;
    }

    @Override
    public Path resolveTemp(String name) {
        return tempDirectory.resolve(name);
    }

    @Override
    public Path resolveCache(String name) {
        return this.resolveRoot(name);
    }

    @Override
    public Path resolveLibrary(String name) {
        return libsDirectory.resolve(name);
    }
}
