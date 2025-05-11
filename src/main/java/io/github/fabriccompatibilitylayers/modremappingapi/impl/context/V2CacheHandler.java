package io.github.fabriccompatibilitylayers.modremappingapi.impl.context;

import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@ApiStatus.Internal
public class V2CacheHandler implements InternalCacheHandler {
    private final Path contextDirectory;
    private final Path tempDirectory, cacheDirectory, mappingsDirectory, librariesDirectory;

    public V2CacheHandler(Path contextDirectory) {
        this.contextDirectory = contextDirectory;
        tempDirectory = this.contextDirectory.resolve("temp");
        cacheDirectory = this.contextDirectory.resolve("cache");
        mappingsDirectory = this.contextDirectory.resolve("mappings");
        librariesDirectory = this.contextDirectory.resolve("libraries");

        var directories = List.of(tempDirectory, cacheDirectory, mappingsDirectory, librariesDirectory);
        
        for (var directory : directories) {
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
    public Path resolveTemp(String name) {
        return tempDirectory.resolve(name);
    }

    @Override
    public Path resolveCache(String name) {
        return cacheDirectory.resolve(name);
    }

    @Override
    public Path resolveLibrary(String name) {
        return librariesDirectory.resolve(name);
    }

    @Override
    public Path resolveMappings(String name) {
        return mappingsDirectory.resolve(name);
    }

    @Override
    public Path resolveRoot(String name) {
        return this.contextDirectory.resolve(name);
    }

    @Override
    public Path getTempDirectory() {
        return tempDirectory;
    }
}
