package io.github.fabriccompatibilitylayers.modremappingapi.impl.defaults;

import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.ModCandidate;
import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.ModDiscovererConfig;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@ApiStatus.Internal
public class ModDiscovererConfigImpl implements ModDiscovererConfig {
    private final String folderName;
    private final Pattern fileNameMatcher;
    private final boolean searchRecursively;
    private final Predicate<String> directoryFilter;
    private final @Nullable Collector candidateCollector;
    private final boolean exportToOriginalFolder;
    private final boolean allowDirectoryMods;

    private ModDiscovererConfigImpl(String folderName, Pattern fileNameMatcher, boolean searchRecursively, Predicate<String> directoryFilter, @Nullable Collector candidateCollector, boolean exportToOriginalFolder, boolean allowDirectoryMods) {
        this.folderName = folderName;
        this.fileNameMatcher = fileNameMatcher;
        this.searchRecursively = searchRecursively;
        this.directoryFilter = directoryFilter;
        this.candidateCollector = candidateCollector;
        this.exportToOriginalFolder = exportToOriginalFolder;
        this.allowDirectoryMods = allowDirectoryMods;
    }

    @Override
    public String getFolderName() {
        return folderName;
    }

    @Override
    public Pattern getFileNameMatcher() {
        return fileNameMatcher;
    }

    @Override
    public boolean searchRecursively() {
        return searchRecursively;
    }

    @Override
    public Predicate<String> getDirectoryFilter() {
        return directoryFilter;
    }

    @Override
    public Collector getCandidateCollector() {
        return this.candidateCollector == null ? this::defaultCandidateCollector : this.candidateCollector;
    }

    @Override
    public boolean getExportToOriginalFolder() {
        return this.exportToOriginalFolder;
    }

    private List<ModCandidate> defaultCandidateCollector(ModDiscovererConfig config, Path modPath, List<String> fileList) {
        List<ModCandidate> candidates = new ArrayList<>();

        for (String file : fileList) {
            if (file.endsWith(".class")) {
                candidates.add(new DefaultModCandidate(modPath, config));
                break;
            }
        }

        return candidates;
    }

    @Override
    public boolean allowDirectoryMods() {
        return allowDirectoryMods;
    }

    public static class BuilderImpl implements ModDiscovererConfig.Builder {
        private final String folderName;
        private String fileNameMatcher = "(.+).jar$";
        private boolean searchRecursively = false;
        private Predicate<String> directoryFilter = s -> true;
        private Collector candidateCollector;
        private boolean exportToOriginalFolder = false;
        private boolean allowDirectoryMods = false;

        public BuilderImpl(String folderName) {
            this.folderName = folderName;
        }

        @Override
        public Builder fileNameMatcher(String pattern) {
            this.fileNameMatcher = pattern;
            return this;
        }

        @Override
        public Builder searchRecursively(boolean searchRecursively) {
            this.searchRecursively = searchRecursively;
            return this;
        }

        @Override
        public Builder directoryFilter(Predicate<String> filter) {
            this.directoryFilter = filter;
            return this;
        }

        @Override
        public Builder candidateCollector(Collector collector) {
            this.candidateCollector = collector;
            return this;
        }

        @Override
        public Builder exportToOriginalFolder(boolean exportToOriginalFolder) {
            this.exportToOriginalFolder = exportToOriginalFolder;
            return this;
        }

        @Override
        public Builder allowDirectoryMods(boolean allowDirectoryMods) {
            this.allowDirectoryMods = allowDirectoryMods;
            return this;
        }

        @Override
        public ModDiscovererConfig build() {
            return new ModDiscovererConfigImpl(
                    folderName,
                    Pattern.compile(fileNameMatcher),
                    searchRecursively,
                    directoryFilter,
                    candidateCollector,
                    exportToOriginalFolder,
                    allowDirectoryMods
            );
        }
    }
}
