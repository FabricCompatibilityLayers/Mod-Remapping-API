package io.github.fabriccompatibiltylayers.modremappingapi.impl.context.v2;

import io.github.fabriccompatibilitylayers.modremappingapi.impl.InternalCacheHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class V2CacheHandler implements InternalCacheHandler {
    private final Path contextDirectory;
    private final Path tempDirectory, cacheDirectory, mappingsDirectory, librariesDirectory;

    public V2CacheHandler(Path contextDirectory) {
        this.contextDirectory = contextDirectory;
        tempDirectory = this.contextDirectory.resolve("temp");
        cacheDirectory = this.contextDirectory.resolve("cache");
        mappingsDirectory = this.contextDirectory.resolve("mappings");
        librariesDirectory = this.contextDirectory.resolve("libraries");

        for (Path p : new Path[]{tempDirectory, cacheDirectory, mappingsDirectory, librariesDirectory}) {
            if (!Files.exists(p)) {
                try {
                    Files.createDirectories(p);
                } catch (IOException e) {
                    throw new RuntimeException(e);
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
