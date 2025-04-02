package io.github.fabriccompatibiltylayers.modremappingapi.impl.context.v2;

import fr.catcore.modremapperapi.utils.Constants;
import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.*;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.LibraryHandler;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.context.BaseModRemapperContext;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.context.MappingsRegistryInstance;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings.MappingsRegistry;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.ModTrRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.SoftLockFixer;
import net.fabricmc.tinyremapper.TinyRemapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ModRemmaperV2Context extends BaseModRemapperContext<ModRemapper> {
    private final List<ModRemapper> remappers;
    private final Set<RemappingFlags> remapFlags = new HashSet<>();
    private final Map<String, List<String>> mixin2TargetMap = new HashMap<>();
    private final MappingsRegistryInstance mappingsRegistry = new MappingsRegistryInstance();
    private final LibraryHandler libraryHandler = new LibraryHandler();

    public ModRemmaperV2Context(String id, List<ModRemapper> remappers) {
        super(id);
        this.remappers = remappers;
    }

    @Override
    public void init() {
        for (ModRemapper remapper : remappers) {
            MappingsConfig mappings = remapper.getMappingsConfig();

            if (mappings.getSourceNamespace() != null) {
                this.mappingsRegistry.setSourceNamespace(mappings.getSourceNamespace());
            }

            if (mappings.getExtraMappings() != null) {
                try {
                    this.mappingsRegistry.addToFormattedMappings(mappings.getExtraMappings().get(), mappings.getRenamingMap());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            remapFlags.addAll(remapper.getRemappingFlags());
        }

        try {
            this.mappingsRegistry.completeFormattedMappings();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        libraryHandler.init(this.mappingsRegistry.getSourceNamespace());

        this.mappingsRegistry.generateAdditionalMappings();
    }

    @Override
    public void remapMods(Map<ModCandidate, Path> pathMap) {
        Constants.MAIN_LOGGER.debug("Starting jar remapping!");
        SoftLockFixer.preloadClasses();
        TinyRemapper remapper = ModTrRemapper.makeRemapper(this);
        Constants.MAIN_LOGGER.debug("Remapper created!");
        ModTrRemapper.remapMods(remapper, pathMap, this);
        Constants.MAIN_LOGGER.debug("Jar remapping done!");

        this.mappingsRegistry.writeFullMappings();
    }

    @Override
    public void afterRemap() {
        for (ModRemapper remapper : remappers) {
            remapper.afterRemapping();
        }
    }

    @Override
    public List<ModRemapper> discoverMods(boolean remapClassEdits) {
        List<ModRemapper> collected = new ArrayList<>();
        List<ModCandidate> candidates = new ArrayList<>();
        Map<ModDiscovererConfig, V2ModDiscoverer> config2Discoverer = new HashMap<>();

        for (ModRemapper remapper : remappers) {
            for (ModDiscovererConfig config : remapper.getModDiscoverers()) {
                V2ModDiscoverer discoverer = new V2ModDiscoverer(this.contextId, config);
                config2Discoverer.put(config, discoverer);
                candidates.addAll(discoverer.collect());
            }
        }

        for (ModRemapper remapper : remappers) {
            collected.addAll(remapper.collectSubRemappers(candidates));
        }

        Map<ModDiscovererConfig, List<ModCandidate>> config2Candidates =
                candidates.stream().collect(Collectors.groupingBy(ModCandidate::getDiscovererConfig));

        for (ModCandidate candidate : candidates) {
            mappingsRegistry.addModMappings(candidate.getPath());
        }

        mappingsRegistry.generateModMappings();

        Map<ModCandidate, Path> candidateToOutput = new HashMap<>();

        for (Map.Entry<ModDiscovererConfig, List<ModCandidate>> entry : config2Candidates.entrySet()) {
            ModDiscovererConfig config = entry.getKey();

            candidateToOutput.putAll(
                    config2Discoverer.get(config).computeDestinations(entry.getValue())
            );
        }

        if (!candidateToOutput.isEmpty()) this.remapMods(candidateToOutput);

        return collected;
    }

    @Override
    public void gatherRemappers() {

    }

    @Override
    public Map<String, List<String>> getMixin2TargetMap() {
        return mixin2TargetMap;
    }

    @Override
    public MappingsRegistry getMappingsRegistry() {
        return mappingsRegistry;
    }

    @Override
    public void addToRemapperBuilder(TinyRemapper.Builder builder) {

    }

    @Override
    public Set<RemappingFlags> getRemappingFlags() {
        return remapFlags;
    }

    @Override
    public LibraryHandler getLibraryHandler() {
        return libraryHandler;
    }

    public List<ModRemapper> getRemappers() {
        return remappers;
    }
}
