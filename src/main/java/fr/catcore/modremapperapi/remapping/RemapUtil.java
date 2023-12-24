package fr.catcore.modremapperapi.remapping;

import fr.catcore.modremapperapi.ModRemappingAPI;
import fr.catcore.modremapperapi.api.ModRemapper;
import fr.catcore.modremapperapi.api.RemapLibrary;
import fr.catcore.modremapperapi.utils.Constants;
import fr.catcore.modremapperapi.utils.FileUtils;
import fr.catcore.modremapperapi.utils.MappingsUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.tinyremapper.*;
import net.fabricmc.tinyremapper.extension.mixin.MixinExtension;
import org.objectweb.asm.*;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static fr.catcore.modremapperapi.utils.MappingsUtils.getNativeNamespace;
import static fr.catcore.modremapperapi.utils.MappingsUtils.getTargetNamespace;

public class RemapUtil {
    private static MappingTree LOADER_TREE;
    private static MappingTree MINECRAFT_TREE;
    private static MappingTree MODS_TREE;

    private static final Map<String, String> MOD_MAPPINGS = new HashMap<>();

    protected static final Map<String, List<String>> MIXINED = new HashMap<>();

    private static String defaultPackage = "";

    public static final List<String> MC_CLASS_NAMES = new ArrayList<>();

    public static void init() {
        downloadRemappingLibs();
        generateMappings();

        for (ModRemapper remapper : ModRemappingAPI.MOD_REMAPPERS) {
            Optional<String> pkg = remapper.getDefaultPackage();

            if (pkg.isPresent()) {
                defaultPackage = pkg.get();
                break;
            }
        }

        LOADER_TREE = makeTree(Constants.EXTRA_MAPPINGS_FILE);
        MINECRAFT_TREE = MappingsUtils.getMinecraftMappings();

        for (MappingTree.ClassMapping classView : MINECRAFT_TREE.getClasses()) {
            String className = classView.getName(getNativeNamespace());

            if (className != null) {
                MC_CLASS_NAMES.add(className);
            }
        }
    }

    private static void downloadRemappingLibs() {
        try {
            for (ModRemapper remapper : ModRemappingAPI.MOD_REMAPPERS) {
                for (RemapLibrary library : remapper.getRemapLibraries()) {
                    File libPath = new File(Constants.LIB_FOLDER, library.fileName);

                    if (!libPath.exists() && !library.url.isEmpty()) {
                        Constants.MAIN_LOGGER.info("Downloading remapping library '" + library.fileName + "' from url '" + library.url + "'");
                        try (BufferedInputStream inputStream = new BufferedInputStream(new URL(library.url).openStream())) {
                            try (BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(libPath.toPath()))) {
                                byte[] buffer = new byte[2048];

                                // Increments file size
                                int length;
                                int downloaded = 0;

                                // Looping until server finishes
                                while ((length = inputStream.read(buffer)) != -1) {
                                    // Writing data
                                    outputStream.write(buffer, 0, length);
                                    downloaded += length;
//                                    Constants.MAIN_LOGGER.debug("Download Status: " + (downloaded * 100) / (contentLength * 1.0) + "%");
                                }

                                outputStream.close();
                                inputStream.close();
                            }
                        }

                        FileUtils.excludeFromZipFile(libPath, library.toExclude);
                        Constants.MAIN_LOGGER.info("Remapping library ready for use.");
                    } else if (!libPath.exists() && library.path != null) {
                        Constants.MAIN_LOGGER.info("Extracting remapping library '" + library.fileName + "' from mod jar.");
                        FileUtils.copyFile(library.path, libPath.toPath());
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
                if (!MC_CLASS_NAMES.contains(clName) || ModRemappingAPI.remapClassEdits) classes.add(clName);
            }
        }

        classes.forEach(cl -> MOD_MAPPINGS.put(cl, (cl.contains("/") ? "" : defaultPackage) + cl));

        return files;
    }

    public static void generateModMappings() {
        if (Constants.REMAPPED_MAPPINGS_FILE.exists()) {
            Constants.REMAPPED_MAPPINGS_FILE.delete();
        }

        MappingList mappings = new MappingList();
        MOD_MAPPINGS.forEach(mappings::add);

        List<String> lines = new ArrayList<>();

        if (ModRemappingAPI.BABRIC) {
            lines.add(toString("v1", "intermediary", "glue", "server", "client", "named"));
        } else {
            lines.add(toString("v1", "official", "intermediary", "named"));
        }

        mappings.forEach(mappingBuilder -> lines.addAll(mappingBuilder.build()));

        FileUtils.writeTextFile(lines, Constants.REMAPPED_MAPPINGS_FILE);

        MODS_TREE = makeTree(Constants.REMAPPED_MAPPINGS_FILE);
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

    public static class MappingList extends ArrayList<MappingBuilder> {
        public MappingList() {
            super();
        }

        public MappingBuilder add(String obfuscated, String intermediary) {
            MappingBuilder builder = MappingBuilder.create(obfuscated, intermediary);
            this.add(builder);
            return builder;
        }

        public MappingBuilder add(String name) {
            MappingBuilder builder = MappingBuilder.create(name);
            this.add(builder);
            return builder;
        }
    }

    private static void generateMappings() {
        if (Constants.EXTRA_MAPPINGS_FILE.exists()) {
            Constants.EXTRA_MAPPINGS_FILE.delete();
        }

        List<String> lines = new ArrayList<>();

        if (ModRemappingAPI.BABRIC) {
            lines.add(toString("v1", "intermediary", "glue", "server", "client", "named"));
        } else {
            lines.add(toString("v1", "official", "intermediary", "named"));
        }

        MappingList mappingList = new MappingList();

        for (ModRemapper remapper : ModRemappingAPI.MOD_REMAPPERS) {
            remapper.getMappingList(mappingList);
        }

        mappingList.forEach(mappingBuilder -> lines.addAll(mappingBuilder.build()));

        FileUtils.writeTextFile(lines, Constants.EXTRA_MAPPINGS_FILE);
    }

    /**
     * Will convert array to mapping-like string (with tab separator).
     *
     * @param line array of {@link String} that represents mappings line.
     */
    private static String toString(String... line) {
        StringBuilder builder = new StringBuilder(line[0]);
        for (int j = 1; j < line.length; j++) {
            builder.append('\t');
            builder.append(line[j]);
        }
        return builder.toString();
    }

    /**
     * Will make tree for specified mappings file.
     *
     * @param file mappings {@link File} in tiny format.
     */
    private static MappingTree makeTree(File file) {
        MemoryMappingTree tree = new MemoryMappingTree();
        try {
            FileReader reader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(reader);
            MappingsUtils.loadMappings(bufferedReader, tree);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tree;
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
                "fr.catcore.modremapperapi.utils.DefaultModEntry",
                "fr.catcore.modremapperapi.utils.DefaultModRemapper",
                "fr.catcore.modremapperapi.utils.FakeModManager",
                "fr.catcore.modremapperapi.utils.FileUtils",
                "fr.catcore.modremapperapi.utils.MappingsUtils",
                "fr.catcore.modremapperapi.utils.MappingsUtils$1",
                "fr.catcore.modremapperapi.utils.MixinUtils",
                "fr.catcore.modremapperapi.utils.ModDiscoverer",
                "fr.catcore.modremapperapi.utils.ModDiscoverer$1",
                "fr.catcore.modremapperapi.utils.ModDiscoverer$2",
                "fr.catcore.modremapperapi.utils.ModEntry",
                "fr.catcore.modremapperapi.utils.RefmapJson",
                "fr.catcore.modremapperapi.remapping.MapEntryType",
                "fr.catcore.modremapperapi.remapping.MappingBuilder",
                "fr.catcore.modremapperapi.remapping.MappingBuilder$Entry",
                "fr.catcore.modremapperapi.remapping.MappingBuilder$Type",
                "fr.catcore.modremapperapi.remapping.MixinPostApplyVisitor",
                "fr.catcore.modremapperapi.remapping.MRAClassVisitor",
                "fr.catcore.modremapperapi.remapping.MRAMethodVisitor",
                "fr.catcore.modremapperapi.remapping.MRAPostApplyVisitor",
                "fr.catcore.modremapperapi.remapping.RefmapRemapper",
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
                getLibClassName("tinyremapper", "FileSystemHandler"),
                getLibClassName("tinyremapper", "FileSystemHandler$RefData"),
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
                getLibClassName("tinyremapper", "TinyRemapper$Direction"),
                getLibClassName("tinyremapper", "TinyRemapper$Extension"),
                getLibClassName("tinyremapper", "TinyRemapper$LinkedMethodPropagation"),
                getLibClassName("tinyremapper", "TinyRemapper$MrjState"),
                getLibClassName("tinyremapper", "TinyRemapper$Propagation"),
                getLibClassName("tinyremapper", "TinyRemapper$StateProcessor"),
                getLibClassName("tinyremapper", "TinyUtils"),
                getLibClassName("tinyremapper", "TinyUtils$1"),
                getLibClassName("tinyremapper", "TinyUtils$MemberMapping"),
                getLibClassName("tinyremapper", "TinyUtils$MethodArgMapping"),
                getLibClassName("tinyremapper", "TinyUtils$MethodVarMapping"),
                getLibClassName("tinyremapper", "TinyUtils$SimpleClassMapper"),
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
                getLibClassName("tinyremapper", "extension.mixin.common.Logger"),
                getLibClassName("tinyremapper", "extension.mixin.common.Logger$Level"),
                getLibClassName("tinyremapper", "extension.mixin.soft.SoftTargetMixinClassVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.SoftTargetMixinMethodVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.AccessorAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.AccessorAnnotationVisitor$AccessorSecondPassAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.FirstPassAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.InvokerAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.InvokerAnnotationVisitor$InvokerSecondPassAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.MixinAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.MixinAnnotationVisitor$MixinSecondPassAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.MixinAnnotationVisitor$MixinSecondPassAnnotationVisitor$1"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.MixinAnnotationVisitor$MixinSecondPassAnnotationVisitor$2"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.AtAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.AtAnnotationVisitor$AtConstructorMappable"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.AtAnnotationVisitor$AtMethodMappable"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.AtAnnotationVisitor$AtSecondPassAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.AtAnnotationVisitor$AtSecondPassAnnotationVisitor$1"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.CommonInjectionAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.CommonInjectionAnnotationVisitor$CommonInjectionSecondPassAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.CommonInjectionAnnotationVisitor$CommonInjectionSecondPassAnnotationVisitor$1"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.CommonInjectionAnnotationVisitor$CommonInjectionSecondPassAnnotationVisitor$2"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.CommonInjectionAnnotationVisitor$CommonInjectionSecondPassAnnotationVisitor$3"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.CommonInjectionAnnotationVisitor$InjectMethodMappable"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.InjectAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.ModifyArgAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.ModifyArgsAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.ModifyConstantAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.ModifyVariableAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.RedirectAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.SliceAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.data.MemberInfo"),
                getLibClassName("tinyremapper", "extension.mixin.soft.util.NamedMappable"),
                getLibClassName("tinyremapper", "extension.mixin.hard.HardTargetMixinClassVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.hard.HardTargetMixinFieldVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.hard.HardTargetMixinMethodVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.AccessorAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.AccessorAnnotationVisitor$AccessorMappable"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.ImplementsAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.ImplementsAnnotationVisitor$1"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.ImplementsAnnotationVisitor$InterfaceAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.ImplementsAnnotationVisitor$SoftImplementsMappable"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.InvokerAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.InvokerAnnotationVisitor$InvokerMappable"),
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
                getLibClassName("tinyremapper", "extension.mixin.MixinExtension$AnnotationTarget"),
                getLibClassName("tinyremapper", "api.TrClass"),
                getLibClassName("tinyremapper", "api.TrEnvironment"),
                getLibClassName("tinyremapper", "api.TrField"),
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
            builder.withMappings(MappingsUtils.createProvider(tree));
        }

        MRAPostApplyVisitor applyVisitor = new MRAPostApplyVisitor();
        MixinPostApplyVisitor mixinPostApplyVisitor = new MixinPostApplyVisitor(trees);

        VisitorInfos infos = new VisitorInfos();

        for (ModRemapper modRemapper : ModRemappingAPI.MOD_REMAPPERS) {
            modRemapper.registerVisitors(infos);
        }

        applyVisitor.setInfos(infos);

        builder.extraPostApplyVisitor(applyVisitor);
        builder.extraPostApplyVisitor(mixinPostApplyVisitor);

        builder.extension(new MixinExtension(EnumSet.of(MixinExtension.AnnotationTarget.HARD)));

        TinyRemapper remapper = builder.build();

        MappingsUtils.addMinecraftJar(remapper);

        for (ModRemapper modRemapper : ModRemappingAPI.MOD_REMAPPERS) {
            for (RemapLibrary library : modRemapper.getRemapLibraries()) {
                File libPath = new File(Constants.LIB_FOLDER, library.fileName);
                if (libPath.exists()) {
                    remapper.readClassPathAsync(libPath.toPath());
                } else {
                    Constants.MAIN_LOGGER.info("Library " + libPath.toPath() + " does not exist.");
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

    public static String getRemappedFieldName(Class<?> owner, String fieldName) {
        int target = MINECRAFT_TREE.getNamespaceId(getTargetNamespace());
        MappingTree.ClassMapping classMapping = MINECRAFT_TREE.getClass(owner.getName().replace(".", "/"), target);

        if (classMapping != null) {
            for (MappingTree.FieldMapping fieldDef : classMapping.getFields()) {
                String fieldSubName = fieldDef.getName(getNativeNamespace());
                if (!(ModRemappingAPI.BABRIC && fieldSubName == null) && Objects.equals(fieldSubName, fieldName)) {
                    return fieldDef.getName(getTargetNamespace());
                }
            }
        }

        if (owner.getSuperclass() != null) {
            fieldName = getRemappedFieldName(owner.getSuperclass(), fieldName);
        }

        return fieldName;
    }

    public static String getRemappedMethodName(Class<?> owner, String methodName, Class<?>[] parameterTypes) {
        String argDesc = classTypeToDescriptor(parameterTypes);

        int target = MINECRAFT_TREE.getNamespaceId(getTargetNamespace());
        MappingTree.ClassMapping classMapping = MINECRAFT_TREE.getClass(owner.getName().replace(".", "/"), target);

        if (classMapping != null) {
            for (MappingTree.MethodMapping methodDef : classMapping.getMethods()) {
                String methodSubName = methodDef.getName(getNativeNamespace());
                if (!(ModRemappingAPI.BABRIC && methodSubName == null) && Objects.equals(methodSubName, methodName)) {
                    String methodDescriptor = methodDef.getDesc(getTargetNamespace());

                    if (methodDescriptor.startsWith(argDesc)) {
                        return methodDef.getName(getTargetNamespace());
                    }
                }
            }
        }

        if (owner.getSuperclass() != null) {
            methodName = getRemappedMethodName(owner.getSuperclass(), methodName, parameterTypes);
        }

        return methodName;
    }

    private static String classTypeToDescriptor(Class<?>[] classTypes) {
        StringBuilder desc = new StringBuilder("(");

        for (Class<?> clas : classTypes) {
            desc.append(Type.getDescriptor(clas));
        }

        return desc + ")";
    }

    /**
     * A shortcut to the Fabric Environment getter.
     */
    public static EnvType getEnvironment() {
        return FabricLoader.getInstance().getEnvironmentType();
    }

    public static List<Path> getRemapClasspath() throws IOException {
        String remapClasspathFile = System.getProperty("fabric.remapClasspathFile");

        if (remapClasspathFile == null) {
            throw new RuntimeException("No remapClasspathFile provided");
        }

        String content = new String(Files.readAllBytes(Paths.get(remapClasspathFile)), StandardCharsets.UTF_8);

        return Arrays.stream(content.split(File.pathSeparator))
                .map(Paths::get)
                .collect(Collectors.toList());
    }

    @Deprecated
    public static String getNativeNamespace() {
        return MappingsUtils.getNativeNamespace();
    }
}
