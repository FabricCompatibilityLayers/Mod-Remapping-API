package io.github.fabriccompatibilitylayers.modremappingapi.api.v2;

import java.util.List;

public interface ModRemapper {
    String getContextId();

    List<ModDiscoverer> getModDiscoverers();
}
