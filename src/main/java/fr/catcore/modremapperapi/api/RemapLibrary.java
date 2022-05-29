package fr.catcore.modremapperapi.api;

import java.util.List;

public class RemapLibrary {
    public final String url;
    public final List<String> toExclude;
    public final String fileName;

    public RemapLibrary(String url, List<String> toExclude, String fileName) {
        this.url = url;
        this.toExclude = toExclude;
        this.fileName = fileName;
    }
}
