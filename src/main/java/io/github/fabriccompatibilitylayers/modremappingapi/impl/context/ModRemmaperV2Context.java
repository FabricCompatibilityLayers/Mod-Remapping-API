package io.github.fabriccompatibilitylayers.modremappingapi.impl.context;

import fr.catcore.modremapperapi.utils.Constants;
import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.*;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper.VisitorInfosImpl;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.mappings.MappingsRegistry;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper.ModTrRemapper;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper.SoftLockFixer;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper.visitor.MRAApplyVisitor;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.utils.CacheUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.tinyremapper.TinyRemapper;
import org.jetbrains.annotations.ApiStatus;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@ApiStatus.Internal
public class ModRemmaperV2Context extends BaseModRemapperContext<ModRemapper> {
    private final List<ModRemapper> remappers;
    private final Set<RemappingFlags> remapFlags = new HashSet<>();
    private final MixinData mixinData = new MixinData();
    private final MappingsRegistryInstance mappingsRegistry;
    private final LibraryHandler libraryHandler = new LibraryHandler();
    private final InternalCacheHandler cacheHandler;

    public ModRemmaperV2Context(String id, List<ModRemapper> remappers) {
        super(id);
        this.remappers = remappers;
        this.cacheHandler = new V2CacheHandler(CacheUtils.getCachePath(id));
        this.mappingsRegistry = new MappingsRegistryInstance(this.cacheHandler);
    }

    @Override
    public void init() {
        for (var remapper : remappers) {
            remapper.init(this.cacheHandler);

            var mappings = remapper.getMappingsConfig();

            if (mappings.getSourceNamespace() != null) {
                this.mappingsRegistry.setSourceNamespace(mappings.getSourceNamespace());
            }

            if (mappings.getExtraMappings() != null) {
                try {
                    this.mappingsRegistry.addToFormattedMappings(
                            new ByteArrayInputStream(mappings.getExtraMappings().get().getBytes(StandardCharsets.UTF_8)),
                            mappings.getRenamingMap()
                    );
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            if (mappings.getDefaultPackage() != null) {
                this.mappingsRegistry.setDefaultPackage(mappings.getDefaultPackage());
            }

            remapFlags.addAll(remapper.getRemappingFlags());
        }

        try {
            this.mappingsRegistry.completeFormattedMappings();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.gatherLibraries();

        this.registerAdditionalMappings();
        this.mappingsRegistry.generateAdditionalMappings();
    }

    @Override
    public void remapMods(Map<ModCandidate, Path> pathMap) {
        Constants.MAIN_LOGGER.debug("Starting jar remapping!");
        SoftLockFixer.preloadClasses();
        var remapper = ModTrRemapper.makeRemapper(this);
        Constants.MAIN_LOGGER.debug("Remapper created!");
        ModTrRemapper.remapMods(remapper, pathMap, this);
        Constants.MAIN_LOGGER.debug("Jar remapping done!");

        this.mappingsRegistry.writeFullMappings();
    }

    @Override
    public void afterRemap() {
        remappers.forEach(ModRemapper::afterRemapping);
    }

    @Override
    public List<ModRemapper> discoverMods(boolean remapClassEdits) {
        var collected = new ArrayList<ModRemapper>();
        var candidates = new ArrayList<ModCandidate>();
        var config2Discoverer = new HashMap<ModDiscovererConfig, ModDiscoverer>();

        for (var remapper : remappers) {
            for (var config : remapper.getModDiscoverers()) {
                var discoverer = new ModDiscoverer(config);
                config2Discoverer.put(config, discoverer);
                candidates.addAll(discoverer.collect());
            }
        }

        for (var remapper : remappers) {
            collected.addAll(remapper.collectSubRemappers(candidates));
        }

        var config2Candidates = candidates.stream()
                .collect(Collectors.groupingBy(ModCandidate::getDiscovererConfig));

        for (var candidate : candidates) {
            mappingsRegistry.addModMappings(candidate.getPath());
        }

        mappingsRegistry.generateModMappings();

        var candidateToOutput = new HashMap<ModCandidate, Path>();

        for (var entry : config2Candidates.entrySet()) {
            var config = entry.getKey();
            candidateToOutput.putAll(
                    config2Discoverer.get(config).computeDestinations(entry.getValue(), this.cacheHandler)
            );
        }

        if (!candidateToOutput.isEmpty()) {
            this.remapMods(candidateToOutput);
        }

        return collected;
    }

    @Override
    public void gatherRemappers() {

    }

    @Override
    public MappingsRegistry getMappingsRegistry() {
        return mappingsRegistry;
    }

    private void registerAdditionalMappings() {
        var builder = new V2MappingBuilderImpl(mappingsRegistry.getAdditionalMappings());

        for (var remapper : remappers) {
            remapper.registerAdditionalMappings(builder);
        }
    }

    @Override
    public void addToRemapperBuilder(TinyRemapper.Builder builder) {
        var preInfos = new VisitorInfosImpl();
        var postInfos = new VisitorInfosImpl();

        for (var remapper : remappers) {
            remapper.registerPreVisitors(preInfos);
            remapper.registerPostVisitors(postInfos);
        }

        builder.extraPreApplyVisitor(new MRAApplyVisitor(preInfos));
        builder.extraPostApplyVisitor(new MRAApplyVisitor(postInfos));
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

    @Override
    public MixinData getMixinData() {
        return mixinData;
    }

    @Override
    public void gatherLibraries() {
        libraryHandler.init(this.mappingsRegistry.getSourceNamespace(), this.cacheHandler);

        var libraries = new ArrayList<RemapLibrary>();

        for (var remapper : remappers) {
            remapper.addRemappingLibraries(libraries, FabricLoader.getInstance().getEnvironmentType());
        }

        libraryHandler.cacheLibraries(libraries);
    }

    @Override
    public CacheHandler getCacheHandler() {
        return this.cacheHandler;
    }
}
