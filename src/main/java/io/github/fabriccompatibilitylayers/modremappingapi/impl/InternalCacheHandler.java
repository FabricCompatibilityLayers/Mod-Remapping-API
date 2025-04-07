package io.github.fabriccompatibilitylayers.modremappingapi.impl;

import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.CacheHandler;

import java.nio.file.Path;

public interface InternalCacheHandler extends CacheHandler {
    Path resolveMappings(String name);
    Path resolveRoot(String name);
    Path getTempDirectory();
}
