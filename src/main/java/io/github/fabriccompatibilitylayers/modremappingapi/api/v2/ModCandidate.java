package io.github.fabriccompatibilitylayers.modremappingapi.api.v2;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public interface ModCandidate {
    String getId();
    Path getPath();
    String getType();
    @Nullable String getAccessWidenerPath();
    @Nullable ModCandidate getParent();
    @Nullable String getVersion();
    @Nullable String getParentSubPath();
    String getDestinationName();
    ModDiscovererConfig getDiscovererConfig();
    void setAccessWidener(byte[] data);
    byte @Nullable [] getAccessWidener();
    void setDestination(Path destination);
    @Nullable Path getDestination();
    void setPath(Path path);
}
