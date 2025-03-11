package io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.minecraft;

import fr.catcore.modremapperapi.remapping.RemapUtil;
import fr.catcore.modremapperapi.utils.Constants;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.MappingsUtilsImpl;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.RemapUtils;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.CacheUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@ApiStatus.Internal
public class MinecraftRemapper {
    private static Collection<Path> getMinecraftJar(Collection<Path> sourcePaths, String src, String target) throws IOException {
        Path targetFolder = CacheUtils.getLibraryPath(target);

        if (!Files.exists(targetFolder)) {
            Files.createDirectories(targetFolder);
        }

        Map<Path, Path> paths = computeLibraryPaths(sourcePaths, target);

        if (exist(paths.values())) return paths.values();

        delete(paths.values());

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

        return paths.values();
    }

    @ApiStatus.Internal
    public static Map<Path, Path> computeLibraryPaths(Collection<Path> sourcePaths, String target) {
        return sourcePaths.stream().collect(Collectors.toMap(p -> p,
                p -> CacheUtils.getLibraryPath(target).resolve(p.toFile().getName())));
    }

    @ApiStatus.Internal
    public static boolean exist(Collection<Path> paths) {
        for (Path path : paths) {
            if (!Files.exists(path)) return false;
        }

        return true;
    }

    @ApiStatus.Internal
    public static void delete(Collection<Path> paths) {
        for (Path path : paths) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @ApiStatus.Internal
    public static void addMinecraftJar(TinyRemapper remapper) throws IOException {
        Collection<Path> classPath;

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            try {
                classPath = getMinecraftJar(
                            getMinecraftJar(RemapUtils.getRemapClasspath(), MappingsUtilsImpl.getTargetNamespace(), "intermediary"),
                        "intermediary",
                        "official"
                );

                if (!MappingsUtilsImpl.isSourceNamespaceObf()) {
                    classPath = getMinecraftJar(classPath, "official", MappingsUtilsImpl.getSourceNamespace());
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to populate default remap classpath", e);
            }
        } else {
            classPath = RemapUtils.getClassPathFromObjectShare();

            if (!MappingsUtilsImpl.isSourceNamespaceObf()) {
                classPath = getMinecraftJar(classPath, "official", MappingsUtilsImpl.getSourceNamespace());
            }
        }

        for (Path path : classPath) {
            Constants.MAIN_LOGGER.debug("Appending '%s' to remapper classpath", path);
            remapper.readClassPathAsync(path);
        }
    }
}
