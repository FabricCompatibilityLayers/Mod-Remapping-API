package io.github.fabriccompatibiltylayers.modremappingapi.impl.discover;

import io.github.fabriccompatibiltylayers.modremappingapi.impl.ModEntry;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings.MappingsRegistry;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.FileUtils;
import org.spongepowered.include.com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public abstract class BaseModDiscoverer {
    public abstract boolean isValidFileName(String fileName);
    public abstract boolean allowDirectories();
    public abstract Optional<ModEntry> discoverFolderMod(Path folder, Path destinationFolder) throws IOException;
    public abstract Optional<ModEntry> discoverFileMod(Path file, Path destinationFolder) throws IOException;

    public List<ModEntry> discoverMods(Path folder, Path destination) throws IOException {
        List<ModEntry> mods = new ArrayList<>();

        if (!Files.isDirectory(folder)) return ImmutableList.of();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
            for (Path path : stream) {
                String name = path.getFileName().toString();

                if (Files.isDirectory(path) && allowDirectories()) {
                    discoverFolderMod(path, destination)
                            .ifPresent(mods::add);
                } else if (Files.exists(path) && isValidFileName(name)) {
                    discoverFileMod(path, destination)
                            .ifPresent(mods::add);
                }
            }
        }

        return mods;
    }

    public Map<Path, Path> excludeClassEdits(Map<Path, Path> modPaths, Path tempFolder, MappingsRegistry mappingsRegistry) {
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

                FileUtils.removeEntriesFromZip(entry.getValue(), mappingsRegistry.getVanillaClassNames());
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
                errored.add(entry.getValue());
            }
        }

        errored.forEach(map::remove);

        return map;
    }
}
