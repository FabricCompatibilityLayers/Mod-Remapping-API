package io.github.fabriccompatibilitylayers.modremappingapi.api.v2;

import io.github.fabriccompatibilitylayers.modremappingapi.impl.defaults.DefaultRemapLibrary;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public interface RemapLibrary {
    static Builder builder(String fileName) {
        return new DefaultRemapLibrary.BuilderImpl(fileName);
    }

    @Nullable String getURL();
    @Nullable Path getPath();
    String getFileName();
    List<String> getToExclude();

    default boolean mergeWithMainJar() {
        return false;
    }
    default int getPriority() {
        return 1000;
    }

    @Deprecated
    static RemapLibrary of(Path path, String fileName) {
        return RemapLibrary.builder(fileName)
                .path(path)
                .build();
    }

    @Deprecated
    static RemapLibrary of(Path path, String fileName, List<String> toExclude) {
        return RemapLibrary.builder(fileName)
                .path(path)
                .addExcludedPaths(toExclude)
                .build();
    }

    @Deprecated
    static RemapLibrary of(String url, String fileName, List<String> toExclude) {
        return RemapLibrary.builder(fileName)
                .url(url)
                .addExcludedPaths(toExclude)
                .build();
    }

    interface Builder {
        Builder url(String url);
        Builder path(Path path);
        Builder addExcludedPaths(Collection<String> toExclude);
        Builder mergeWithMainJar(boolean mergeWithMainJar);
        Builder priority(int priority);
        RemapLibrary build();
    }
}
