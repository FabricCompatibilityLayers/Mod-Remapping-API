package fr.catcore.modremapperapi.api.v1;

import java.nio.file.Path;

public interface ModDiscoverer {
    String getId();
    String getName();

    String[] getModFolders();

    String[] getModExtensions();

    boolean isMod(String fileName);
    boolean isMod(String fileName, Path filePath);

    default boolean acceptAnyFile() {
        return false;
    }

    default boolean acceptDirectories() {
        return false;
    }

    default String getDirectoryExtension() {
        return ".zip";
    }

    boolean addToClasspath();

    boolean excludeEdits();
}
