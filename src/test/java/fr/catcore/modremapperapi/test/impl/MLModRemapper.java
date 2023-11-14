package fr.catcore.modremapperapi.test.impl;

import fr.catcore.modremapperapi.api.v1.ModDiscoverer;
import fr.catcore.modremapperapi.api.v1.ModRemapper;
import fr.catcore.modremapperapi.api.v1.ModCandidate;

import java.util.ArrayList;
import java.util.List;

public class MLModRemapper implements ModRemapper {
    @Override
    public ModDiscoverer[] getModDiscoverers() {
        return new ModDiscoverer[]{
                new MLModDiscoverer(),
                new MLLibDiscoverer()
        };
    }

    @Override
    public void filterModEntries(List<ModCandidate> entries) {
        List<String> found = new ArrayList<>();

        entries.forEach(modEntry -> {
            if (modEntry.getInfos() instanceof MLModInfos) {
                found.add(modEntry.getFilePath().toString());
            }
        });

        entries.removeIf(modEntry -> !(modEntry.getInfos() instanceof MLModInfos)
                && found.contains(modEntry.getFilePath().toString()));
    }

    @Override
    public String getDefaultPackage() {
        return "net/minecraft/";
    }
}
