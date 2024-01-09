package fr.catcore.modremappingapi.impl;

import fr.catcore.modremappingapi.api.v1.ModInfos;

import java.util.Locale;

public class LibraryModInfo implements ModInfos {
    private final String fileName;
    public LibraryModInfo(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String getName() {
        return fileName;
    }

    @Override
    public String getId() {
        return fileName.toLowerCase(Locale.ENGLISH);
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getType() {
        return "library";
    }
}
