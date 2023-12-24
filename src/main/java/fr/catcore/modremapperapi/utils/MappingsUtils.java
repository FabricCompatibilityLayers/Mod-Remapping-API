package fr.catcore.modremapperapi.utils;

import fr.catcore.modremapperapi.ModRemappingAPI;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.*;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.loader.impl.launch.MappingConfiguration;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.format.tiny.Tiny1FileReader;
import net.fabricmc.mappingio.format.tiny.Tiny2FileReader;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.tinyremapper.*;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipError;

import static fr.catcore.modremapperapi.remapping.RemapUtil.getRemapClasspath;

public class MappingsUtils {
    private static MemoryMappingTree MINECRAFT_MAPPINGS;
    private static boolean initialized = false;

    public static String getNativeNamespace() {
        if (ModRemappingAPI.BABRIC) {
            return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT ? "client" : "server";
        }

        return "official";
    }

    public static String getTargetNamespace() {
        return !FabricLoader.getInstance().isDevelopmentEnvironment() ? "intermediary" : FabricLoader.getInstance().getMappingResolver().getCurrentRuntimeNamespace();
    }
    
    private static void initialize() {
        if (initialized) return;

        URL url = MappingConfiguration.class.getClassLoader().getResource("mappings/mappings.tiny");

        if (url != null) {
            try {
                URLConnection connection = url.openConnection();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    long time = System.currentTimeMillis();
                    MINECRAFT_MAPPINGS = new MemoryMappingTree();

                    // We will only ever need to read tiny here
                    // so to strip the other formats from the included copy of mapping IO, don't use MappingReader.read()
                    reader.mark(4096);
                    final MappingFormat format = MappingReader.detectFormat(reader);
                    reader.reset();

                    switch (format) {
                        case TINY_FILE:
                            Tiny1FileReader.read(reader, MINECRAFT_MAPPINGS);
                            break;
                        case TINY_2_FILE:
                            Tiny2FileReader.read(reader, MINECRAFT_MAPPINGS);
                            break;
                        default:
                            throw new UnsupportedOperationException("Unsupported mapping format: " + format);
                    }

                    Log.debug(LogCategory.MAPPINGS, "Loading mappings took %d ms", System.currentTimeMillis() - time);
                }
            } catch (IOException | ZipError e) {
                throw new RuntimeException("Error reading "+url, e);
            }
        }

        if (MINECRAFT_MAPPINGS == null) {
            Log.info(LogCategory.MAPPINGS, "Mappings not present!");
            MINECRAFT_MAPPINGS = new MemoryMappingTree();
        }

        initialized = true;
    }

    public static MappingTree loadMappings(Reader reader) {
        MemoryMappingTree tree = new MemoryMappingTree();
        try {
            loadMappings(reader, tree);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tree;
    }

    public static void loadMappings(Reader reader, MappingVisitor tree) throws IOException {
        MappingReader.read(reader, tree);
    }

    public static MappingTree getMinecraftMappings() {
        initialize();
        return MINECRAFT_MAPPINGS;
    }

    public static IMappingProvider createProvider(MappingTree mappings) {
        return (acceptor) -> {
            final int fromId = mappings.getNamespaceId(getNativeNamespace());
            final int toId = mappings.getNamespaceId(getTargetNamespace());

            for (MappingTree.ClassMapping classDef : mappings.getClasses()) {
                String className = classDef.getName(fromId);
                String dstName = classDef.getName(toId);

                if (ModRemappingAPI.BABRIC && className == null) {
                    if (dstName == null) continue;
                    className = dstName;
                }

                if (dstName == null) {
                    dstName = className;
                }

                acceptor.acceptClass(className, dstName);

                for (MappingTree.FieldMapping field : classDef.getFields()) {
                    String fieldName = field.getName(fromId);
                    String dstFieldName = field.getName(toId);
                    String fieldDesc = field.getDesc(fromId);
                    String dstFieldDesc = field.getDesc(toId);

                    if (ModRemappingAPI.BABRIC && fieldName == null) {
                        if (dstFieldName == null) continue;
                        fieldName = dstFieldName;
                    }

                    if (ModRemappingAPI.BABRIC && fieldDesc == null) {
                        if (dstFieldDesc == null) continue;
                        fieldDesc = dstFieldDesc;
                    }

                    acceptor.acceptField(memberOf(className, fieldName, fieldDesc), dstFieldName);
                }

                for (MappingTree.MethodMapping method : classDef.getMethods()) {
                    String methodName = method.getName(fromId);
                    String dstMethodName = method.getName(toId);
                    String methodDesc = method.getDesc(fromId);
                    String dstMethodDesc = method.getDesc(toId);

                    if (ModRemappingAPI.BABRIC && methodName == null) {
                        if (dstMethodName == null) continue;
                        methodName = dstMethodName;
                    }

                    if (ModRemappingAPI.BABRIC && methodDesc == null) {
                        if (dstMethodDesc == null) continue;
                        methodDesc = dstMethodDesc;
                    }

                    IMappingProvider.Member methodIdentifier = memberOf(className, methodName, methodDesc);
                    acceptor.acceptMethod(methodIdentifier, dstMethodName);
                }
            }
        };
    }

    private static IMappingProvider createBackwardProvider(MappingTree mappings) {
        return (acceptor) -> {
            final int fromId = mappings.getNamespaceId(getTargetNamespace());
            final int toId = mappings.getNamespaceId(getNativeNamespace());

            for (MappingTree.ClassMapping classDef : mappings.getClasses()) {
                String className = classDef.getName(fromId);
                String dstName = classDef.getName(toId);

                if (ModRemappingAPI.BABRIC && dstName == null) {
                    if (className == null) continue;
                    dstName = className;
                }

                if (className == null) {
                    className = dstName;
                }

                acceptor.acceptClass(className, dstName);

                for (MappingTree.FieldMapping field : classDef.getFields()) {
                    String fieldName = field.getName(fromId);
                    String dstFieldName = field.getName(toId);
                    String fieldDesc = field.getDesc(fromId);
                    String dstFieldDesc = field.getDesc(toId);

                    if (ModRemappingAPI.BABRIC && dstFieldName == null) {
                        if (fieldName == null) continue;
                        dstFieldName = fieldName;
                    }

                    if (ModRemappingAPI.BABRIC && dstFieldDesc == null) {
                        if (fieldDesc == null) continue;
                        dstFieldDesc = fieldDesc;
                    }

                    acceptor.acceptField(memberOf(className, fieldName, fieldDesc), dstFieldName);
                }

                for (MappingTree.MethodMapping method : classDef.getMethods()) {
                    String methodName = method.getName(fromId);
                    String dstMethodName = method.getName(toId);
                    String methodDesc = method.getDesc(fromId);
                    String dstMethodDesc = method.getDesc(toId);

                    if (ModRemappingAPI.BABRIC && dstMethodName == null) {
                        if (methodName == null) continue;
                        dstMethodName = methodName;
                    }

                    if (ModRemappingAPI.BABRIC && dstMethodDesc == null) {
                        if (methodDesc == null) continue;
                        dstMethodDesc = methodDesc;
                    }

                    IMappingProvider.Member methodIdentifier = memberOf(className, methodName, methodDesc);
                    acceptor.acceptMethod(methodIdentifier, dstMethodName);
                }
            }
        };
    }

    private static IMappingProvider.Member memberOf(String className, String memberName, String descriptor) {
        return new IMappingProvider.Member(className, memberName, descriptor);
    }

    private static Path[] getMinecraftJar() throws IOException {
        Path[] originalClassPath = getRemapClasspath().toArray(new Path[0]);

        Map<Path, Path> paths = new HashMap<>();

        for (Path path :
                originalClassPath) {
            Constants.MAIN_LOGGER.info(path.toString());
            paths.put(path, new File(Constants.LIB_FOLDER, path.toFile().getName()).toPath());
            paths.get(path).toFile().delete();
        }

        TinyRemapper.Builder builder = TinyRemapper
                .newRemapper()
                .renameInvalidLocals(true)
                .ignoreFieldDesc(false)
                .propagatePrivate(true)
                .ignoreConflicts(true)
                .fixPackageAccess(true)
                .withMappings(createBackwardProvider(getMinecraftMappings()));

        TinyRemapper remapper = builder.build();

        Constants.MAIN_LOGGER.info("Remapping minecraft jar back to obfuscated!");

        List<OutputConsumerPath> outputConsumerPaths = new ArrayList<>();

        List<OutputConsumerPath.ResourceRemapper> resourceRemappers = new ArrayList<>(NonClassCopyMode.FIX_META_INF.remappers);

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

        return paths.values().toArray(new Path[0]);
    }

    public static void addMinecraftJar(TinyRemapper remapper) {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            try {
                remapper.readClassPathAsync(getMinecraftJar());
            } catch (IOException e) {
                throw new RuntimeException("Failed to populate default remap classpath", e);
            }
        } else {
            ObjectShare share = FabricLoader.getInstance().getObjectShare();
            Object inputs = share.get("fabric-loader:inputGameJars");
            List<Path> list = new ArrayList<>();

            Object oldJar = FabricLoader.getInstance().getObjectShare().get("fabric-loader:inputGameJar");

            List<Path> classPaths = FabricLauncherBase.getLauncher().getClassPath();

            if (inputs instanceof List) {
                List<Path> paths = (List<Path>) inputs;

                if (oldJar instanceof Path) {
                    if (paths.get(0).toString().equals(oldJar.toString())) {
                        list.addAll(paths);
                    } else {
                        list.add((Path) oldJar);
                    }
                } else {
                    list.addAll(paths);
                }
            } else {
                list.add((Path) oldJar);
            }

            list.addAll(classPaths);

            Object realmsJar = share.get("fabric-loader:inputRealmsJar");

            if (realmsJar instanceof Path) list.add((Path) realmsJar);

            for (Path path : list) {
                Constants.MAIN_LOGGER.debug("Appending '%s' to remapper classpath", path);
                remapper.readClassPathAsync(path);
            }
        }
    }
}
