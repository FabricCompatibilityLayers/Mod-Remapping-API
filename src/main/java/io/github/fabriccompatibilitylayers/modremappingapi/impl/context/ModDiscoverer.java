package io.github.fabriccompatibilitylayers.modremappingapi.impl.context;

import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.ModCandidate;
import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.ModDiscovererConfig;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.mappings.MappingsRegistry;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.utils.FileUtils;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@ApiStatus.Internal
public class ModDiscoverer {
    private final ModDiscovererConfig config;
    private Path originalDirectory;

    public ModDiscoverer(ModDiscovererConfig config) {
        this.config = config;
    }

    public List<ModCandidate> collect() {
        originalDirectory = FabricLoader.getInstance().getGameDir().resolve(config.getFolderName());

        if (!Files.isDirectory(originalDirectory)) return Collections.emptyList();

        var candidates = new ArrayList<ModCandidate>();

        try {
            searchDir(candidates, originalDirectory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return candidates;
    }

    private void searchDir(List<ModCandidate> candidates, Path dir) throws IOException {
        try (var stream = Files.newDirectoryStream(dir)) {
            for (var path : stream) {
                var name = path.getFileName().toString();

                if (Files.isDirectory(path)) {
                    if (config.searchRecursively()) {
                        if (config.getDirectoryFilter().test(name)) searchDir(candidates, path);
                    } else if (config.allowDirectoryMods()) {
                        discoverMods(candidates, path);
                    }
                } else if (Files.exists(path)) {
                    var matcher = config.getFileNameMatcher().matcher(name);

                    if (matcher.matches()) {
                        discoverMods(candidates, path);
                    }
                }
            }
        }
    }

    private void discoverMods(List<ModCandidate> candidates, Path modPath) throws IOException {
        var entries = FileUtils.listPathContent(modPath);
        candidates.addAll(config.getCandidateCollector().collect(config, modPath, entries));
    }

    public void excludeClassEdits(List<ModCandidate> candidates, InternalCacheHandler cacheHandler, MappingsRegistry registry) throws IOException, URISyntaxException {
        var tempDirectory = cacheHandler.resolveTemp(this.config.getFolderName());

        if (!Files.exists(tempDirectory)) {
            try {
                Files.createDirectories(tempDirectory);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create temp directory", e);
            }
        } else {
            FileUtils.emptyDir(tempDirectory);
        }

        for (var candidate : candidates) {
            var modPath = candidate.getPath();
            var outputName = candidate.getDestinationName();
            var outputPath = tempDirectory.resolve(outputName);

            if (Files.isDirectory(modPath)) {
                FileUtils.zipFolder(modPath, outputPath);
            } else {
                Files.copy(modPath, outputPath);
            }

            FileUtils.removeEntriesFromZip(outputPath, registry.getVanillaClassNames());
            candidate.setPath(outputPath);
        }
    }

    public Map<ModCandidate, Path> computeDestinations(List<ModCandidate> candidates, InternalCacheHandler cacheHandler) {
        Path destination;

        if (config.getExportToOriginalFolder()) {
            destination = originalDirectory;
        } else {
            destination = cacheHandler.resolveCache(config.getFolderName());
        }

        if (!Files.exists(destination)) {
            try {
                Files.createDirectories(destination);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create destination directory", e);
            }
        } else if (!config.getExportToOriginalFolder()) {
            FileUtils.emptyDir(destination);
        }

        var finalDestination = destination;

        return candidates.stream().collect(Collectors.toMap(
                candidate -> candidate,
                candidate -> {
                    var modDestination = finalDestination.resolve(candidate.getDestinationName());
                    candidate.setDestination(modDestination);

                    if (Files.exists(modDestination)) {
                        try {
                            Files.delete(modDestination);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to delete existing mod destination", e);
                        }
                    }

                    return modDestination;
                }
        ));
    }
}
