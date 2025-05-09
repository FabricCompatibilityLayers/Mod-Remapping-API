package io.github.fabriccompatibilitylayers.modremappingapi.api.v2;

import io.github.fabriccompatibilitylayers.modremappingapi.impl.defaults.DefaultMappingsConfig;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Supplier;

public interface MappingsConfig {
    @Nullable String getSourceNamespace();
    @Nullable Supplier<String> getExtraMappings();
    Map<String, String> getRenamingMap();
    @Nullable String getDefaultPackage();

    static MappingsConfig defaultConfig() {
        return new DefaultMappingsConfig();
    }
}
