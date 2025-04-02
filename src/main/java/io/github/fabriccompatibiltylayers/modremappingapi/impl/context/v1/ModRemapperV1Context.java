package io.github.fabriccompatibiltylayers.modremappingapi.impl.context.v1;

import fr.catcore.modremapperapi.utils.Constants;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingBuilder;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ModRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.*;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.compatibility.V0ModRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.context.BaseModRemapperContext;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.context.MappingsRegistryInstance;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings.MappingsRegistry;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.ModTrRemapper;
import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.RemappingFlags;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.SoftLockFixer;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.visitor.MRAApplyVisitor;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.tinyremapper.TinyRemapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

public class ModRemapperV1Context extends BaseModRemapperContext<ModRemapper> {
    private final Set<RemappingFlags> remapFlags = new HashSet<>();
    private final List<ModRemapper> remappers = new ArrayList<>();
    private final Map<String, List<String>> mixin2TargetMap = new HashMap<>();
    private final MappingsRegistryInstance mappingsRegistry = new MappingsRegistryInstance();
    private final LibraryHandler libraryHandler = new LibraryHandler();
    private final V1ModDiscoverer modDiscoverer = new V1ModDiscoverer();

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

    public void remapMods(Map<io.github.fabriccompatibilitylayers.modremappingapi.api.v2.ModCandidate, Path> pathMap) {
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

    @Override
    public List<ModRemapper> discoverMods(boolean remapClassEdits) {
        Map<ModCandidate, Path> modPaths = this.modDiscoverer.init(remappers, remapClassEdits, this);

        for (ModCandidate candidate : modPaths.keySet()) {
            mappingsRegistry.addModMappings(candidate.original);
        }

        mappingsRegistry.generateModMappings();

//        this.remapMods(modPaths);

        modPaths.values().forEach(FabricLauncherBase.getLauncher()::addToClassPath);

        return new ArrayList<>();
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
}
