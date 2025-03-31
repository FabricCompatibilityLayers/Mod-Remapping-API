package io.github.fabriccompatibilitylayers.modremappingapi.api.v2;

import io.github.fabriccompatibilitylayers.modremappingapi.impl.ModDiscovererImpl;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public interface ModDiscoverer {
    static Builder builder(String folderName) {
        return new ModDiscovererImpl.BuilderImpl(folderName);
    }

    String getFolderName();
    Pattern getFileNameMatcher();
    boolean searchRecursively();
    Predicate<String> getDirectoryFilter();

    interface Builder {
        Builder fileNameMatcher(String pattern);
        Builder searchRecursively(boolean searchRecursively);
        Builder directoryFilter(Predicate<String> filter);

        ModDiscoverer build();
    }
}
