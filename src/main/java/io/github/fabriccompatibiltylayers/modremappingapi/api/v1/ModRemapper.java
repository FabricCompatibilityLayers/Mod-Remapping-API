package io.github.fabriccompatibiltylayers.modremappingapi.api.v1;

import net.fabricmc.api.EnvType;

import java.util.List;
import java.util.Optional;

public interface ModRemapper {
    String[] getJarFolders();

    void addRemapLibraries(List<RemapLibrary> libraries, EnvType environment);

    void registerMappings(MappingBuilder list);

    void registerPreVisitors(VisitorInfos infos);
    void registerPostVisitors(VisitorInfos infos);

    default Optional<String> getDefaultPackage() {
        return Optional.empty();
    }

    default void afterRemap() {}
}
