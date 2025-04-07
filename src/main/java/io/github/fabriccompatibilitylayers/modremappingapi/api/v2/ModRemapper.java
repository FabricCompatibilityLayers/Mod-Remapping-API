package io.github.fabriccompatibilitylayers.modremappingapi.api.v2;

import java.util.List;

public interface ModRemapper {
    String getContextId();

    void init(CacheHandler cacheHandler);

    List<ModDiscovererConfig> getModDiscoverers();
    List<ModRemapper> collectSubRemappers(List<ModCandidate> discoveredMods);
    MappingsConfig getMappingsConfig();
    List<RemappingFlags> getRemappingFlags();
    void afterRemapping();
}
