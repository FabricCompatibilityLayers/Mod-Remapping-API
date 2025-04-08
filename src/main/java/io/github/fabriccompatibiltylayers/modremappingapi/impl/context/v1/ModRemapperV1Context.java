package io.github.fabriccompatibiltylayers.modremappingapi.impl.context.v1;

import fr.catcore.modremapperapi.utils.Constants;
import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.ModCandidate;
import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.ModDiscovererConfig;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.DefaultModCandidate;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.InternalCacheHandler;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingBuilder;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ModRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.*;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.compatibility.V0ModRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.context.BaseModRemapperContext;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.context.MappingsRegistryInstance;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.context.MixinData;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.context.v2.V2ModDiscoverer;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings.MappingsRegistry;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.ModTrRemapper;
import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.RemappingFlags;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.SoftLockFixer;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.visitor.MRAApplyVisitor;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.FileUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.tinyremapper.TinyRemapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
            Optional<String> pkg = remapper.getDefaultPackage();

            pkg.ifPresent(this.mappingsRegistry::setDefaultPackage);

            Optional<String> sourceNamespace = remapper.getSourceNamespace();

            sourceNamespace.ifPresent(this.mappingsRegistry::setSourceNamespace);

            Optional<Supplier<InputStream>> mappings = remapper.getExtraMapping();

            mappings.ifPresent(inputStreamSupplier -> {
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

        libraryHandler.init(this.mappingsRegistry.getSourceNamespace());

        libraryHandler.gatherRemapLibraries(remappers);

        this.registerAdditionalMappings();
        this.mappingsRegistry.generateAdditionalMappings();
    }

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
        remappers.forEach(ModRemapper::afterRemap);
    }

    private List<ModCandidate> collectCandidates(ModDiscovererConfig config, Path modPath, List<String> entries) {
        boolean fabric = false;
        boolean hasClass = false;

        for (String entry : entries) {
            if (entry.endsWith("fabric.mod.json") || entry.endsWith("quilt.mod.json") || entry.endsWith("quilt.mod.json5")) {
                fabric = true;
                break;
            }

            if (entry.endsWith(".class")) {
                hasClass = true;
            }
        }

        List<ModCandidate> list = new ArrayList<>();

        if (hasClass && !fabric) {
            list.add(new DefaultModCandidate(modPath, config));
        }

        return list;
    }

    @Override
    public List<ModRemapper> discoverMods(boolean remapClassEdits) {
        Map<String, List<String>> excluded = new HashMap<>();

        Set<String> modFolders = new HashSet<>();

        for (ModRemapper remapper : remappers) {
            Collections.addAll(modFolders, remapper.getJarFolders());

            if (remapper instanceof V0ModRemapper) {
                excluded.putAll(((V0ModRemapper) remapper).getExclusions());
            }
        }

        List<ModCandidate> candidates = new ArrayList<>();
        Map<ModDiscovererConfig, V2ModDiscoverer> config2Discoverer = new HashMap<>();

        for (String modFolder : modFolders) {
            ModDiscovererConfig config = ModDiscovererConfig.builder(modFolder)
                    .fileNameMatcher("(.+).(jar|zip)$")
                    .candidateCollector(this::collectCandidates)
                    .build();
            V2ModDiscoverer discoverer = new V2ModDiscoverer(config);
            config2Discoverer.put(config, discoverer);
            candidates.addAll(discoverer.collect());
        }

        try {
            this.handleV0Excluded(candidates, excluded);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

        Map<ModDiscovererConfig, List<ModCandidate>> config2Candidates =
                candidates.stream().collect(Collectors.groupingBy(ModCandidate::getDiscovererConfig));

        for (Map.Entry<ModDiscovererConfig, List<ModCandidate>> entry : config2Candidates.entrySet()) {
            ModDiscovererConfig config = entry.getKey();

            try {
                config2Discoverer.get(config).excludeClassEdits(entry.getValue(), this.cacheHandler, this.mappingsRegistry);
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

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

        candidateToOutput.values().forEach(FabricLauncherBase.getLauncher()::addToClassPath);

        return new ArrayList<>();
    }

    private void handleV0Excluded(List<ModCandidate> mods, Map<String, List<String>> excludedMap) throws IOException, URISyntaxException {
        for (ModCandidate modCandidate : mods) {
            if (excludedMap.containsKey(modCandidate.getId())) {
                if (Files.isDirectory(modCandidate.getPath())) {
                    for (String excluded : excludedMap.get(modCandidate.getId())) {
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
        FabricLoader.getInstance()
                .getEntrypoints(v0EntrypointName, fr.catcore.modremapperapi.api.ModRemapper.class)
                .stream()
                .map(V0ModRemapper::new)
                .forEach(remappers::add);

        remappers.addAll(FabricLoader.getInstance().getEntrypoints(v1EntrypointName, ModRemapper.class));
    }

    @Override
    public MappingsRegistry getMappingsRegistry() {
        return this.mappingsRegistry;
    }

    private void registerAdditionalMappings() {
        MappingBuilder builder = new MappingBuilderImpl(mappingsRegistry.getAdditionalMappings());

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
}
