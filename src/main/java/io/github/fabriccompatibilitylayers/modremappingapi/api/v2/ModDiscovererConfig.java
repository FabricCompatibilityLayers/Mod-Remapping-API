package io.github.fabriccompatibilitylayers.modremappingapi.api.v2;

import io.github.fabriccompatibilitylayers.modremappingapi.impl.ModDiscovererConfigImpl;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public interface ModDiscovererConfig {
    static Builder builder(String folderName) {
        return new ModDiscovererConfigImpl.BuilderImpl(folderName);
    }

    String getFolderName();
    Pattern getFileNameMatcher();
    boolean searchRecursively();
    Predicate<String> getDirectoryFilter();
    Collector getCandidateCollector();
    boolean getExportToOriginalFolder();
    boolean allowDirectoryMods();

    interface Builder {
        Builder fileNameMatcher(String pattern);
        Builder searchRecursively(boolean searchRecursively);
        Builder directoryFilter(Predicate<String> filter);
        Builder candidateCollector(Collector collector);
        Builder exportToOriginalFolder(boolean exportToOriginalFolder);
        Builder allowDirectoryMods(boolean allowDirectoryMods);

        ModDiscovererConfig build();
    }

    @FunctionalInterface
    interface Collector {
        List<ModCandidate> collect(ModDiscovererConfig config, Path modPath, List<String> entries);
    }
}
