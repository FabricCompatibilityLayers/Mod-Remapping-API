package io.github.fabriccompatibiltylayers.modremappingapi.impl;

import java.nio.file.Path;

public class DefaultModEntry extends ModEntry {
    protected DefaultModEntry(String modName, Path file, Path original) {
        super(modName, modName, file, original);
    }

    @Override
    String getType() {
        return "Possible";
    }
}
