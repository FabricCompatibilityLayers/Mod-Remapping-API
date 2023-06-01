package fr.catcore.modremapperapi.utils;

import fr.catcore.modremapperapi.api.ModRemapper;
import fr.catcore.modremapperapi.api.RemapLibrary;
import fr.catcore.modremapperapi.remapping.RemapUtil;
import fr.catcore.modremapperapi.remapping.VisitorInfos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public void registerVisitors(VisitorInfos infos) {

    }
}
