package io.github.fabriccompatibiltylayers.modremappingapi.impl;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class ModCandidate {
    public final String modName;
    public final @Nullable String accessWidenerName;

    public final Path file;
    public final Path original;

    public byte[] accessWidener;

    public ModCandidate(String modName, @Nullable String accessWidenerName, Path file, Path original) {
        this.modName = modName;
        this.accessWidenerName = accessWidenerName;
        this.file = file;
        this.original = original;
    }

    public ModCandidate(String modName, Path file, Path original) {
        this(modName, null, file, original);
    }
}
