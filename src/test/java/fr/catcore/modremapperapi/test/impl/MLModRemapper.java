package fr.catcore.modremapperapi.test.impl;

import fr.catcore.modremapperapi.api.v1.ModDiscoverer;
import fr.catcore.modremapperapi.api.v1.ModRemapper;

public class MLModRemapper implements ModRemapper {
    @Override
    public ModDiscoverer[] getModDiscoverers() {
        return new ModDiscoverer[]{
                new MLModDiscoverer()
        };
    }
}
