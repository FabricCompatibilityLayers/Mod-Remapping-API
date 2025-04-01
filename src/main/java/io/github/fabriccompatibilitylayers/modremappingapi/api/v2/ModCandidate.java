package io.github.fabriccompatibilitylayers.modremappingapi.api.v2;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public interface ModCandidate {
    String getId();
    Path getPath();
    String getType();
    @Nullable String getAccessWidenerPath();
    @Nullable ModCandidate getParent();
}
