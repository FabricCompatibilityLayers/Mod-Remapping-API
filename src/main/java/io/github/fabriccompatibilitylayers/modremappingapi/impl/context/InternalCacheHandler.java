package io.github.fabriccompatibilitylayers.modremappingapi.impl.context;

import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.CacheHandler;
import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Path;

@ApiStatus.Internal
public interface InternalCacheHandler extends CacheHandler {
    Path resolveMappings(String name);
    Path resolveRoot(String name);
    Path getTempDirectory();
}
