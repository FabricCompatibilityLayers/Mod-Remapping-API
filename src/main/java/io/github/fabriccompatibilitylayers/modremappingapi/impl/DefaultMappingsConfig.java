package io.github.fabriccompatibilitylayers.modremappingapi.impl;

import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.MappingsConfig;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

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
        return new HashMap<>();
    }

    @Override
    public @Nullable String getDefaultPackage() {
        return null;
    }
}
