package fr.catcore.modremapperapi.remapping;

import fr.catcore.modremapperapi.utils.Constants;
import fr.catcore.modremapperapi.utils.FileUtils;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingUtils;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ModRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.RemapLibrary;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.MappingBuilderImpl;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.MappingsUtilsImpl;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.VisitorInfosImpl;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.minecraft.MinecraftRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.resource.RefmapRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.visitor.MRAApplyVisitor;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.visitor.MixinPostApplyVisitor;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.CacheUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.tinyremapper.*;
import net.fabricmc.tinyremapper.extension.mixin.MixinExtension;
import org.jetbrains.annotations.ApiStatus;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class RemapUtil {
    private static List<ModRemapper> remappers;
    private static MappingTree LOADER_TREE;
    private static MappingTree MINECRAFT_TREE;
    private static MemoryMappingTree MODS_TREE;

    @ApiStatus.Internal
    public static final Map<String, List<String>> MIXINED = new HashMap<>();

    private static String defaultPackage = "";

    public static final List<String> MC_CLASS_NAMES = new ArrayList<>();

    public static void init(List<io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ModRemapper> modRemappers) {
        remappers = modRemappers;

        for (ModRemapper remapper : remappers) {
            Optional<String> pkg = remapper.getDefaultPackage();

            pkg.ifPresent(s -> defaultPackage = s);

            Optional<String> sourceNamespace = remapper.getSourceNamespace();

            sourceNamespace.ifPresent(MappingsUtilsImpl::setSourceNamespace);

            Optional<Supplier<InputStream>> mappings = remapper.getExtraMapping();

            mappings.ifPresent(inputStreamSupplier -> MappingsUtilsImpl.loadExtraMappings(inputStreamSupplier.get()));
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

        MINECRAFT_TREE = MappingsUtilsImpl.getMinecraftMappings();

        writeMcMappings();

        LOADER_TREE = generateMappings();
        MappingsUtilsImpl.addMappingsToContext(LOADER_TREE);

        for (MappingTree.ClassMapping classView : MINECRAFT_TREE.getClasses()) {
            String className = classView.getName(MappingsUtilsImpl.getSourceNamespace());

            if (className != null) {
                MC_CLASS_NAMES.add(className);
            }
        }

        try {
            MODS_TREE = new MemoryMappingTree();
            MappingsUtilsImpl.initializeMappingTree(MODS_TREE);
        } catch (IOException e) {
            throw new RuntimeException(e);
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
                        io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.FileUtils.downloadFile(library.url, path);
                        FileUtils.excludeFromZipFile(path.toFile(), library.toExclude);
                        Constants.MAIN_LOGGER.info("Remapping library ready for use.");
                    } else if (library.path != null) {
                        Constants.MAIN_LOGGER.info("Extracting remapping library '" + library.fileName + "' from mod jar.");
                        io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.FileUtils.copyZipFile(library.path, path);
                        Constants.MAIN_LOGGER.info("Remapping library ready for use.");
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void remapMods(Map<Path, Path> pathMap) {
        Constants.MAIN_LOGGER.debug("Starting jar remapping!");
        preloadClasses();
        TinyRemapper remapper = makeRemapper(MINECRAFT_TREE, LOADER_TREE, MODS_TREE);
        Constants.MAIN_LOGGER.debug("Remapper created!");
        remapFiles(remapper, pathMap);
        Constants.MAIN_LOGGER.debug("Jar remapping done!");

        MappingsUtilsImpl.writeFullMappings();
    }

    public static List<String> makeModMappings(Path modPath) {
        File path = modPath.toFile();
        List<String> files = new ArrayList<>();
        if (path.isFile()) {
            try {
                FileInputStream fileinputstream = new FileInputStream(path);
                ZipInputStream zipinputstream = new ZipInputStream(fileinputstream);

                while (true) {
                    ZipEntry zipentry = zipinputstream.getNextEntry();
                    if (zipentry == null) {
                        zipinputstream.close();
                        fileinputstream.close();
                        break;
                    }

                    String s1 = zipentry.getName();
                    if (!zipentry.isDirectory()) {
                        files.add(s1.replace("\\", "/"));
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (path.isDirectory()) {
            files.addAll(generateFolderMappings(path.listFiles()));
        }

        List<String> classes = new ArrayList<>();

        for (String file : files) {
            if (file.endsWith(".class")) {
                String clName = file.replace(".class", "");
                classes.add(clName);
            }
        }

        io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingBuilder mappingBuilder = new MappingBuilderImpl(MODS_TREE);

        classes.forEach(cl -> mappingBuilder.addMapping(cl, (cl.contains("/") ? "" : defaultPackage) + cl));

        return files;
    }

    public static void generateModMappings() {
        try {
            MODS_TREE.visitEnd();

            MappingWriter writer = MappingWriter.create(Constants.REMAPPED_MAPPINGS_FILE.toPath(), MappingFormat.TINY_2_FILE);
            MODS_TREE.accept(writer);
        } catch (IOException e) {
            throw new RuntimeException("Error while generating mods mappings", e);
        }

        MappingsUtilsImpl.addMappingsToContext(MODS_TREE);
    }

    public static void writeMcMappings() {
        try {
            MappingWriter writer = MappingWriter.create(Constants.MC_MAPPINGS_FILE.toPath(), MappingFormat.TINY_2_FILE);
            MINECRAFT_TREE.accept(writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<String> generateFolderMappings(File[] files) {
        List<String> list = new ArrayList<>();

        for (File file : files) {
            if (file.isFile()) list.add(file.getName());
            else if (file.isDirectory()) {
                String name = file.getName();

                for (String fileName : generateFolderMappings(file.listFiles())) {
                    list.add(name + "/" + fileName);
                }
            }
        }

        return list;
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
        MemoryMappingTree mappingTree = new MemoryMappingTree();

        try {
            MappingsUtilsImpl.initializeMappingTree(mappingTree);

            io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingBuilder builder = new MappingBuilderImpl(mappingTree);

            for (ModRemapper remapper : remappers) {
                remapper.registerMappings(builder);
            }

            mappingTree.visitEnd();

            MappingWriter mappingWriter = MappingWriter.create(Constants.EXTRA_MAPPINGS_FILE.toPath(), MappingFormat.TINY_2_FILE);
            mappingTree.accept(mappingWriter);
        } catch (IOException e) {
            throw new RuntimeException("Error while generating remappers mappings", e);
        }

        return mappingTree;
    }

    private static String getLibClassName(String lib, String string) {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            return "net.fabricmc." + lib + "." + string;
        }

        return "fr.catcore.modremapperapi.impl.lib." + lib + "." + string;
    }

    private static void preloadClasses() {
        for (String clazz : new String[]{
                "java.io.IOException",
                "java.net.URI",
                "java.net.URISyntaxException",
                "java.nio.file.FileSystem",
                "java.nio.file.FileVisitResult",
                "java.nio.file.Files",
                "java.nio.file.Path",
                "java.nio.file.SimpleFileVisitor",
                "java.nio.file.attribute.BasicFileAttributes",
                "java.util.ArrayDeque",
                "java.util.ArrayList",
                "java.util.Collection",
                "java.util.Collections",
                "java.util.HashMap",
                "java.util.HashSet",
                "java.util.IdentityHashMap",
                "java.util.List",
                "java.util.Map",
                "java.util.Objects",
                "java.util.Optional",
                "java.util.Queue",
                "java.util.Set",
                "java.util.concurrent.CompletableFuture",
                "java.util.concurrent.ConcurrentHashMap",
                "java.util.concurrent.ExecutionException",
                "java.util.concurrent.ExecutorService",
                "java.util.concurrent.Executors",
                "java.util.concurrent.Future",
                "java.util.concurrent.TimeUnit",
                "java.util.concurrent.atomic.AtomicReference",
                "java.util.function.BiConsumer",
                "java.util.function.Supplier",
                "java.util.regex.Pattern",
                "java.util.stream.Collectors",
                "java.util.zip.ZipError",

                "org.objectweb.asm.ClassReader",
                "org.objectweb.asm.ClassVisitor",
                "org.objectweb.asm.ClassWriter",
                "org.objectweb.asm.FieldVisitor",
                "org.objectweb.asm.MethodVisitor",
                "org.objectweb.asm.Opcodes",
                "org.objectweb.asm.commons.Remapper",
                "org.objectweb.asm.util.CheckClassAdapter",

                "fr.catcore.modremapperapi.api.RemapLibrary",
                "fr.catcore.modremapperapi.api.ModRemapper",
                "fr.catcore.modremapperapi.utils.BArrayList",
                "fr.catcore.modremapperapi.utils.CollectionUtils",
                "fr.catcore.modremapperapi.utils.Constants",
                "io.github.fabriccompatibiltylayers.modremappingapi.impl.DefaultModEntry",
                "io.github.fabriccompatibiltylayers.modremappingapi.impl.DefaultModRemapper",
                "fr.catcore.modremapperapi.utils.FileUtils",
                "fr.catcore.modremapperapi.utils.MappingsUtils",
                "fr.catcore.modremapperapi.utils.MixinUtils",
                "io.github.fabriccompatibiltylayers.modremappingapi.impl.ModDiscoverer",
                "io.github.fabriccompatibiltylayers.modremappingapi.impl.ModDiscoverer$1",
                "io.github.fabriccompatibiltylayers.modremappingapi.impl.ModEntry",
                "io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.resource.RefmapJson",
                "fr.catcore.modremapperapi.remapping.MapEntryType",
                "fr.catcore.modremapperapi.remapping.MappingBuilder",
                "fr.catcore.modremapperapi.remapping.MappingBuilder$Entry",
                "fr.catcore.modremapperapi.remapping.MappingBuilder$Type",
                "io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.visitor.MixinPostApplyVisitor",
                "io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.asm.MRAClassVisitor",
                "io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.asm.MRAMethodVisitor",
                "io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.visitor.MRAApplyVisitor",
                "io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.resource.RefmapRemapper",
                "fr.catcore.modremapperapi.remapping.RemapUtil",
                "fr.catcore.modremapperapi.remapping.RemapUtil$MappingList",
                "fr.catcore.modremapperapi.remapping.VisitorInfos",
                "fr.catcore.modremapperapi.remapping.VisitorInfos$MethodNamed",
                "fr.catcore.modremapperapi.remapping.VisitorInfos$MethodValue",
                "fr.catcore.modremapperapi.remapping.VisitorInfos$Type",
                "net.fabricmc.loader.impl.launch.FabricLauncher",
                "net.fabricmc.loader.impl.launch.FabricLauncherBase",
                "net.fabricmc.loader.api.ObjectShare",

                getLibClassName("tinyremapper", "AsmClassRemapper"),
                getLibClassName("tinyremapper", "AsmClassRemapper$AsmAnnotationRemapper"),
                getLibClassName("tinyremapper", "AsmClassRemapper$AsmAnnotationRemapper$AsmArrayAttributeAnnotationRemapper"),
                getLibClassName("tinyremapper", "AsmClassRemapper$AsmFieldRemapper"),
                getLibClassName("tinyremapper", "AsmClassRemapper$AsmMethodRemapper"),
                getLibClassName("tinyremapper", "AsmClassRemapper$AsmRecordComponentRemapper"),
                getLibClassName("tinyremapper", "AsmRemapper"),
                getLibClassName("tinyremapper", "BridgeHandler"),
                getLibClassName("tinyremapper", "ClassInstance"),
                getLibClassName("tinyremapper", "FileSystemReference"),
                getLibClassName("tinyremapper", "IMappingProvider"),
                getLibClassName("tinyremapper", "IMappingProvider$MappingAcceptor"),
                getLibClassName("tinyremapper", "IMappingProvider$Member"),
                getLibClassName("tinyremapper", "InputTag"),
                getLibClassName("tinyremapper", "MemberInstance"),
                getLibClassName("tinyremapper", "MetaInfFixer"),
                getLibClassName("tinyremapper", "MetaInfRemover"),
                getLibClassName("tinyremapper", "NonClassCopyMode"),
                getLibClassName("tinyremapper", "OutputConsumerPath"),
                getLibClassName("tinyremapper", "OutputConsumerPath$1"),
                getLibClassName("tinyremapper", "OutputConsumerPath$Builder"),
                getLibClassName("tinyremapper", "OutputConsumerPath$ResourceRemapper"),
                getLibClassName("tinyremapper", "PackageAccessChecker"),
                getLibClassName("tinyremapper", "Propagator"),
                getLibClassName("tinyremapper", "TinyRemapper"),
                getLibClassName("tinyremapper", "TinyRemapper$1"),
                getLibClassName("tinyremapper", "TinyRemapper$1$1"),
                getLibClassName("tinyremapper", "TinyRemapper$2"),
                getLibClassName("tinyremapper", "TinyRemapper$3"),
                getLibClassName("tinyremapper", "TinyRemapper$4"),
                getLibClassName("tinyremapper", "TinyRemapper$5"),
                getLibClassName("tinyremapper", "TinyRemapper$AnalyzeVisitorProvider"),
                getLibClassName("tinyremapper", "TinyRemapper$ApplyVisitorProvider"),
                getLibClassName("tinyremapper", "TinyRemapper$Builder"),
                getLibClassName("tinyremapper", "TinyRemapper$CLIExtensionProvider"),
                getLibClassName("tinyremapper", "TinyRemapper$Direction"),
                getLibClassName("tinyremapper", "TinyRemapper$Extension"),
                getLibClassName("tinyremapper", "TinyRemapper$LinkedMethodPropagation"),
                getLibClassName("tinyremapper", "TinyRemapper$MrjState"),
                getLibClassName("tinyremapper", "TinyRemapper$Propagation"),
                getLibClassName("tinyremapper", "TinyRemapper$StateProcessor"),
                getLibClassName("tinyremapper", "TinyUtils"),
                getLibClassName("tinyremapper", "TinyUtils$MappingAdapter"),
                getLibClassName("tinyremapper", "VisitTrackingClassRemapper"),
                getLibClassName("tinyremapper", "VisitTrackingClassRemapper$VisitKind"),
                getLibClassName("tinyremapper", "extension.mixin.common.IMappable"),
                getLibClassName("tinyremapper", "extension.mixin.common.MapUtility"),
                getLibClassName("tinyremapper", "extension.mixin.common.ResolveUtility"),
                getLibClassName("tinyremapper", "extension.mixin.common.StringUtility"),
                getLibClassName("tinyremapper", "extension.mixin.common.data.Annotation"),
                getLibClassName("tinyremapper", "extension.mixin.common.data.AnnotationElement"),
                getLibClassName("tinyremapper", "extension.mixin.common.data.CommonData"),
                getLibClassName("tinyremapper", "extension.mixin.common.data.Constant"),
                getLibClassName("tinyremapper", "extension.mixin.common.data.Message"),
                getLibClassName("tinyremapper", "extension.mixin.common.data.MxClass"),
                getLibClassName("tinyremapper", "extension.mixin.common.data.MxMember"),
                getLibClassName("tinyremapper", "extension.mixin.common.data.Pair"),
                getLibClassName("tinyremapper", "extension.mixin.soft.SoftTargetMixinClassVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.SoftTargetMixinMethodVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.AccessorAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.InvokerAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.MixinAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.AtAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.AtAnnotationVisitor$AtConstructorMappable"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.AtAnnotationVisitor$AtSecondPassAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.AtAnnotationVisitor$AtSecondPassAnnotationVisitor$1"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.CommonInjectionAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.CommonInjectionAnnotationVisitor$InjectMethodMappable"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.DefinitionAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.DefinitionAnnotationVisitor$MemberRemappingVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.DefinitionsAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.DefinitionsAnnotationVisitor$DefinitionRemappingVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.DescAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.InjectAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.ModifyArgAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.ModifyArgsAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.ModifyConstantAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.ModifyExpressionValueAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.ModifyReceiverAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.ModifyReturnValueAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.ModifyVariableAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.RedirectAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.SliceAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.WrapMethodAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.WrapOperationAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.WrapWithConditionAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.WrapWithConditionV2AnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.data.MemberInfo"),
                getLibClassName("tinyremapper", "extension.mixin.soft.util.NamedMappable"),
                getLibClassName("tinyremapper", "extension.mixin.hard.HardTargetMixinClassVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.hard.HardTargetMixinFieldVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.hard.HardTargetMixinMethodVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.ImplementsAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.ImplementsAnnotationVisitor$1"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.ImplementsAnnotationVisitor$InterfaceAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.ImplementsAnnotationVisitor$SoftImplementsMappable"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.MixinAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.MixinAnnotationVisitor$1"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.MixinAnnotationVisitor$2"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.OverwriteAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.OverwriteAnnotationVisitor$OverwriteMappable"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.ShadowAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.ShadowAnnotationVisitor$ShadowPrefixMappable"),
                getLibClassName("tinyremapper", "extension.mixin.hard.data.SoftInterface"),
                getLibClassName("tinyremapper", "extension.mixin.hard.data.SoftInterface$Remap"),
                getLibClassName("tinyremapper", "extension.mixin.hard.util.CamelPrefixString"),
                getLibClassName("tinyremapper", "extension.mixin.hard.util.ConvertibleMappable"),
                getLibClassName("tinyremapper", "extension.mixin.hard.util.HardTargetMappable"),
                getLibClassName("tinyremapper", "extension.mixin.hard.util.IConvertibleString"),
                getLibClassName("tinyremapper", "extension.mixin.hard.util.IdentityString"),
                getLibClassName("tinyremapper", "extension.mixin.hard.util.PrefixString"),
                getLibClassName("tinyremapper", "extension.mixin.MixinExtension"),
                getLibClassName("tinyremapper", "extension.mixin.MixinExtension$AnalyzeVisitorProvider"),
                getLibClassName("tinyremapper", "extension.mixin.MixinExtension$AnnotationTarget"),
                getLibClassName("tinyremapper", "extension.mixin.MixinExtension$CLIProvider"),
                getLibClassName("tinyremapper", "extension.mixin.MixinExtension$PreApplyVisitorProvider"),
                getLibClassName("tinyremapper", "api.TrClass"),
                getLibClassName("tinyremapper", "api.TrEnvironment"),
                getLibClassName("tinyremapper", "api.TrField"),
                getLibClassName("tinyremapper", "api.TrLogger"),
                getLibClassName("tinyremapper", "api.TrLogger$Level"),
                getLibClassName("tinyremapper", "api.TrMember"),
                getLibClassName("tinyremapper", "api.TrMember$MemberType"),
                getLibClassName("tinyremapper", "api.TrMethod"),
                getLibClassName("tinyremapper", "api.TrRemapper"),

                getLibClassName("mappingio", "MappingReader"),
                getLibClassName("mappingio", "MappingReader$1"),
                getLibClassName("mappingio", "FlatMappingVisitor"),
                getLibClassName("mappingio", "MappedElementKind"),
                getLibClassName("mappingio", "MappingFlag"),
                getLibClassName("mappingio", "MappingUtil"),
                getLibClassName("mappingio", "MappingVisitor"),
                getLibClassName("mappingio", "MappingWriter"),
                getLibClassName("mappingio", "MappingWriter$1")
        }) {
            try {
                Constants.MAIN_LOGGER.debug("Preloading class: " + clazz);
                Class.forName(clazz);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
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
            builder.withMappings(MappingsUtilsImpl.createProvider(tree, MappingsUtilsImpl.getSourceNamespace(), MappingsUtilsImpl.getTargetNamespace()));
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
                Path libPath = CacheUtils.getLibraryPath(library.fileName);

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
