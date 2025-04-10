package io.github.fabriccompatibilitylayers.modremappingapi.api.v2;

import io.github.fabriccompatibilitylayers.modremappingapi.impl.context.BaseModRemapperContext;

import java.nio.file.Path;

public interface CacheHandler {
    Path resolveTemp(String name);
    Path resolveCache(String name);
    Path resolveLibrary(String name);

    static CacheHandler getCacheHandler(String contextId) {
        return BaseModRemapperContext.get(contextId).getCacheHandler();
    }
}
