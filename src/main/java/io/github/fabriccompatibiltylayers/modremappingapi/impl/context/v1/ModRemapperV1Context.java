package io.github.fabriccompatibiltylayers.modremappingapi.impl.context.v1;

import fr.catcore.modremapperapi.utils.Constants;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ModRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.LibraryHandler;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.MappingsUtilsImpl;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.ModDiscoverer;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.compatibility.V0ModRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.context.BaseModRemapperContext;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.context.MappingsRegistryInstance;
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

public class ModRemapperV1Context extends BaseModRemapperContext {
    private final List<ModRemapper> remappers = new ArrayList<>();
    private final Map<String, List<String>> mixin2TargetMap = new HashMap<>();
    private final MappingsRegistryInstance mappingsRegistry = new MappingsRegistryInstance();

    public static ModRemapperV1Context INSTANCE;

    public ModRemapperV1Context() {
        super("mod-remapping-api:v1");
        INSTANCE = this;
    }

    public void init() {
        for (ModRemapper remapper : remappers) {
            Optional<String> pkg = remapper.getDefaultPackage();

            pkg.ifPresent(this.mappingsRegistry::setDefaultPackage);

            Optional<String> sourceNamespace = remapper.getSourceNamespace();

            sourceNamespace.ifPresent(MappingsUtilsImpl::setSourceNamespace);

            Optional<Supplier<InputStream>> mappings = remapper.getExtraMapping();

            mappings.ifPresent(inputStreamSupplier -> {
                try {
                    this.mappingsRegistry.addToFormattedMappings(inputStreamSupplier.get());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        try {
            this.mappingsRegistry.completeFormattedMappings();
        } catch (IOException e) {
            throw new RuntimeException(e);
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

    @Override
    public MappingsRegistry getMappingsRegistry() {
        return this.mappingsRegistry;
    }
}
