package io.github.fabriccompatibiltylayers.modremappingapi.impl.context;

import io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings.MappingsRegistry;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface ModRemapperContext {
    void init();
    void remapMods(Map<Path, Path> pathMap);
    void afterRemap();
    void discoverMods(boolean remapClassEdits);
    void gatherRemappers();
    Map<String, List<String>> getMixin2TargetMap();
    MappingsRegistry getMappingsRegistry();
}
