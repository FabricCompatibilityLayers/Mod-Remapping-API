package io.github.fabriccompatibiltylayers.modremappingapi.impl.context.v2;

import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.ModCandidate;
import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.ModDiscovererConfig;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.FileUtils;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class V2ModDiscoverer {
    private final ModDiscovererConfig config;

    public V2ModDiscoverer(ModDiscovererConfig config) {
        this.config = config;
    }

    public List<ModCandidate> collect() {
        Path folder = FabricLoader.getInstance().getGameDir().resolve(config.getFolderName());

        if (!Files.isDirectory(folder)) return new ArrayList<>();

        List<ModCandidate> candidates = new ArrayList<>();

        try {
            searchDir(candidates, folder);
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
}
