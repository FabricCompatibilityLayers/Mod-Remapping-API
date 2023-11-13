package fr.catcore.modremapperapi.api.v1;

import java.nio.file.Path;

public interface ModCandidate {
    Path getFilePath();

    ModInfos getInfos();

    ModDiscoverer getModDiscoverer();
}
