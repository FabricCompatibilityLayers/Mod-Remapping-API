package fr.catcore.modremapperapi.test.impl;

import fr.catcore.modremappingapi.api.v1.ModDiscoverer;
import fr.catcore.modremappingapi.api.v1.ModInfos;

import java.io.IOException;
import java.nio.file.Path;

public class MLLibDiscoverer implements ModDiscoverer {
    @Override
    public String getId() {
        return "ml-libs";
    }

    @Override
    public String getName() {
        return "ML Libraries";
    }

    @Override
    public String[] getModFolders() {
        return new String[]{"mods"};
    }

    @Override
    public String[] getModExtensions() {
        return new String[]{".zip", ".jar"};
    }

    @Override
    public boolean isMod(String fileName) {
        return false;
    }

    @Override
    public boolean isMod(String fileName, Path filePath) throws IOException {
        return false;
    }

    @Override
    public ModInfos parseModInfos(String fileName, Path filePath) throws IOException {
        return null;
    }

    @Override
    public boolean acceptAnyFile() {
        return true;
    }

    @Override
    public boolean acceptDirectories() {
        return true;
    }

    @Override
    public boolean addToClasspath() {
        return true;
    }

    @Override
    public boolean excludeEdits() {
        return true;
    }
}
