package fr.catcore.modremapperapi.impl;

import fr.catcore.modremapperapi.api.v1.ModDiscoverer;
import fr.catcore.modremapperapi.api.v1.ModCandidate;
import fr.catcore.modremapperapi.api.v1.ModRemapper;

import java.util.List;

public class BuiltInRemapper implements ModRemapper {
    @Override
    public ModDiscoverer[] getModDiscoverers() {
        return new ModDiscoverer[]{new BuiltInDiscoverer()};
    }

    @Override
    public void filterModEntries(List<ModCandidate> entries) {

    }
}
