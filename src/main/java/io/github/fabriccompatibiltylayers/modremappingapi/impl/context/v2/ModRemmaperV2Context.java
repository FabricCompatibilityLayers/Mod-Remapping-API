package io.github.fabriccompatibiltylayers.modremappingapi.impl.context.v2;

import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.ModRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.LibraryHandler;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.ModCandidate;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.context.BaseModRemapperContext;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.context.MappingsRegistryInstance;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.context.ModRemapperContext;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings.MappingsRegistry;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.RemappingFlags;
import net.fabricmc.tinyremapper.TinyRemapper;

import java.nio.file.Path;
import java.util.*;

public class ModRemmaperV2Context extends BaseModRemapperContext<ModRemapper> {
    private final List<ModRemapper> remappers;
    private final Set<RemappingFlags> remapFlags = new HashSet<>();
    private final Map<String, List<String>> mixin2TargetMap = new HashMap<>();
    private final MappingsRegistryInstance mappingsRegistry = new MappingsRegistryInstance();
    private final LibraryHandler libraryHandler = new LibraryHandler();

    public ModRemmaperV2Context(String id, List<ModRemapper> remappers) {
        super(id);
        this.remappers = remappers;
    }

    @Override
    public void init() {

    }

    @Override
    public void remapMods(Map<ModCandidate, Path> pathMap) {

    }

    @Override
    public void afterRemap() {

    }

    @Override
    public List<ModRemapper> discoverMods(boolean remapClassEdits) {
        for (ModRemapper remapper : remappers) {

        }
    }

    @Override
    public void gatherRemappers() {

    }

    @Override
    public Map<String, List<String>> getMixin2TargetMap() {
        return mixin2TargetMap;
    }

    @Override
    public MappingsRegistry getMappingsRegistry() {
        return mappingsRegistry;
    }

    @Override
    public void addToRemapperBuilder(TinyRemapper.Builder builder) {

    }

    @Override
    public Set<RemappingFlags> getRemappingFlags() {
        return remapFlags;
    }

    @Override
    public LibraryHandler getLibraryHandler() {
        return libraryHandler;
    }

    public List<ModRemapper> getRemappers() {
        return remappers;
    }
}
