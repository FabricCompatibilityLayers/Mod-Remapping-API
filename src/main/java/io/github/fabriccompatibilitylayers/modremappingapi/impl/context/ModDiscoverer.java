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

        if (!Files.isDirectory(originalDirectory)) return new ArrayList<>();

        List<ModCandidate> candidates = new ArrayList<>();

        try {
            searchDir(candidates, originalDirectory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return candidates;
    }

    private void searchDir(List<ModCandidate> candidates, Path dir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                String name = path.getFileName().toString();

                if (Files.isDirectory(path)) {
                    if (config.searchRecursively()) {
                        if (config.getDirectoryFilter().test(name)) searchDir(candidates, path);
                    } else if (config.allowDirectoryMods()) {
                        discoverMods(candidates, path);
                    }
                } else if (Files.exists(path)) {
                    Matcher matcher = config.getFileNameMatcher().matcher(name);

                    if (matcher.matches()) {
                        discoverMods(candidates, path);
                    }
                }
            }
        }
    }

    private void discoverMods(List<ModCandidate> candidates, Path modPath) throws IOException {
        List<String> entries = FileUtils.listPathContent(modPath);

        candidates.addAll(config.getCandidateCollector().collect(config, modPath, entries));
    }

    public void excludeClassEdits(List<ModCandidate> candidates, InternalCacheHandler cacheHandler, MappingsRegistry registry) throws IOException, URISyntaxException {
        Path tempDirectory = cacheHandler.resolveTemp(this.config.getFolderName());

        if (!Files.exists(tempDirectory)) {
            try {
                Files.createDirectories(tempDirectory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            FileUtils.emptyDir(tempDirectory);
        }

        for (ModCandidate candidate : candidates) {
            Path modPath = candidate.getPath();

            String outputName = candidate.getDestinationName();
            Path outputPath = tempDirectory.resolve(outputName);

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
                throw new RuntimeException(e);
            }
        } else if (!config.getExportToOriginalFolder()) {
            FileUtils.emptyDir(destination);
        }

        Path finalDestination = destination;

        return candidates.stream().collect(Collectors.toMap(
                candidate -> candidate,
                candidate -> {
                    Path modDestination = finalDestination.resolve(candidate.getDestinationName());
                    candidate.setDestination(modDestination);

                    if (Files.exists(modDestination)) {
                        try {
                            Files.delete(modDestination);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    return modDestination;
                }
        ));
    }
}
