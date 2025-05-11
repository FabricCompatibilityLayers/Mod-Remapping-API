package io.github.fabriccompatibilitylayers.modremappingapi.impl.compatibility.v1;

import fr.catcore.modremapperapi.utils.Constants;
import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.CacheHandler;
import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.ModCandidate;
import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.ModDiscovererConfig;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.context.*;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.defaults.DefaultModCandidate;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper.VisitorInfosImpl;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingBuilder;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ModRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.RemapLibrary;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.compatibility.v0.V0ModRemapper;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.mappings.MappingsRegistry;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper.ModTrRemapper;
import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.RemappingFlags;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper.SoftLockFixer;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper.visitor.MRAApplyVisitor;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.utils.FileUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.tinyremapper.TinyRemapper;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@ApiStatus.Internal
public class ModRemapperV1Context extends BaseModRemapperContext<ModRemapper> {
    private final Set<RemappingFlags> remapFlags = new HashSet<>();
    private final List<ModRemapper> remappers = new ArrayList<>();
    private final MixinData mixinData = new MixinData();
    private final InternalCacheHandler cacheHandler = new V1CacheHandler();
    private final MappingsRegistryInstance mappingsRegistry = new MappingsRegistryInstance(cacheHandler);
    private final LibraryHandler libraryHandler = new LibraryHandler();

    public static ModRemapperV1Context INSTANCE;

    public ModRemapperV1Context() {
        super("mod-remapping-api_v1");
        INSTANCE = this;
    }

    public void init() {
        for (ModRemapper remapper : remappers) {
            remapper.getDefaultPackage().ifPresent(this.mappingsRegistry::setDefaultPackage);
            remapper.getSourceNamespace().ifPresent(this.mappingsRegistry::setSourceNamespace);

            remapper.getExtraMapping().ifPresent(inputStreamSupplier -> {
                try {
                    this.mappingsRegistry.addToFormattedMappings(inputStreamSupplier.get(), new HashMap<>());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            if (remapper.remapMixins()) {
                remapFlags.add(RemappingFlags.MIXIN);
            }
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
        remappers.forEach(ModRemapper::afterRemap);
    }

    private List<ModCandidate> collectCandidates(ModDiscovererConfig config, Path modPath, List<String> entries) {
        boolean fabric = false;
        boolean hasClass = false;

        for (var entry : entries) {
            if (entry.endsWith("fabric.mod.json") || entry.endsWith("quilt.mod.json") || entry.endsWith("quilt.mod.json5")) {
                fabric = true;
                break;
            }

            if (entry.endsWith(".class")) {
                hasClass = true;
            }
        }

        var list = new ArrayList<ModCandidate>();

        if (hasClass && !fabric) {
            list.add(new DefaultModCandidate(modPath, config));
        }

        return list;
    }

    @Override
    public List<ModRemapper> discoverMods(boolean remapClassEdits) {
        var excluded = new HashMap<String, List<String>>();
        var modFolders = new HashSet<String>();

        for (var remapper : remappers) {
            Collections.addAll(modFolders, remapper.getJarFolders());

            if (remapper instanceof V0ModRemapper v0Remapper) {
                excluded.putAll(v0Remapper.getExclusions());
            }
        }

        var candidates = new ArrayList<ModCandidate>();
        var config2Discoverer = new HashMap<ModDiscovererConfig, ModDiscoverer>();

        for (var modFolder : modFolders) {
            var config = ModDiscovererConfig.builder(modFolder)
                    .fileNameMatcher("(.+).(jar|zip)$")
                    .candidateCollector(this::collectCandidates)
                    .build();
            var discoverer = new ModDiscoverer(config);
            config2Discoverer.put(config, discoverer);
            candidates.addAll(discoverer.collect());
        }

        try {
            this.handleV0Excluded(candidates, excluded);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

        var config2Candidates = candidates.stream()
                .collect(Collectors.groupingBy(ModCandidate::getDiscovererConfig));

        for (var entry : config2Candidates.entrySet()) {
            var config = entry.getKey();

            try {
                config2Discoverer.get(config).excludeClassEdits(entry.getValue(), this.cacheHandler, this.mappingsRegistry);
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        candidates.forEach(candidate ->
            mappingsRegistry.addModMappings(candidate.getPath())
        );

        mappingsRegistry.generateModMappings();

        var candidateToOutput = new HashMap<ModCandidate, Path>();

        for (var entry : config2Candidates.entrySet()) {
            var config = entry.getKey();

            candidateToOutput.putAll(
                    config2Discoverer.get(config).computeDestinations(entry.getValue(), this.cacheHandler)
            );
        }

        if (!candidateToOutput.isEmpty()) this.remapMods(candidateToOutput);

        candidateToOutput.values().forEach(FabricLauncherBase.getLauncher()::addToClassPath);

        return new ArrayList<>();
    }

    private void handleV0Excluded(List<ModCandidate> mods, Map<String, List<String>> excludedMap) throws IOException, URISyntaxException {
        for (var modCandidate : mods) {
            if (excludedMap.containsKey(modCandidate.getId())) {
                if (Files.isDirectory(modCandidate.getPath())) {
                    for (var excluded : excludedMap.get(modCandidate.getId())) {
                        if (Files.deleteIfExists(modCandidate.getPath().resolve(excluded))) {
                            Constants.MAIN_LOGGER.debug("File deleted: " + modCandidate.getPath().resolve(excluded));
                        }
                    }
                } else {
                    FileUtils.removeEntriesFromZip(modCandidate.getPath(), excludedMap.get(modCandidate.getId()));
                }
            }
        }
    }

    private static final String v0EntrypointName = "mod-remapper-api:modremapper";
    private static final String v1EntrypointName = "mod-remapper-api:modremapper_v1";

    @Override
    public void gatherRemappers() {
        // Collect v0 remappers
        var v0Remappers = FabricLoader.getInstance()
                .getEntrypoints(v0EntrypointName, fr.catcore.modremapperapi.api.ModRemapper.class)
                .stream()
                .map(V0ModRemapper::new)
                .toList();

        remappers.addAll(v0Remappers);

        // Collect v1 remappers
        remappers.addAll(FabricLoader.getInstance().getEntrypoints(v1EntrypointName, ModRemapper.class));

        if (remappers.size() == 1) remappers.clear();
    }

    @Override
    public MappingsRegistry getMappingsRegistry() {
        return this.mappingsRegistry;
    }

    private void registerAdditionalMappings() {
        MappingBuilder builder = new V1MappingBuilderImpl(mappingsRegistry.getAdditionalMappings());

        for (ModRemapper remapper : remappers) {
            remapper.registerMappings(builder);
        }
    }

    @Override
    public void addToRemapperBuilder(TinyRemapper.Builder builder) {
        VisitorInfosImpl preInfos = new VisitorInfosImpl();
        VisitorInfosImpl postInfos = new VisitorInfosImpl();

        for (ModRemapper modRemapper : remappers) {
            modRemapper.registerPreVisitors(preInfos);
            modRemapper.registerPostVisitors(postInfos);
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

    @Override
    public MixinData getMixinData() {
        return mixinData;
    }

    @Override
    public void gatherLibraries() {
        libraryHandler.init(this.mappingsRegistry.getSourceNamespace(), this.cacheHandler);

        var libraries = new ArrayList<RemapLibrary>();

        remappers.forEach(remapper ->
            remapper.addRemapLibraries(libraries, FabricLoader.getInstance().getEnvironmentType())
        );

        libraryHandler.cacheLibraries(new ArrayList<>(libraries));
    }

    @Override
    public CacheHandler getCacheHandler() {
        return this.cacheHandler;
    }
}
