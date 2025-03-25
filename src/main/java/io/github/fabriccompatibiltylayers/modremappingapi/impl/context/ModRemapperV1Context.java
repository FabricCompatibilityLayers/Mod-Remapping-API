package io.github.fabriccompatibiltylayers.modremappingapi.impl.context;

import fr.catcore.modremapperapi.utils.Constants;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ModRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.LibraryHandler;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.MappingsUtilsImpl;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.ModDiscoverer;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.compatibility.V0ModRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings.MappingsRegistry;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.ModTrRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.SoftLockFixer;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.CacheUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.tinyremapper.TinyRemapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

public class ModRemapperV1Context implements ModRemapperContext {
    private final List<ModRemapper> remappers = new ArrayList<>();
    private final Map<String, List<String>> mixin2TargetMap = new HashMap<>();

    public ModRemapperV1Context() {}

    public void init() {
        for (ModRemapper remapper : remappers) {
            Optional<String> pkg = remapper.getDefaultPackage();

            pkg.ifPresent(MappingsUtilsImpl::setDefaultPackage);

            Optional<String> sourceNamespace = remapper.getSourceNamespace();

            sourceNamespace.ifPresent(MappingsUtilsImpl::setSourceNamespace);

            Optional<Supplier<InputStream>> mappings = remapper.getExtraMapping();

            mappings.ifPresent(inputStreamSupplier -> {
                try {
                    MappingsRegistry.generateFormattedMappings(inputStreamSupplier.get());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        if (!MappingsRegistry.generated) {
            try {
                MappingsRegistry.generateFormattedMappings(null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        Path sourceLibraryPath = CacheUtils.getLibraryPath(MappingsUtilsImpl.getSourceNamespace());

        if (!Files.exists(sourceLibraryPath)) {
            try {
                Files.createDirectories(sourceLibraryPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        LibraryHandler.gatherRemapLibraries(remappers);

        MappingsRegistry.registerAdditionalMappings(remappers);
    }

    public void remapMods(Map<Path, Path> pathMap) {
        Constants.MAIN_LOGGER.debug("Starting jar remapping!");
        SoftLockFixer.preloadClasses();
        TinyRemapper remapper = ModTrRemapper.makeRemapper(remappers);
        Constants.MAIN_LOGGER.debug("Remapper created!");
        ModTrRemapper.remapMods(remapper, pathMap);
        Constants.MAIN_LOGGER.debug("Jar remapping done!");

        MappingsUtilsImpl.writeFullMappings();
    }

    @Override
    public void afterRemap() {
        remappers.forEach(ModRemapper::afterRemap);
    }

    @Override
    public void discoverMods(boolean remapClassEdits) {
        ModDiscoverer.init(remappers, remapClassEdits, this);
    }

    private static final String v0EntrypointName = "mod-remapper-api:modremapper";
    private static final String v1EntrypointName = "mod-remapper-api:modremapper_v1";

    @Override
    public void gatherRemappers() {
        FabricLoader.getInstance()
                .getEntrypoints(v0EntrypointName, fr.catcore.modremapperapi.api.ModRemapper.class)
                .stream()
                .map(V0ModRemapper::new)
                .forEach(remappers::add);

        remappers.addAll(FabricLoader.getInstance().getEntrypoints(v1EntrypointName, ModRemapper.class));
    }

    @Override
    public Map<String, List<String>> getMixin2TargetMap() {
        return mixin2TargetMap;
    }
}
