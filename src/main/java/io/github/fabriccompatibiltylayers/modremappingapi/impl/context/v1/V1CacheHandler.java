package io.github.fabriccompatibiltylayers.modremappingapi.impl.context.v1;

import io.github.fabriccompatibilitylayers.modremappingapi.impl.InternalCacheHandler;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.CacheUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class V1CacheHandler implements InternalCacheHandler {
    private final Path contextDirectory = CacheUtils.MAIN_FOLDER;
    private final Path tempDirectory = contextDirectory.resolve("temp");
    private final Path libsDirectory = CacheUtils.LIBRARY_FOLDER;

    public V1CacheHandler() {
        for (Path p : new Path[] {tempDirectory, libsDirectory}) {
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
