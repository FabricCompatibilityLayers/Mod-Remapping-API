package fr.catcore.modremapperapi.impl;

import fr.catcore.modremapperapi.api.v1.ModDiscoverer;
import fr.catcore.modremapperapi.api.v1.ModInfos;

import java.io.IOException;
import java.nio.file.Path;

public class BuiltInDiscoverer implements ModDiscoverer {
    @Override
    public String getId() {
        return "builtin";
    }

    @Override
    public String getName() {
        return "Builtin";
    }

    @Override
    public String[] getModFolders() {
        return new String[]{"remap-me"};
    }

    @Override
    public String[] getModExtensions() {
        return new String[]{".jar", ".zip"};
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
    public boolean addToClasspath() {
        return false;
    }

    @Override
    public boolean excludeEdits() {
        return false;
    }
}
