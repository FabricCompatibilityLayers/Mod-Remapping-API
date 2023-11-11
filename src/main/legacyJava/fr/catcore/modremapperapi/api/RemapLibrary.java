package fr.catcore.modremapperapi.api;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RemapLibrary {
    public final String url;
    public final Path path;
    public final List<String> toExclude;
    public final String fileName;

    public RemapLibrary(String url, List<String> toExclude, String fileName) {
        this.url = url;
        this.path = null;
        this.toExclude = toExclude;
        this.fileName = fileName;
    }

    public RemapLibrary(Path path, List<String> toExclude, String fileName) {
        this.url = "";
        this.path = path;
        this.toExclude = toExclude;
        this.fileName = fileName;
    }

    public RemapLibrary(Path path, String fileName) {
        this.url = "";
        this.path = path;
        this.toExclude = new ArrayList<>();
        this.fileName = fileName;
    }
}
