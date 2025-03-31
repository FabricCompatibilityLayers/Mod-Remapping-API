package io.github.fabriccompatibiltylayers.modremappingapi.impl;

import java.nio.file.Path;

public class DefaultModCandidate extends ModCandidate {
    public DefaultModCandidate(String modName, Path file, Path original) {
        super(modName, modName, file, original);
    }
}
