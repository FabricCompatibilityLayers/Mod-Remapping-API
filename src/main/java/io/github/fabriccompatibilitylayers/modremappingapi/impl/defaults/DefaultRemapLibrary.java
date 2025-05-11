package io.github.fabriccompatibilitylayers.modremappingapi.impl.defaults;

import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.RemapLibrary;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@ApiStatus.Internal
public record DefaultRemapLibrary(
        @Nullable String url,
        @Nullable Path path,
        String fileName,
        List<String> toExclude
) implements RemapLibrary {

    public DefaultRemapLibrary(@Nullable String url, String fileName, List<String> toExclude) {
        this(url, null, fileName, toExclude);
    }

    public DefaultRemapLibrary(@Nullable Path path, String fileName, List<String> toExclude) {
        this(null, path, fileName, toExclude);
    }

    public DefaultRemapLibrary(@Nullable Path path, String fileName) {
        this(null, path, fileName, Collections.emptyList());
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
