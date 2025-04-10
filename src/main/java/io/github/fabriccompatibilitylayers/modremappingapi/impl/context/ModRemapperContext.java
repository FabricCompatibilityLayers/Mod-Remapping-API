package io.github.fabriccompatibilitylayers.modremappingapi.impl.context;

import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.CacheHandler;
import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.ModCandidate;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.mappings.MappingsRegistry;
import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.RemappingFlags;
import net.fabricmc.tinyremapper.TinyRemapper;
import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApiStatus.Internal
public interface ModRemapperContext<T> {
    void init();
    void remapMods(Map<ModCandidate, Path> pathMap);
    void afterRemap();
    List<T> discoverMods(boolean remapClassEdits);
    void gatherRemappers();
    MappingsRegistry getMappingsRegistry();
    void addToRemapperBuilder(TinyRemapper.Builder builder);
    Set<RemappingFlags> getRemappingFlags();
    LibraryHandler getLibraryHandler();
    String getId();
    MixinData getMixinData();
    void gatherLibraries();
    CacheHandler getCacheHandler();
}
