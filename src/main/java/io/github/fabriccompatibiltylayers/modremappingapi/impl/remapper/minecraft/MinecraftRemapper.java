package io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.minecraft;

import fr.catcore.modremapperapi.utils.Constants;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.RemapUtils;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings.MappingTreeHelper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings.MappingsRegistry;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.TrRemapperHelper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.CacheUtils;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.FileUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@ApiStatus.Internal
public class MinecraftRemapper {
    private static Collection<Path> getMinecraftJar(Collection<Path> sourcePaths, String src, String target, MappingsRegistry mappingsRegistry) throws IOException {
        Path targetFolder = CacheUtils.getLibraryPath(target);

        if (!Files.exists(targetFolder)) {
            Files.createDirectories(targetFolder);
        }

        Map<Path, Path> paths = CacheUtils.computeLibraryPaths(new HashSet<>(sourcePaths), target);

        if (FileUtils.exist(paths.values())) return paths.values();

        FileUtils.delete(paths.values());

        TinyRemapper.Builder builder = TinyRemapper
                .newRemapper()
                .renameInvalidLocals(true)
                .ignoreFieldDesc(false)
                .propagatePrivate(true)
                .ignoreConflicts(true)
                .fixPackageAccess(true)
                .withMappings(
                        MappingTreeHelper.createMappingProvider(
                                mappingsRegistry.getFormattedMappings(),
                                src, target)
                );

        TinyRemapper remapper = builder.build();

        Constants.MAIN_LOGGER.info("Remapping minecraft jar from " + src + " to " + target + "!");

        List<OutputConsumerPath> outputConsumerPaths = new ArrayList<>();

        List<OutputConsumerPath.ResourceRemapper> resourceRemappers = new ArrayList<>(NonClassCopyMode.FIX_META_INF.remappers);

        TrRemapperHelper.applyRemapper(remapper, paths, outputConsumerPaths, resourceRemappers, true, src, target);

        Constants.MAIN_LOGGER.info("MC jar remapped successfully!");

        return paths.values();
    }

    @ApiStatus.Internal
    public static void addMinecraftJar(TinyRemapper remapper, MappingsRegistry mappingsRegistry) throws IOException {
        Collection<Path> classPath;

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            try {
                classPath = getMinecraftJar(
                            getMinecraftJar(RemapUtils.getRemapClasspath(), mappingsRegistry.getTargetNamespace(), "intermediary", mappingsRegistry),
                        "intermediary",
                        "official",
                        mappingsRegistry
                );

                if (!mappingsRegistry.isSourceNamespaceObf()) {
                    classPath = getMinecraftJar(classPath, "official", mappingsRegistry.getSourceNamespace(), mappingsRegistry);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to populate default remap classpath", e);
            }
        } else {
            classPath = RemapUtils.getClassPathFromObjectShare();

            if (!mappingsRegistry.isSourceNamespaceObf()) {
                classPath = getMinecraftJar(classPath, "official", mappingsRegistry.getSourceNamespace(), mappingsRegistry);
            }
        }

        for (Path path : classPath) {
            Constants.MAIN_LOGGER.debug("Appending '%s' to remapper classpath", path);
            remapper.readClassPathAsync(path);
        }
    }
}
