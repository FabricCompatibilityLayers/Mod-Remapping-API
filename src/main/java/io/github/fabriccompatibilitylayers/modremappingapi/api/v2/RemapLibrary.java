package io.github.fabriccompatibilitylayers.modremappingapi.api.v2;

import io.github.fabriccompatibilitylayers.modremappingapi.impl.defaults.DefaultRemapLibrary;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

public interface RemapLibrary {
    @Nullable String getURL();
    @Nullable Path getPath();
    String getFileName();
    List<String> getToExclude();

    static RemapLibrary of(Path path, String fileName) {
        return new DefaultRemapLibrary(path, fileName);
    }

    static RemapLibrary of(Path path, String fileName, List<String> toExclude) {
        return new DefaultRemapLibrary(path, fileName, toExclude);
    }

    static RemapLibrary of(String url, String fileName, List<String> toExclude) {
        return new DefaultRemapLibrary(url, fileName, toExclude);
    }
}
