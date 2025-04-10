package io.github.fabriccompatibilitylayers.modremappingapi.impl.defaults;

import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.RemapLibrary;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@ApiStatus.Internal
public class DefaultRemapLibrary implements RemapLibrary {
    private final @Nullable String url;
    private final @Nullable Path path;
    private final String fileName;
    private final List<String> toExclude;

    public DefaultRemapLibrary(@Nullable String url, String fileName, List<String> toExclude) {
        this.url = url;
        this.path = null;
        this.fileName = fileName;
        this.toExclude = toExclude;
    }

    public DefaultRemapLibrary(@Nullable Path path, String fileName, List<String> toExclude) {
        this.url = null;
        this.path = path;
        this.fileName = fileName;
        this.toExclude = toExclude;
    }

    public DefaultRemapLibrary(@Nullable Path path, String fileName) {
        this(path, fileName, Collections.emptyList());
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
