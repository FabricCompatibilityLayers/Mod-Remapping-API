package fr.catcore.modremapperapi.impl;

import fr.catcore.modremapperapi.api.v1.ModDiscoverer;
import fr.catcore.modremapperapi.api.v1.ModEntry;
import fr.catcore.modremapperapi.api.v1.ModInfos;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class ModEntryImpl implements ModEntry {
    private final Path filePath;
    private final @Nullable String modEntry;
    private final ModInfos infos;
    private final ModDiscoverer modDiscoverer;

    public ModEntryImpl(Path filePath, @Nullable String modEntry, ModInfos infos, ModDiscoverer modDiscoverer) {
        this.filePath = filePath;
        this.modEntry = modEntry;
        this.infos = infos;
        this.modDiscoverer = modDiscoverer;
    }

    @Override
    public Path getFilePath() {
        return this.filePath;
    }

    @Override
    public ModInfos getInfos() {
        return this.infos;
    }

    @Override
    public ModDiscoverer getModDiscoverer() {
        return this.modDiscoverer;
    }
}
