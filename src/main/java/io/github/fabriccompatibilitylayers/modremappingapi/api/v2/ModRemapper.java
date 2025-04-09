package io.github.fabriccompatibilitylayers.modremappingapi.api.v2;

import net.fabricmc.api.EnvType;

import java.util.List;

public interface ModRemapper {
    String getContextId();

    void init(CacheHandler cacheHandler);

    List<ModDiscovererConfig> getModDiscoverers();
    List<ModRemapper> collectSubRemappers(List<ModCandidate> discoveredMods);
    MappingsConfig getMappingsConfig();
    List<RemappingFlags> getRemappingFlags();
    void afterRemapping();
    void afterAllRemappings();

    void addRemappingLibraries(List<RemapLibrary> libraries, EnvType environment);
    void registerAdditionalMappings(MappingBuilder mappingBuilder);
    void registerPreVisitors(VisitorInfos visitorInfos);
    void registerPostVisitors(VisitorInfos visitorInfos);
}
