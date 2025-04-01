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
    BiFunction<Path, List<String>, List<ModCandidate>> getCandidateCollector();

    interface Builder {
        Builder fileNameMatcher(String pattern);
        Builder searchRecursively(boolean searchRecursively);
        Builder directoryFilter(Predicate<String> filter);
        Builder candidateCollector(BiFunction<Path, List<String>, List<ModCandidate>> collector);

        ModDiscovererConfig build();
    }
}
