package fr.catcore.modremapperapi.api;

import fr.catcore.modremapperapi.remapping.RemapUtil;
import fr.catcore.modremapperapi.remapping.VisitorInfos;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @deprecated Use {@link io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ModRemapper} instead with entrypoint key "mod-remapper-api:modremapper_v1"
 */
@Deprecated
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

    void registerVisitors(VisitorInfos infos);

    default Optional<String> getDefaultPackage() {
        return Optional.empty();
    }

    default void afterRemap() {}
}
