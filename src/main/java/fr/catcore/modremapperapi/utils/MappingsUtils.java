package fr.catcore.modremapperapi.utils;

import io.github.fabriccompatibiltylayers.modremappingapi.impl.MappingsUtilsImpl;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.tinyremapper.*;

@Deprecated
public class MappingsUtils {
    @Deprecated
    public static String getNativeNamespace() {
        return MappingsUtilsImpl.getNativeNamespace();
    }

    @Deprecated
    public static String getTargetNamespace() {
        return MappingsUtilsImpl.getTargetNamespace();
    }

    @Deprecated
    public static MappingTree getMinecraftMappings() {
        return MappingsUtilsImpl.getVanillaMappings();
    }

    @Deprecated
    public static IMappingProvider createProvider(MappingTree mappings) {
        return MappingsUtilsImpl.createProvider(mappings, getNativeNamespace(), getTargetNamespace());
    }
}
