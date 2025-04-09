package io.github.fabriccompatibiltylayers.modremappingapi.api.v1;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Deprecated
public class RemapLibrary implements io.github.fabriccompatibilitylayers.modremappingapi.api.v2.RemapLibrary {
    public final String url;
    public final Path path;
    public final List<String> toExclude;
    public final String fileName;

    public RemapLibrary(String url, List<String> toExclude, String fileName) {
        this.url = url;
        this.path = null;
        this.toExclude = toExclude;
        this.fileName = fileName;
    }

    public RemapLibrary(Path path, List<String> toExclude, String fileName) {
        this.url = "";
        this.path = path;
        this.toExclude = toExclude;
        this.fileName = fileName;
    }

    public RemapLibrary(Path path, String fileName) {
        this.url = "";
        this.path = path;
        this.toExclude = new ArrayList<>();
        this.fileName = fileName;
    }

    @ApiStatus.Internal
    public RemapLibrary(String url, Path path, List<String> toExclude, String fileName) {
        this.url = url;
        this.path = path;
        this.toExclude = toExclude;
        this.fileName = fileName;
    }

    @Override
    public @Nullable String getURL() {
        return this.url;
    }

    @Override
    public @Nullable Path getPath() {
        return this.path;
    }

    @Override
    public String getFileName() {
        return this.fileName;
    }

    @Override
    public List<String> getToExclude() {
        return this.toExclude;
    }
}
