package io.github.fabriccompatibilitylayers.modremappingapi.impl;

import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.ModDiscoverer;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Predicate;
import java.util.regex.Pattern;

@ApiStatus.Internal
public class ModDiscovererImpl implements ModDiscoverer {
    private final String folderName;
    private final Pattern fileNameMatcher;
    private final boolean searchRecursively;
    private final Predicate<String> directoryFilter;

    private ModDiscovererImpl(String folderName, Pattern fileNameMatcher, boolean searchRecursively, Predicate<String> directoryFilter) {
        this.folderName = folderName;
        this.fileNameMatcher = fileNameMatcher;
        this.searchRecursively = searchRecursively;
        this.directoryFilter = directoryFilter;
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

    public static class BuilderImpl implements ModDiscoverer.Builder {
        private final String folderName;
        private String fileNameMatcher = "(.+).jar$";
        private boolean searchRecursively = false;
        private Predicate<String> directoryFilter = s -> true;

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
        public ModDiscoverer build() {
            return new ModDiscovererImpl(folderName, Pattern.compile(fileNameMatcher), searchRecursively, directoryFilter);
        }
    }
}
