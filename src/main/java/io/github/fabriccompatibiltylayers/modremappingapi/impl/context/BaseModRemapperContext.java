package io.github.fabriccompatibiltylayers.modremappingapi.impl.context;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseModRemapperContext<T> implements ModRemapperContext<T> {
    private static final Map<String, ModRemapperContext> REGISTRY = new HashMap<>();

    public final String contextId;

    public BaseModRemapperContext(String id) {
        REGISTRY.put(id, this);
        this.contextId = id;
    }

    public static ModRemapperContext get(String id) {
        return REGISTRY.get(id);
    }

    @Override
    public String getId() {
        return this.contextId;
    }
}
