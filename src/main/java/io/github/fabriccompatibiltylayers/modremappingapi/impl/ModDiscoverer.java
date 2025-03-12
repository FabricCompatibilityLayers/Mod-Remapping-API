package io.github.fabriccompatibiltylayers.modremappingapi.impl;

import fr.catcore.modremapperapi.utils.Constants;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ModRemapper;
import fr.catcore.modremapperapi.remapping.RemapUtil;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.compatibility.V0ModRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.CacheUtils;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.FileUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.include.com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

public class ModDiscoverer {
    private static final Map<String, List<String>> EXCLUDED = new HashMap<>();

    protected static void init(List<ModRemapper> modRemappers, boolean remapClassEdits) {
        RemapUtil.init(modRemappers);

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
                    else io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.FileUtils.emptyDir(cacheFolder);

                    mods.addAll(discoverModsInFolder(mcSubFolder, cacheFolder));
                } catch (IOException | URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        }

        File mainTempDir = CacheUtils.getCachePath("temp").toFile();
        if (mainTempDir.exists()) {
            io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.FileUtils.emptyDir(mainTempDir.toPath());
        }

        Map<Path, Path> modPaths = mods.stream()
                .filter(entry -> entry.original != null)
                .collect(Collectors.toMap(entry -> entry.original.toPath(), entry -> entry.file.toPath()));

        if (!remapClassEdits) {
            modPaths = excludeClassEdits(modPaths);
        }

        for (Path path : modPaths.keySet()) {
            RemapUtil.makeModMappings(path);
        }

        RemapUtil.generateModMappings();

        RemapUtil.remapMods(modPaths);

        modPaths.values().forEach(FabricLauncherBase.getLauncher()::addToClassPath);
    }

    private static Map<Path, Path> excludeClassEdits(Map<Path, Path> modPaths) {
        Map<Path, Path> map = new HashMap<>();
        Map<Path, Path> convertMap = new HashMap<>();

        File mainTempDir = CacheUtils.getCachePath("temp").toFile();
        mainTempDir.mkdirs();


        for (Map.Entry<Path, Path> entry : modPaths.entrySet()) {
            File tempDir = new File(mainTempDir, entry.getValue().toFile().getParentFile().getName());
            if (!tempDir.exists()) tempDir.mkdir();

            File tempFile = new File(tempDir, entry.getValue().toFile().getName());
            map.put(tempFile.toPath(), entry.getValue());
            convertMap.put(entry.getKey(), tempFile.toPath());
        }

        List<Path> errored = new ArrayList<>();

        for (Map.Entry<Path, Path> entry : convertMap.entrySet()) {
            try {
                if (Files.isDirectory(entry.getKey())) {
                    FileUtils.zipFolder(entry.getKey(), entry.getValue());
                } else {
                    Files.copy(entry.getKey(), entry.getValue(), StandardCopyOption.REPLACE_EXISTING);
                }

                FileUtils.removeEntriesFromZip(entry.getValue(), RemapUtil.MC_CLASS_NAMES);
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
                            folder.toFile(),
                            destination.toFile()
                    )
            );
        }

        return Optional.empty();
    }

    private static Optional<ModEntry> discoverFileMod(Path file, Path destinationFolder) throws IOException {
        String fileName = file.getFileName().toString().replace(" ", "_");
        String modName = fileName.replace(".jar", "").replace(".zip", "");

        List<String> entries = FileUtils.listZipContent(file);

        for (String entry : entries) {
            if (Objects.equals(entry, "/fabric.mod.json")) break;

            if (entry.endsWith(".class")) {
                return Optional.of(
                        new DefaultModEntry(
                                modName,
                                file.toFile(),
                                destinationFolder.resolve(fileName).toFile()
                        )
                );
            }
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
                if (Files.isDirectory(modEntry.file.toPath())) {
                    for (String excluded : EXCLUDED.get(modEntry.modId)) {
                        if (Files.deleteIfExists(modEntry.file.toPath().resolve(excluded))) {
                            Constants.MAIN_LOGGER.debug("File deleted: " + modEntry.file.toPath().resolve(excluded));
                        }
                    }
                } else {
                    FileUtils.removeEntriesFromZip(modEntry.file.toPath(), EXCLUDED.get(modEntry.modId));
                }
            }
        }

        return mods;
    }
}
