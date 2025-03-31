package io.github.fabriccompatibiltylayers.modremappingapi.impl.context.v1;

import fr.catcore.modremapperapi.utils.Constants;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ModRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.DefaultModCandidate;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.ModCandidate;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.compatibility.V0ModRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.discover.BaseModDiscoverer;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.CacheUtils;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.FileUtils;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

public class V1ModDiscoverer extends BaseModDiscoverer {
    private final Map<String, List<String>> excluded = new HashMap<>();

    public Map<Path, Path> init(List<ModRemapper> modRemappers, boolean remapClassEdits, ModRemapperV1Context context) {
        Set<String> modFolders = new HashSet<>();

        for (ModRemapper remapper : modRemappers) {
            Collections.addAll(modFolders, remapper.getJarFolders());

            if (remapper instanceof V0ModRemapper) {
                excluded.putAll(((V0ModRemapper) remapper).getExclusions());
            }
        }

        List<ModCandidate> mods = new ArrayList<>();

        for (String jarFolder : modFolders) {
            Path mcSubFolder = FabricLoader.getInstance().getGameDir().resolve(jarFolder);
            Path cacheFolder = CacheUtils.getCachePath(jarFolder);

            try {
                if (!Files.exists(mcSubFolder)) Files.createDirectories(mcSubFolder);
                if (!Files.exists(cacheFolder)) Files.createDirectories(cacheFolder);
                else FileUtils.emptyDir(cacheFolder);

                mods.addAll(this.discoverMods(mcSubFolder, cacheFolder));

                this.handleV0Excluded(mods);
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
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
            modPaths = excludeClassEdits(modPaths, mainTempDir, context.getMappingsRegistry());
        }

        return modPaths;
    }

    private void handleV0Excluded(List<ModCandidate> mods) throws IOException, URISyntaxException {
        for (ModCandidate modCandidate : mods) {
            if (excluded.containsKey(modCandidate.modId)) {
                if (Files.isDirectory(modCandidate.file)) {
                    for (String excluded : excluded.get(modCandidate.modId)) {
                        if (Files.deleteIfExists(modCandidate.file.resolve(excluded))) {
                            Constants.MAIN_LOGGER.debug("File deleted: " + modCandidate.file.resolve(excluded));
                        }
                    }
                } else {
                    FileUtils.removeEntriesFromZip(modCandidate.file, excluded.get(modCandidate.modId));
                }
            }
        }
    }

    @Override
    public boolean isValidFileName(String fileName) {
        return fileName.endsWith(".jar") || fileName.endsWith(".zip");
    }

    @Override
    public boolean allowDirectoryMods() {
        return true;
    }

    @Override
    public boolean searchRecursively() {
        return false;
    }

    @Override
    public boolean isValidDirectoryName(String directoryName) {
        return false;
    }

    @Override
    public Optional<ModCandidate> discoverFolderMod(Path folder, Path destinationFolder) throws IOException {
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
                    new DefaultModCandidate(
                            name,
                            destination,
                            folder
                    )
            );
        }

        return Optional.empty();
    }

    @Override
    public Optional<ModCandidate> discoverFileMod(Path file, Path destinationFolder) throws IOException {
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
                    new DefaultModCandidate(
                            modName,
                            destinationFolder.resolve(fileName),
                            file
                    )
            );
        }

        return Optional.empty();
    }
}
