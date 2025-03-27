package io.github.fabriccompatibiltylayers.modremappingapi.impl.context;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseModRemapperContext implements ModRemapperContext {
    private static final Map<String, ModRemapperContext> REGISTRY = new HashMap<>();

    public BaseModRemapperContext(String id) {
        REGISTRY.put(id, this);
    }

    public static ModRemapperContext get(String id) {
        return REGISTRY.get(id);
    }
}
