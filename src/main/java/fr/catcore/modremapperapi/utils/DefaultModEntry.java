package fr.catcore.modremapperapi.utils;

import net.fabricmc.loader.impl.metadata.LoaderModMetadata;

import java.io.File;

public class DefaultModEntry extends ModEntry {
    protected DefaultModEntry(String modName, File file, File original) {
        super(modName, modName, file, original);
    }

    @Override
    LoaderModMetadata createModMetadata() {
        return null;
    }

    @Override
    String getType() {
        return "Possible";
    }
}
