package io.github.fabriccompatibiltylayers.modremappingapi.impl.context.v2;

import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.ModCandidate;
import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.ModDiscovererConfig;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.CacheUtils;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.FileUtils;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class V2ModDiscoverer {
    private final String contextId;
    private final ModDiscovererConfig config;
    private Path originalDirectory;

    public V2ModDiscoverer(String contextId, ModDiscovererConfig config) {
        this.contextId = contextId;
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
                    } else {
                        // TODO Add directory mods support
                    }
                } else if (Files.exists(path)) {
                    Matcher matcher = config.getFileNameMatcher().matcher(name);

                    if (matcher.matches()) {
                        discoverFileMods(candidates, path);
                    }
                }
            }
        }
    }

    private void discoverFileMods(List<ModCandidate> candidates, Path modPath) throws IOException {
        List<String> entries = FileUtils.listZipContent(modPath);

        candidates.addAll(config.getCandidateCollector().apply(modPath, entries));
    }

    public Map<ModCandidate, Path> computeDestinations(List<ModCandidate> candidates) {
        Path destination;

        if (config.getExportToOriginalFolder()) {
            destination = originalDirectory;
        } else {
            destination = CacheUtils.getCachePath(this.contextId + "/" + config.getFolderName());
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
                    return modDestination;
                }
        ));
    }
}
