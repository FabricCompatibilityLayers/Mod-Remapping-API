package fr.catcore.modremapperapi.remapping;

import fr.catcore.modremapperapi.utils.Constants;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingUtils;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ModRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.RemapLibrary;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.MappingBuilderImpl;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.MappingsUtilsImpl;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.VisitorInfosImpl;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings.MappingTreeHelper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings.MappingsRegistry;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.SoftLockFixer;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.minecraft.MinecraftRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.resource.RefmapRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.visitor.MRAApplyVisitor;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.visitor.MixinPostApplyVisitor;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.CacheUtils;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.FileUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.tinyremapper.*;
import net.fabricmc.tinyremapper.extension.mixin.MixinExtension;
import org.jetbrains.annotations.ApiStatus;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

public class RemapUtil {
    private static List<ModRemapper> remappers;
    private static MappingTree LOADER_TREE;

    @ApiStatus.Internal
    public static final Map<String, List<String>> MIXINED = new HashMap<>();

    @ApiStatus.Internal
    public static String defaultPackage = "";

    @ApiStatus.Internal
    public static final List<String> MC_CLASS_NAMES = new ArrayList<>();

    @ApiStatus.Internal
    public static void init(List<io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ModRemapper> modRemappers) {
        remappers = modRemappers;

        for (ModRemapper remapper : remappers) {
            Optional<String> pkg = remapper.getDefaultPackage();

            pkg.ifPresent(s -> defaultPackage = s);

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

        downloadRemappingLibs();

        writeMcMappings();

        LOADER_TREE = generateMappings();
        MappingsUtilsImpl.addMappingsToContext(LOADER_TREE);

        for (MappingTree.ClassMapping classView : MappingsRegistry.FORMATTED.getClasses()) {
            String className = classView.getName(MappingsUtilsImpl.getSourceNamespace());

            if (className != null) {
                MC_CLASS_NAMES.add("/" + className + ".class");
            }
        }
    }

    private static void downloadRemappingLibs() {
        try {
            for (ModRemapper remapper : remappers) {
                List<RemapLibrary> libraries = new ArrayList<>();

                remapper.addRemapLibraries(libraries, FabricLoader.getInstance().getEnvironmentType());

                Map<RemapLibrary, Path> libraryPaths = CacheUtils.computeExtraLibraryPaths(libraries, MappingsUtilsImpl.getSourceNamespace());

                for (Map.Entry<RemapLibrary, Path> entry : libraryPaths.entrySet()) {
                    RemapLibrary library = entry.getKey();
                    Path path = entry.getValue();

                    if (!library.url.isEmpty()) {
                        Constants.MAIN_LOGGER.info("Downloading remapping library '" + library.fileName + "' from url '" + library.url + "'");
                        FileUtils.downloadFile(library.url, path);
                        FileUtils.removeEntriesFromZip(path, library.toExclude);
                        Constants.MAIN_LOGGER.info("Remapping library ready for use.");
                    } else if (library.path != null) {
                        Constants.MAIN_LOGGER.info("Extracting remapping library '" + library.fileName + "' from mod jar.");
                        FileUtils.copyZipFile(library.path, path);
                        Constants.MAIN_LOGGER.info("Remapping library ready for use.");
                    }
                }
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @ApiStatus.Internal
    public static void remapMods(Map<Path, Path> pathMap) {
        Constants.MAIN_LOGGER.debug("Starting jar remapping!");
        SoftLockFixer.preloadClasses();
        TinyRemapper remapper = makeRemapper(MappingsRegistry.FORMATTED, LOADER_TREE, MappingsRegistry.MODS);
        Constants.MAIN_LOGGER.debug("Remapper created!");
        remapFiles(remapper, pathMap);
        Constants.MAIN_LOGGER.debug("Jar remapping done!");

        MappingsUtilsImpl.writeFullMappings();
    }

    @ApiStatus.Internal
    public static void writeMcMappings() {
        try {
            MappingTreeHelper.exportMappings(MappingsRegistry.FORMATTED, Constants.MC_MAPPINGS_FILE.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated
    public static class MappingList extends ArrayList<MappingBuilder> {
        public MappingList() {
            super();
        }

        @Deprecated
        public MappingBuilder add(String obfuscated, String intermediary) {
            MappingBuilder builder = MappingBuilder.create(obfuscated, intermediary);
            this.add(builder);
            return builder;
        }

        @Deprecated
        public MappingBuilder add(String name) {
            MappingBuilder builder = MappingBuilder.create(name);
            this.add(builder);
            return builder;
        }

        @ApiStatus.Internal
        public void accept(MappingVisitor visitor) throws IOException {
            for (MappingBuilder builder : this) builder.accept(visitor);
        }
    }

    private static MappingTree generateMappings() {
        MemoryMappingTree mappingTree;

        try {
            mappingTree = MappingTreeHelper.createMappingTree();

            io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingBuilder builder = new MappingBuilderImpl(mappingTree);

            for (ModRemapper remapper : remappers) {
                remapper.registerMappings(builder);
            }

            mappingTree.visitEnd();

            MappingTreeHelper.exportMappings(mappingTree, Constants.EXTRA_MAPPINGS_FILE.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Error while generating remappers mappings", e);
        }

        return mappingTree;
    }

    /**
     * Will create remapper with specified trees.
     */
    private static TinyRemapper makeRemapper(MappingTree... trees) {
        TinyRemapper.Builder builder = TinyRemapper
                .newRemapper()
                .renameInvalidLocals(true)
                .ignoreFieldDesc(false)
                .propagatePrivate(true)
                .ignoreConflicts(true);

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            builder.fixPackageAccess(true);
        }

        for (MappingTree tree : trees) {
            builder.withMappings(MappingTreeHelper.createMappingProvider(tree, MappingsUtilsImpl.getSourceNamespace(), MappingsUtilsImpl.getTargetNamespace()));
        }

        MRAApplyVisitor preApplyVisitor = new MRAApplyVisitor();
        MRAApplyVisitor postApplyVisitor = new MRAApplyVisitor();
        MixinPostApplyVisitor mixinPostApplyVisitor = new MixinPostApplyVisitor();

        VisitorInfosImpl preInfos = new VisitorInfosImpl();
        VisitorInfosImpl postInfos = new VisitorInfosImpl();

        for (ModRemapper modRemapper : remappers) {
            modRemapper.registerPreVisitors(preInfos);
            modRemapper.registerPostVisitors(postInfos);
        }

        preApplyVisitor.setInfos(preInfos);
        postApplyVisitor.setInfos(postInfos);

        builder.extraPreApplyVisitor(preApplyVisitor);
        builder.extraPostApplyVisitor(postApplyVisitor);
        builder.extraPostApplyVisitor(mixinPostApplyVisitor);

        builder.extension(new MixinExtension(EnumSet.of(MixinExtension.AnnotationTarget.HARD)));

        TinyRemapper remapper = builder.build();

        try {
            MinecraftRemapper.addMinecraftJar(remapper);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (ModRemapper modRemapper : remappers) {
            List<RemapLibrary> libraries = new ArrayList<>();

            modRemapper.addRemapLibraries(libraries, FabricLoader.getInstance().getEnvironmentType());

            for (RemapLibrary library : libraries) {
                Path libPath = CacheUtils.getLibraryPath(MappingsUtilsImpl.getSourceNamespace()).resolve(library.fileName);

                if (Files.exists(libPath)) {
                    remapper.readClassPathAsync(libPath);
                } else {
                    Constants.MAIN_LOGGER.info("Library " + libPath + " does not exist.");
                }
            }
        }

        return remapper;
    }

    /**
     * Will remap file with specified remapper and store it into output.
     *
     * @param remapper {@link TinyRemapper} to remap with.
     */
    private static void remapFiles(TinyRemapper remapper, Map<Path, Path> paths) {
        List<OutputConsumerPath> outputConsumerPaths = new ArrayList<>();

        List<OutputConsumerPath.ResourceRemapper> resourceRemappers = new ArrayList<>(NonClassCopyMode.FIX_META_INF.remappers);
        resourceRemappers.add(new RefmapRemapper());

        applyRemapper(remapper, paths, outputConsumerPaths, resourceRemappers, true, MappingsUtilsImpl.getSourceNamespace(), MappingsUtilsImpl.getTargetNamespace());
    }

    @ApiStatus.Internal
    public static void applyRemapper(TinyRemapper remapper, Map<Path, Path> paths, List<OutputConsumerPath> outputConsumerPaths, List<OutputConsumerPath.ResourceRemapper> resourceRemappers, boolean analyzeMapping, String srcNamespace, String targetNamespace) {
        try {
            Map<Path, InputTag> tagMap = new HashMap<>();

            Constants.MAIN_LOGGER.debug("Creating InputTags!");
            for (Path input : paths.keySet()) {
                InputTag tag = remapper.createInputTag();
                tagMap.put(input, tag);
                remapper.readInputsAsync(tag, input);
            }

            Constants.MAIN_LOGGER.debug("Initializing remapping!");
            for (Map.Entry<Path, Path> entry : paths.entrySet()) {
                Constants.MAIN_LOGGER.debug("Starting remapping " + entry.getKey().toString() + " to " + entry.getValue().toString());
                OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(entry.getValue()).build();

                outputConsumerPaths.add(outputConsumer);

                Constants.MAIN_LOGGER.debug("Apply remapper!");
                remapper.apply(outputConsumer, tagMap.get(entry.getKey()));

                Constants.MAIN_LOGGER.debug("Add input as non class file!");
                outputConsumer.addNonClassFiles(entry.getKey(), remapper, resourceRemappers);

                Constants.MAIN_LOGGER.debug("Done 1!");
            }

            if (analyzeMapping) MappingsUtilsImpl.completeMappingsFromTr(remapper.getEnvironment(), srcNamespace);
        } catch (Exception e) {
            remapper.finish();
            outputConsumerPaths.forEach(o -> {
                try {
                    o.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            });
            throw new RuntimeException("Failed to remap jar", e);
        } finally {
            remapper.finish();
            outputConsumerPaths.forEach(o -> {
                try {
                    o.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @Deprecated
    public static String getRemappedFieldName(Class<?> owner, String fieldName) {
        return MappingUtils.mapField(owner, fieldName).name;
    }

    @Deprecated
    public static String getRemappedMethodName(Class<?> owner, String methodName, Class<?>[] parameterTypes) {
        return MappingUtils.mapMethod(owner, methodName, parameterTypes).name;
    }

    /**
     * A shortcut to the Fabric Environment getter.
     */
    @Deprecated
    public static EnvType getEnvironment() {
        return FabricLoader.getInstance().getEnvironmentType();
    }

    @Deprecated
    public static String getNativeNamespace() {
        return MappingsUtilsImpl.getNativeNamespace();
    }
}
