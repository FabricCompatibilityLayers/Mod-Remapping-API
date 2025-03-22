package io.github.fabriccompatibiltylayers.modremappingapi.impl;

import fr.catcore.modremapperapi.utils.Constants;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ModRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings.MappingsRegistry;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.ModTrRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.SoftLockFixer;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.CacheUtils;
import net.fabricmc.tinyremapper.TinyRemapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class ModRemapperContext {
    private final List<ModRemapper> remappers;

    public ModRemapperContext(List<ModRemapper> remappers) {
        this.remappers = remappers;
    }

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
}
