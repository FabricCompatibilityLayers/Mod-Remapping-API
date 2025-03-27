package io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper;

import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApiStatus.Internal
public class MixinRemappingHelper {
    public static final Map<String, List<String>> MIXIN2TARGETMAP = new HashMap<>();
}
