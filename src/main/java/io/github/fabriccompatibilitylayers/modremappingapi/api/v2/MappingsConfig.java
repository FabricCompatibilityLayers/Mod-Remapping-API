package io.github.fabriccompatibilitylayers.modremappingapi.api.v2;

import io.github.fabriccompatibilitylayers.modremappingapi.impl.DefaultMappingsConfig;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.Map;
import java.util.function.Supplier;

public interface MappingsConfig {
    @Nullable String getSourceNamespace();
    @Nullable Supplier<InputStream> getExtraMappings();
    Map<String, String> getRenamingMap();

    static MappingsConfig defaultConfig() {
        return new DefaultMappingsConfig();
    }
}
