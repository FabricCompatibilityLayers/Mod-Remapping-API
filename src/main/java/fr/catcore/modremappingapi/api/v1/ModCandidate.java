package fr.catcore.modremappingapi.api.v1;

import java.nio.file.Path;

public interface ModCandidate {
    Path getFilePath();

    ModInfos getInfos();

    ModDiscoverer getModDiscoverer();
}
