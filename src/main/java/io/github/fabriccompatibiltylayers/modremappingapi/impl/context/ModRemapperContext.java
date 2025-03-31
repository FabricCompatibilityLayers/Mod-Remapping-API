package io.github.fabriccompatibiltylayers.modremappingapi.impl.context;

import io.github.fabriccompatibiltylayers.modremappingapi.impl.LibraryHandler;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.ModCandidate;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings.MappingsRegistry;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.RemappingFlags;
import net.fabricmc.tinyremapper.TinyRemapper;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ModRemapperContext<T> {
    void init();
    void remapMods(Map<ModCandidate, Path> pathMap);
    void afterRemap();
    List<T> discoverMods(boolean remapClassEdits);
    void gatherRemappers();
    Map<String, List<String>> getMixin2TargetMap();
    MappingsRegistry getMappingsRegistry();
    void addToRemapperBuilder(TinyRemapper.Builder builder);
    Set<RemappingFlags> getRemappingFlags();
    LibraryHandler getLibraryHandler();
}
