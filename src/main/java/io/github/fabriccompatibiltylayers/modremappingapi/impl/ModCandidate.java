package io.github.fabriccompatibiltylayers.modremappingapi.impl;

import java.nio.file.Path;

public abstract class ModCandidate {
    public final String modName;
    public final String modId;

    public final Path file;
    public final Path original;

    protected ModCandidate(String modName, String modId, Path file, Path original) {
        this.modName = modName;
        this.modId = modId;
        this.file = file;
        this.original = original;
    }
}
