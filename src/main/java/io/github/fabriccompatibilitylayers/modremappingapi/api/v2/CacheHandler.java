package io.github.fabriccompatibilitylayers.modremappingapi.api.v2;

import java.nio.file.Path;

public interface CacheHandler {
    Path resolveTemp(String name);
    Path resolveCache(String name);
    Path resolveLibrary(String name);
}
