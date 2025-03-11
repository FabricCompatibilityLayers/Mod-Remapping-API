package io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.resource;

import net.fabricmc.tinyremapper.TinyRemapper;

import java.util.HashMap;
import java.util.Map;

public class RefmapJson {
    public final Map<String, Map<String, String>> mappings = new HashMap<>();
    public final Map<String, Map<String, Map<String, String>>> data = new HashMap<>();

    public void remap(RefmapRemapper remapper, TinyRemapper tiny) {
        this.data.clear();

        // for every class entry we need to remap the mappings of references
        this.mappings.forEach((mixinClass, refmapEntryMap) ->
                refmapEntryMap.replaceAll((originalName, oldMappedName) ->
                        remapper.mapRefMapEntry(mixinClass, oldMappedName, tiny)
                )
        );

        this.data.put("named:intermediary", this.mappings);

    }
}
