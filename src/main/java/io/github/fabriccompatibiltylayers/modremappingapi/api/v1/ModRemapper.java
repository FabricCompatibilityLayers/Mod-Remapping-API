package io.github.fabriccompatibiltylayers.modremappingapi.api.v1;

import net.fabricmc.api.EnvType;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

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

    default Optional<String> getSourceNamespace() {
        return Optional.empty();
    }

    default Optional<Supplier<InputStream>> getExtraMapping() {
        return Optional.empty();
    }

    /**
     * Whether to enable mixin remapping. Enabled by default for compatibility purposes.
     * @return true - enabled <br> false - disabled
     */
    default boolean remapMixins() {
        return true;
    }
}
