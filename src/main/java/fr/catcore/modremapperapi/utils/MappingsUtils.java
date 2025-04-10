package fr.catcore.modremapperapi.utils;

import io.github.fabriccompatibilitylayers.modremappingapi.impl.mappings.MappingsUtilsImpl;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.mappings.MappingTreeHelper;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.mappings.MappingsRegistry;
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
        return MappingsRegistry.VANILLA;
    }

    @Deprecated
    public static IMappingProvider createProvider(MappingTree mappings) {
        return MappingTreeHelper.createMappingProvider(mappings, getNativeNamespace(), getTargetNamespace());
    }
}
