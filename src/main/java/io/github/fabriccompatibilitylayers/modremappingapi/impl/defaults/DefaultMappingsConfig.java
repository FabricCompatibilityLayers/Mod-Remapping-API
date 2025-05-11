package io.github.fabriccompatibilitylayers.modremappingapi.impl.defaults;

import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.MappingsConfig;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@ApiStatus.Internal
public class DefaultMappingsConfig implements MappingsConfig {
    @Override
    public String getSourceNamespace() {
        return "official";
    }

    @Override
    public Supplier<String> getExtraMappings() {
        return null;
    }

    @Override
    public Map<String, String> getRenamingMap() {
        return Map.of();
    }

    @Override
    public @Nullable String getDefaultPackage() {
        return null;
    }
}
