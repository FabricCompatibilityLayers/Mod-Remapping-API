package fr.catcore.modremappingapi.impl;

import fr.catcore.modremappingapi.api.v1.ModDiscoverer;
import fr.catcore.modremappingapi.api.v1.ModCandidate;
import fr.catcore.modremappingapi.api.v1.ModInfos;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class ModCandidateImpl implements ModCandidate {
    private final Path filePath;
    private final @Nullable String modEntry;
    private final ModInfos infos;
    private final ModDiscoverer modDiscoverer;
    private final Path outDir;

    public ModCandidateImpl(Path filePath, @Nullable String modEntry, ModInfos infos, ModDiscoverer modDiscoverer, Path outDir) {
        this.filePath = filePath;
        this.modEntry = modEntry;
        this.infos = infos;
        this.modDiscoverer = modDiscoverer;
        this.outDir = outDir;
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
