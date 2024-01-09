package fr.catcore.modremappingapi.api.v1;

import java.util.List;

public interface ModRemapper {
    ModDiscoverer[] getModDiscoverers();

    void filterModEntries(List<ModCandidate> entries);

    String getDefaultPackage();
}
