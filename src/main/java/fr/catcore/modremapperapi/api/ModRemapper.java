package fr.catcore.modremapperapi.api;

import fr.catcore.modremapperapi.remapping.RemapUtil;
import net.fabricmc.tinyremapper.TinyRemapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ModRemapper {

    String[] getJarFolders();

    RemapLibrary[] getRemapLibraries();

    Map<String, List<String>> getExclusions();

    void getMappingList(RemapUtil.MappingList list);

    Optional<TinyRemapper.ApplyVisitorProvider> getPostRemappingVisitor();
}
