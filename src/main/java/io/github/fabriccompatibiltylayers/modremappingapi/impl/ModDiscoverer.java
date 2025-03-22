package io.github.fabriccompatibiltylayers.modremappingapi.impl;

import fr.catcore.modremapperapi.utils.Constants;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ModRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.compatibility.V0ModRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings.MappingsRegistry;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.CacheUtils;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.FileUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.include.com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

public class ModDiscoverer {
    private static final Map<String, List<String>> EXCLUDED = new HashMap<>();

    protected static void init(List<ModRemapper> modRemappers, boolean remapClassEdits) {
        ModRemapperContext context = new ModRemapperContext(modRemappers);

        context.init();

        List<ModEntry> mods = new ArrayList<>();

        for (ModRemapper remapper : modRemappers) {
            if (remapper instanceof V0ModRemapper) {
                EXCLUDED.putAll(((V0ModRemapper) remapper).getExclusions());
            }
        }

        for (ModRemapper remapper : modRemappers) {
            for (String jarFolder : remapper.getJarFolders()) {
                Path mcSubFolder = FabricLoader.getInstance().getGameDir().resolve(jarFolder);
                Path cacheFolder = CacheUtils.getCachePath(jarFolder);

                try {
                    if (!Files.exists(mcSubFolder)) Files.createDirectories(mcSubFolder);
                    if (!Files.exists(cacheFolder)) Files.createDirectories(cacheFolder);
                    else FileUtils.emptyDir(cacheFolder);

                    mods.addAll(discoverModsInFolder(mcSubFolder, cacheFolder));
                } catch (IOException | URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        }

        Path mainTempDir = CacheUtils.getCachePath("temp");

        if (Files.exists(mainTempDir)) {
            FileUtils.emptyDir(mainTempDir);
        }

        try {
            Files.createDirectory(mainTempDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Map<Path, Path> modPaths = mods.stream()
                .filter(entry -> Files.exists(entry.original))
                .collect(Collectors.groupingBy(entry -> entry.modId))
                .entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getValue().get(0).original, entry -> entry.getValue().get(0).file));

        if (!remapClassEdits) {
            modPaths = excludeClassEdits(modPaths, mainTempDir);
        }

        for (Path path : modPaths.keySet()) {
            MappingsRegistry.addModMappings(path);
        }

        MappingsRegistry.generateModMappings();

        context.remapMods(modPaths);

        modPaths.values().forEach(FabricLauncherBase.getLauncher()::addToClassPath);
    }

    private static Map<Path, Path> excludeClassEdits(Map<Path, Path> modPaths, Path tempFolder) {
        Map<Path, Path> map = new HashMap<>();
        Map<Path, Path> convertMap = new HashMap<>();

        for (Map.Entry<Path, Path> entry : modPaths.entrySet()) {
            Path tempDir = tempFolder.resolve(entry.getValue().getParent().getFileName().toString());

            if (!Files.exists(tempDir)) {
                try {
                    Files.createDirectory(tempDir);
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
            }

            Path tempFile = tempDir.resolve(entry.getValue().getFileName().toString());
            map.put(tempFile, entry.getValue());
            convertMap.put(entry.getKey(), tempFile);
        }

        List<Path> errored = new ArrayList<>();

        for (Map.Entry<Path, Path> entry : convertMap.entrySet()) {
            try {
                if (Files.isDirectory(entry.getKey())) {
                    FileUtils.zipFolder(entry.getKey(), entry.getValue());
                } else {
                    Files.copy(entry.getKey(), entry.getValue(), StandardCopyOption.REPLACE_EXISTING);
                }

                FileUtils.removeEntriesFromZip(entry.getValue(), MappingsRegistry.VANILLA_CLASS_LIST);
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
                errored.add(entry.getValue());
            }
        }

        errored.forEach(map::remove);

        return map;
    }

    private static Optional<ModEntry> discoverFolderMod(Path folder, Path destinationFolder) throws IOException {
        String name = folder.getFileName().toString().replace(" ", "_");
        Path destination = destinationFolder.resolve(name + ".zip");

        final boolean[] hasClasses = {false};

        Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
            @Override
            public @NotNull FileVisitResult visitFile(Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".class")) {
                    hasClasses[0] = true;
                    return FileVisitResult.TERMINATE;
                }

                return super.visitFile(file, attrs);
            }
        });

        if (hasClasses[0]) {
            return Optional.of(
                    new DefaultModEntry(
                            name,
                            destination,
                            folder
                    )
            );
        }

        return Optional.empty();
    }

    private static Optional<ModEntry> discoverFileMod(Path file, Path destinationFolder) throws IOException {
        String fileName = file.getFileName().toString().replace(" ", "_");
        String modName = fileName.replace(".jar", "").replace(".zip", "");

        List<String> entries = FileUtils.listZipContent(file);

        boolean found = false;

        for (String entry : entries) {
            if (entry.contains("fabric.mod.json")) return Optional.empty();

            if (entry.endsWith(".class")) {
                found = true;
            }
        }

        if (found) {
            return Optional.of(
                    new DefaultModEntry(
                            modName,
                            destinationFolder.resolve(fileName),
                            file
                    )
            );
        }

        return Optional.empty();
    }

    private static List<ModEntry> discoverModsInFolder(Path folder, Path destination) throws IOException, URISyntaxException {
        List<ModEntry> mods = new ArrayList<>();

        if (!Files.isDirectory(folder)) return ImmutableList.of();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
            for (Path path : stream) {
                String name = path.getFileName().toString();

                if (Files.isDirectory(path)) {
                    discoverFolderMod(path, destination)
                            .ifPresent(mods::add);
                } else if (Files.exists(path) && (name.endsWith(".jar") || name.endsWith(".zip"))) {
                    discoverFileMod(path, destination)
                            .ifPresent(mods::add);
                }
            }
        }

        for (ModEntry modEntry : mods) {
            if (EXCLUDED.containsKey(modEntry.modId)) {
                if (Files.isDirectory(modEntry.file)) {
                    for (String excluded : EXCLUDED.get(modEntry.modId)) {
                        if (Files.deleteIfExists(modEntry.file.resolve(excluded))) {
                            Constants.MAIN_LOGGER.debug("File deleted: " + modEntry.file.resolve(excluded));
                        }
                    }
                } else {
                    FileUtils.removeEntriesFromZip(modEntry.file, EXCLUDED.get(modEntry.modId));
                }
            }
        }

        return mods;
    }
}
