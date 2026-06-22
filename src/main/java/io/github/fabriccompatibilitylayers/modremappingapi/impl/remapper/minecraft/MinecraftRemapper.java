package io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper.minecraft;

import fr.catcore.modremapperapi.utils.Constants;
import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.CacheHandler;
import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.RemapLibrary;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.context.LibraryHandler;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.mappings.MappingTreeHelper;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.mappings.MappingsRegistry;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper.TrRemapperHelper;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.utils.FileUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.fabriccompatibilitylayers.modremappingapi.impl.utils.FileUtils.getJarFileSystem;

@ApiStatus.Internal
public class MinecraftRemapper {
    private static Map<Path, Path> computeLibraryPaths(Collection<Path> sourcePaths, Path basePath) {
        return sourcePaths.stream().collect(Collectors.toMap(
                path -> path,
                path -> basePath.resolve(path.getFileName())
        ));
    }

    private static Collection<Path> getMinecraftJar(Collection<Path> sourcePaths, String src, String target, MappingsRegistry mappingsRegistry, CacheHandler cacheHandler) throws IOException {
        var targetFolder = cacheHandler.resolveLibrary(target);

        if (Files.notExists(targetFolder)) {
            Files.createDirectories(targetFolder);
        }

        var paths = computeLibraryPaths(new HashSet<>(sourcePaths), targetFolder);

        if (FileUtils.exist(paths.values())) return paths.values();

        FileUtils.delete(paths.values());

        var builder = TinyRemapper
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

        var remapper = builder.build();

        Constants.MAIN_LOGGER.info("Remapping minecraft jar from %s to %s!", src, target);

        var outputConsumerPaths = new ArrayList<OutputConsumerPath>();
        var resourceRemappers = new ArrayList<>(NonClassCopyMode.FIX_META_INF.remappers);

        TrRemapperHelper.applyRemapper(remapper, paths, outputConsumerPaths, resourceRemappers, true, src, target);

        Constants.MAIN_LOGGER.info("MC jar remapped successfully!");

        return paths.values();
    }

    @ApiStatus.Internal
    public static void addMinecraftJar(TinyRemapper remapper, MappingsRegistry mappingsRegistry, CacheHandler cacheHandler, LibraryHandler libraryHandler) throws IOException {
        Collection<Path> classPath;

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            try {
                classPath = getMinecraftJar(
                        getMinecraftJar(ClasspathUtils.getRemapClasspath(), mappingsRegistry.getTargetNamespace(), "intermediary", mappingsRegistry, cacheHandler),
                        "intermediary",
                        "official",
                        mappingsRegistry,
                        cacheHandler
                );

                if (!mappingsRegistry.isSourceNamespaceObf()) {
                    classPath = getMinecraftJar(classPath, "official", mappingsRegistry.getSourceNamespace(), mappingsRegistry, cacheHandler);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to populate default remap classpath", e);
            }
        } else {
            classPath = ClasspathUtils.getClassPathFromObjectShare();

            if (!mappingsRegistry.isSourceNamespaceObf()) {
                classPath = getMinecraftJar(classPath, "official", mappingsRegistry.getSourceNamespace(), mappingsRegistry, cacheHandler);
            }
        }

        var mergableLibraries = libraryHandler.getMergableLibraries();

        if (!mergableLibraries.isEmpty()) {
            classPath = addMergeableLibrariesToClassPath(classPath, mergableLibraries, cacheHandler.resolveLibrary(mappingsRegistry.getSourceNamespace()));
        }

        for (var path : classPath) {
            Constants.MAIN_LOGGER.debug("Appending '%s' to remapper classpath", path);
            remapper.readClassPathAsync(path);
        }
    }

    private static Collection<Path> addMergeableLibrariesToClassPath(Collection<Path> classPath, Collection<Map.Entry<RemapLibrary, Path>> mergableLibraries, Path libraryPath) throws IOException {
        var paths = new HashSet<>(classPath);
        var mcPath = paths.stream()
                .filter(path -> path.getFileName().toString().contains("minecraft"))
                .findFirst()
                .orElse(null);

        if (mcPath == null) return classPath;

        var libsHash = computeLibsHash(mergableLibraries);
        var mcOutputPath = libraryPath.resolve(mcPath.getFileName().toString().replace(".jar", "") + "-" + libsHash + ".jar");

        if (!Files.exists(mcOutputPath)) {
            Files.copy(mcPath, mcOutputPath);
            try (var zipfs = getJarFileSystem(mcOutputPath)) {
                for (var libPath : mergableLibraries.stream().map(Map.Entry::getValue).toList()) {
                    try (var libZipfs = getJarFileSystem(libPath)) {
                        for (var entry : FileUtils.listZipContent(libPath)) {
                            var libEntry = libZipfs.getPath(entry);
                            var mcEntry = zipfs.getPath(entry);
                            var parent = mcEntry.getParent();
                            if (parent != null) Files.createDirectories(parent);
                            Files.copy(libEntry, mcEntry, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            }
        }

        paths.remove(mcPath);
        paths.add(mcOutputPath);
        return paths;
    }

    private static String computeLibsHash(Collection<Map.Entry<RemapLibrary, Path>> mergableLibraries) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var input = mergableLibraries.stream()
                    .map(e -> e.getKey().getFileName())
                    .collect(Collectors.joining())
                    .getBytes(StandardCharsets.UTF_8);
            // Use first 5 bytes (10 hex chars) as a short stable identifier
            return HexFormat.of().formatHex(digest.digest(input), 0, 5);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // SHA-256 is always available
        }
    }
}
