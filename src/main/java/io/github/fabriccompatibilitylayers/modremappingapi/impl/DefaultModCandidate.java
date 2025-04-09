package io.github.fabriccompatibilitylayers.modremappingapi.impl;

import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.ModCandidate;
import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.ModDiscovererConfig;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class DefaultModCandidate implements ModCandidate {
    private String sanitizedFileName, id;
    private Path path;
    private final ModDiscovererConfig discovererConfig;
    private Path destination;

    public DefaultModCandidate(Path path, ModDiscovererConfig discovererConfig) {
        this.sanitizedFileName = path.getFileName().toString().replace(" ", "_");
        this.id = this.sanitizedFileName.replace(".jar", "").replace(".zip", "");
        this.path = path;
        this.discovererConfig = discovererConfig;

        if (!this.sanitizedFileName.endsWith(".jar") && !this.sanitizedFileName.endsWith(".zip")) {
            this.sanitizedFileName += ".zip";
        }
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

    @Override
    public @Nullable String getVersion() {
        return null;
    }

    @Override
    public @Nullable String getParentSubPath() {
        return null;
    }

    @Override
    public String getDestinationName() {
        return this.sanitizedFileName;
    }

    @Override
    public ModDiscovererConfig getDiscovererConfig() {
        return discovererConfig;
    }

    @Override
    public void setAccessWidener(byte[] data) {

    }

    @Override
    public byte @Nullable [] getAccessWidener() {
        return null;
    }

    @Override
    public void setDestination(Path destination) {
        this.destination = destination;
    }

    @Override
    public Path getDestination() {
        return this.destination;
    }

    @Override
    public void setPath(Path path) {
        this.path = path;
    }


}
