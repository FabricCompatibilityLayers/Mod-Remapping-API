package fr.catcore.modremapperapi.utils;

import fr.catcore.modremapperapi.api.ApplyVisitorProvider;
import fr.catcore.modremapperapi.api.ModRemapper;
import fr.catcore.modremapperapi.api.RemapLibrary;
import fr.catcore.modremapperapi.remapping.RemapUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DefaultModRemapper implements ModRemapper {
    @Override
    public String[] getJarFolders() {
        return new String[] {"mods"};
    }

    @Override
    public RemapLibrary[] getRemapLibraries() {
        return new RemapLibrary[0];
    }

    @Override
    public Map<String, List<String>> getExclusions() {
        return new HashMap<>();
    }

    @Override
    public void getMappingList(RemapUtil.MappingList list) {}

    @Override
    public Optional<ApplyVisitorProvider> getPostRemappingVisitor() {
        return Optional.empty();
    }
}
