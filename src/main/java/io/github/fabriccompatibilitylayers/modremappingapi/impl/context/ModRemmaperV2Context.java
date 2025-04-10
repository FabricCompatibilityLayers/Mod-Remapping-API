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
        for (ModRemapper remapper : remappers) {
            remapper.init(this.cacheHandler);

            MappingsConfig mappings = remapper.getMappingsConfig();

            if (mappings.getSourceNamespace() != null) {
                this.mappingsRegistry.setSourceNamespace(mappings.getSourceNamespace());
            }

            if (mappings.getExtraMappings() != null) {
                try {
                    this.mappingsRegistry.addToFormattedMappings(new ByteArrayInputStream(mappings.getExtraMappings().get().getBytes(StandardCharsets.UTF_8)), mappings.getRenamingMap());
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
        Map<ModDiscovererConfig, ModDiscoverer> config2Discoverer = new HashMap<>();

        for (ModRemapper remapper : remappers) {
            for (ModDiscovererConfig config : remapper.getModDiscoverers()) {
                ModDiscoverer discoverer = new ModDiscoverer(config);
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
                    config2Discoverer.get(config).computeDestinations(entry.getValue(), this.cacheHandler)
            );
        }

        if (!candidateToOutput.isEmpty()) this.remapMods(candidateToOutput);

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
        MappingBuilder builder = new V2MappingBuilderImpl(mappingsRegistry.getAdditionalMappings());

        for (ModRemapper remapper : remappers) {
            remapper.registerAdditionalMappings(builder);
        }
    }

    @Override
    public void addToRemapperBuilder(TinyRemapper.Builder builder) {
        VisitorInfosImpl preInfos = new VisitorInfosImpl();
        VisitorInfosImpl postInfos = new VisitorInfosImpl();

        for (ModRemapper remapper : remappers) {
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

        List<RemapLibrary> libraries = new ArrayList<>();

        for (ModRemapper remapper : remappers) {
            remapper.addRemappingLibraries(libraries, FabricLoader.getInstance().getEnvironmentType());
        }

        libraryHandler.cacheLibraries(libraries);
    }

    @Override
    public CacheHandler getCacheHandler() {
        return this.cacheHandler;
    }
}
