package fr.catcore.modremapperapi.api.v1;

import java.util.List;

public interface ModRemapper {
    ModDiscoverer[] getModDiscoverers();

    void filterModEntries(List<ModEntry> entries);
}
