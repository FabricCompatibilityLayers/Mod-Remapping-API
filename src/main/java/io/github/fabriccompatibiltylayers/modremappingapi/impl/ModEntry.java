package io.github.fabriccompatibiltylayers.modremappingapi.impl;

import java.nio.file.Path;

public abstract class ModEntry {
    public final String modName;
    public final String modId;

    public final Path file;
    public final Path original;

    protected ModEntry(String modName, String modId, Path file, Path original) {
        this.modName = modName;
        this.modId = modId;
        this.file = file;
        this.original = original;
    }

    abstract String getType();
}
