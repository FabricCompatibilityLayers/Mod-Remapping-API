package io.github.fabriccompatibilitylayers.modremappingapi.impl;

import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.ModCandidate;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class DefaultModCandidate implements ModCandidate {
    private final String id;
    private final Path path;

    public DefaultModCandidate(Path path) {
        this.id = path.getFileName().toString().replace(".jar", "").replace(".zip", "").replace(" ", "_");
        this.path = path;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Path getPath() {
        return path;
    }

    @Override
    public String getType() {
        return "default";
    }

    @Override
    public @Nullable String getAccessWidenerPath() {
        return null;
    }

    @Override
    public @Nullable ModCandidate getParent() {
        return null;
    }
}
