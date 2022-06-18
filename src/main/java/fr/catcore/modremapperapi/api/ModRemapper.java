package fr.catcore.modremapperapi.api;

import fr.catcore.modremapperapi.remapping.RemapUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.tinyremapper.TinyRemapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ModRemapper {

    String[] getJarFolders();

    default RemapLibrary[] getRemapLibraries() {
        List<RemapLibrary> libraries = new ArrayList<>();
        addRemapLibraries(libraries, FabricLoader.getInstance().getEnvironmentType());
        return libraries.toArray(new RemapLibrary[0]);
    }

    @SuppressWarnings("unused")
    default void addRemapLibraries(List<RemapLibrary> libraries, EnvType environment) {}

    Map<String, List<String>> getExclusions();

    void getMappingList(RemapUtil.MappingList list);

    Optional<TinyRemapper.ApplyVisitorProvider> getPostRemappingVisitor();
}
