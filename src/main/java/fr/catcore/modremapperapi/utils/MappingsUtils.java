package fr.catcore.modremapperapi.utils;

import fr.catcore.modremapperapi.ModRemappingAPI;
import fr.catcore.modremapperapi.remapping.RemapUtil;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.MappingsUtilsImpl;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.*;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.tinyremapper.*;
import org.jetbrains.annotations.ApiStatus;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

import static fr.catcore.modremapperapi.remapping.RemapUtil.getRemapClasspath;

public class MappingsUtils {
    @Deprecated
    public static String getNativeNamespace() {
        if (ModRemappingAPI.BABRIC) {
            return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT ? "client" : "server";
        }

        return "official";
    }

    public static String getTargetNamespace() {
        return FabricLoader.getInstance().getMappingResolver().getCurrentRuntimeNamespace();
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

    /**
     * @deprecated Use {@link MappingsUtilsImpl#getVanillaMappings()} instead for the same behavior.
     */
    @Deprecated
    public static MappingTree getMinecraftMappings() {
        return MappingsUtilsImpl.getVanillaMappings();
    }

    @Deprecated
    public static IMappingProvider createProvider(MappingTree mappings) {
        return MappingsUtilsImpl.createProvider(mappings, getNativeNamespace(), getTargetNamespace());
    }

    private static Path[] getMinecraftJar(List<Path> sourcePaths, String src, String target) throws IOException {
        Path[] originalClassPath = sourcePaths.toArray(new Path[0]);

        Map<Path, Path> paths = new HashMap<>();

        for (Path path :
                originalClassPath) {
            Constants.MAIN_LOGGER.debug(path.toString());
            paths.put(path, new File(
                    new File(Constants.LIB_FOLDER, target),
                    path.toFile().getName()).toPath()
            );
            paths.get(path).toFile().delete();
        }

        TinyRemapper.Builder builder = TinyRemapper
                .newRemapper()
                .renameInvalidLocals(true)
                .ignoreFieldDesc(false)
                .propagatePrivate(true)
                .ignoreConflicts(true)
                .fixPackageAccess(true)
                .withMappings(
                        MappingsUtilsImpl.createProvider(MappingsUtilsImpl.getMinecraftMappings(), src, target)
                );

        TinyRemapper remapper = builder.build();

        Constants.MAIN_LOGGER.info("Remapping minecraft jar from " + src + " to " + target + "!");

        List<OutputConsumerPath> outputConsumerPaths = new ArrayList<>();

        List<OutputConsumerPath.ResourceRemapper> resourceRemappers = new ArrayList<>(NonClassCopyMode.FIX_META_INF.remappers);

        RemapUtil.applyRemapper(remapper, paths, outputConsumerPaths, resourceRemappers, true, src, target);

        Constants.MAIN_LOGGER.info("MC jar remapped successfully!");

        return paths.values().toArray(new Path[0]);
    }

    @ApiStatus.Internal
    public static void addMinecraftJar(TinyRemapper remapper) throws IOException {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            try {
                Path[] classPath = getMinecraftJar(
                        Arrays.asList(
                                getMinecraftJar(
                                        getRemapClasspath(),
                                        getTargetNamespace(),
                                        "intermediary"
                                )
                        ),
                        "intermediary",
                        "official"
                );

                remapper.readClassPathAsync(
                        !Objects.equals(MappingsUtilsImpl.getSourceNamespace(), "official") ?
                            getMinecraftJar(
                                    Arrays.asList(
                                            classPath
                                    ),
                                    "official",
                                    MappingsUtilsImpl.getSourceNamespace()
                            ) : classPath
                );
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

            for (Path path :
                    !Objects.equals(MappingsUtilsImpl.getSourceNamespace(), "official") ?
                            getMinecraftJar(list, "official", MappingsUtilsImpl.getSourceNamespace())
                            : list.toArray(new Path[0])) {
                Constants.MAIN_LOGGER.debug("Appending '%s' to remapper classpath", path);
                remapper.readClassPathAsync(path);
            }
        }
    }
}
