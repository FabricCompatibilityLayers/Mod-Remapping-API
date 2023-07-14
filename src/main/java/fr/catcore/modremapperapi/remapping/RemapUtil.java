package fr.catcore.modremapperapi.remapping;

import fr.catcore.modremapperapi.ModRemappingAPI;
import fr.catcore.modremapperapi.api.ModRemapper;
import fr.catcore.modremapperapi.api.RemapLibrary;
import fr.catcore.modremapperapi.utils.Constants;
import fr.catcore.modremapperapi.utils.FileUtils;
import fr.catcore.modremapperapi.utils.MappingsUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
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
                                    //System.out.println("Downlad Status: " + (downloaded * 100) / (contentLength * 1.0) + "%");
                                }

                                outputStream.close();
                                inputStream.close();
                            }
                        }

                        FileUtils.excludeFromZipFile(libPath, library.toExclude);
                    } else if (!libPath.exists() && library.path != null) {
                        FileUtils.copyFile(library.path, libPath.toPath());
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void remapMods(Map<Path, Path> pathMap) {
        Constants.MAIN_LOGGER.debug("Starting jar remapping!");
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
            lines.add(toString("v1", "intermediary", "glue", "server", "client"));
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
            lines.add(toString("v1", "intermediary", "glue", "server", "client"));
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

        try {
            Class.forName("fr.catcore.modremapperapi.remapping.MRAMethodVisitor");
            Class.forName("fr.catcore.modremapperapi.remapping.VisitorInfos$Type");
            Class.forName("fr.catcore.modremapperapi.remapping.VisitorInfos$MethodValue");
            Class.forName("fr.catcore.modremapperapi.remapping.VisitorInfos$MethodNamed");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        for (ModRemapper modRemapper : ModRemappingAPI.MOD_REMAPPERS) {
            modRemapper.registerVisitors(infos);
        }

        applyVisitor.setInfos(infos);

        builder.extraPostApplyVisitor(applyVisitor);
        builder.extraPostApplyVisitor(mixinPostApplyVisitor);

        builder.extension(new MixinExtension(EnumSet.of(MixinExtension.AnnotationTarget.HARD)));

        TinyRemapper remapper = builder.build();

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            try {
                remapper.readClassPathAsync(getRemapClasspath().toArray(new Path[0]));
            } catch (IOException e) {
                throw new RuntimeException("Failed to populate default remap classpath", e);
            }
        } else {
            remapper.readClassPathAsync((Path) FabricLoader.getInstance().getObjectShare().get("fabric-loader:inputGameJar"));

            for (Path path : FabricLauncherBase.getLauncher().getClassPath()) {
                Constants.MAIN_LOGGER.debug("Appending '%s' to remapper classpath", path);
                remapper.readClassPathAsync(path);
            }
        }

        for (ModRemapper modRemapper : ModRemappingAPI.MOD_REMAPPERS) {
            for (RemapLibrary library : modRemapper.getRemapLibraries()) {
                File libPath = new File(Constants.LIB_FOLDER, library.fileName);
                if (libPath.exists()) {
                    remapper.readClassPathAsync(libPath.toPath());
                } else {
                    Constants.MAIN_LOGGER.error("Library " + libPath.toPath() + " does not exist.");
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

    private static List<Path> getRemapClasspath() throws IOException {
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
