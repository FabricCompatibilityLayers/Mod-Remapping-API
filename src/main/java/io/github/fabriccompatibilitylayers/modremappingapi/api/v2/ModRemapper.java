package io.github.fabriccompatibilitylayers.modremappingapi.api.v2;

import java.util.List;

public interface ModRemapper {
    String getContextId();

    List<ModDiscovererConfig> getModDiscoverers();
    List<ModRemapper> collectSubRemappers(List<ModCandidate> discoveredMods);
}
