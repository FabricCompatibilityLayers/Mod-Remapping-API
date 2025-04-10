package io.github.fabriccompatibilitylayers.modremappingapi.impl.context;

import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.Map;

@ApiStatus.Internal
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
