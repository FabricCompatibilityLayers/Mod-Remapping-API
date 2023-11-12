package fr.catcore.modremapperapi.api.v1;

import java.nio.file.Path;

public interface ModEntry {
    Path getFilePath();

    ModInfos getInfos();

    ModDiscoverer getModDiscoverer();
}
