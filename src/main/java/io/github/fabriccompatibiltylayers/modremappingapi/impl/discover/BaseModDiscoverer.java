package io.github.fabriccompatibiltylayers.modremappingapi.impl.discover;

import io.github.fabriccompatibiltylayers.modremappingapi.impl.ModCandidate;
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
    public abstract boolean allowDirectoryMods();
    public abstract boolean searchRecursively();
    public abstract boolean isValidDirectoryName(String directoryName);
    public abstract Optional<ModCandidate> discoverFolderMod(Path folder, Path destinationFolder) throws IOException;
    public abstract Optional<ModCandidate> discoverFileMod(Path file, Path destinationFolder) throws IOException;

    public List<ModCandidate> discoverMods(Path folder, Path destination) throws IOException {
        List<ModCandidate> mods = new ArrayList<>();

        if (!Files.isDirectory(folder)) return ImmutableList.of();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
            for (Path path : stream) {
                String name = path.getFileName().toString();

                if (Files.isDirectory(path)) {
                    if (searchRecursively() && isValidDirectoryName(name)) {
                        mods.addAll(discoverMods(folder.resolve(name), destination.resolve(name)));
                    } else if (allowDirectoryMods()) {
                        discoverFolderMod(path, destination)
                                .ifPresent(mods::add);
                    }
                } else if (Files.exists(path) && isValidFileName(name)) {
                    discoverFileMod(path, destination)
                            .ifPresent(mods::add);
                }
            }
        }

        return mods;
    }

    public Map<ModCandidate, Path> excludeClassEdits(Map<ModCandidate, Path> modPaths, Path tempFolder, MappingsRegistry mappingsRegistry) {
        Map<ModCandidate, Path> map = new HashMap<>();
        Map<ModCandidate, Path> convertMap = new HashMap<>();

        for (Map.Entry<ModCandidate, Path> entry : modPaths.entrySet()) {
            ModCandidate modCandidate = entry.getKey();
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
            map.put(new ModCandidate(
                    modCandidate.modName,
                    modCandidate.accessWidenerName,
                    modCandidate.file,
                    tempFile
            ), entry.getValue());
            convertMap.put(entry.getKey(), tempFile);
        }

        List<ModCandidate> errored = new ArrayList<>();

        for (Map.Entry<ModCandidate, Path> entry : convertMap.entrySet()) {
            ModCandidate modCandidate = entry.getKey();
            try {
                if (Files.isDirectory(modCandidate.original)) {
                    FileUtils.zipFolder(modCandidate.original, entry.getValue());
                } else {
                    Files.copy(modCandidate.original, entry.getValue(), StandardCopyOption.REPLACE_EXISTING);
                }

                FileUtils.removeEntriesFromZip(entry.getValue(), mappingsRegistry.getVanillaClassNames());
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
                errored.add(modCandidate);
            }
        }

        errored.forEach(map::remove);

        return map;
    }
}
