package io.github.fabriccompatibiltylayers.modremappingapi.impl.context;

import net.fabricmc.tinyremapper.InputTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MixinData {
    private final Map<String, List<String>> mixin2TargetMap = new HashMap<>();
    private final Map<String, Map<String, String>> mixinRefmapData = new HashMap<>();
    private final List<InputTag> hardMixins = new ArrayList<>();

    public Map<String, List<String>> getMixin2TargetMap() {
        return mixin2TargetMap;
    }

    public Map<String, Map<String, String>> getMixinRefmapData() {
        return mixinRefmapData;
    }

    public List<InputTag> getHardMixins() {
        return hardMixins;
    }
}
