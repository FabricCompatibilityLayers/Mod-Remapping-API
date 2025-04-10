package fr.catcore.modremapperapi.remapping;

import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingUtils;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.mappings.MappingsUtilsImpl;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.compatibility.v1.ModRemapperV1Context;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.mappingio.MappingVisitor;
import org.jetbrains.annotations.ApiStatus;

import java.io.*;
import java.util.*;

@Deprecated
public class RemapUtil {
    @Deprecated
    public static final List<String> MC_CLASS_NAMES = ModRemapperV1Context.INSTANCE.getMappingsRegistry().getVanillaClassNames();

    @Deprecated
    public static class MappingList extends ArrayList<MappingBuilder> {
        public MappingList() {
            super();
        }

        @Deprecated
        public MappingBuilder add(String obfuscated, String intermediary) {
            MappingBuilder builder = MappingBuilder.create(obfuscated, intermediary);
            this.add(builder);
            return builder;
        }

        @Deprecated
        public MappingBuilder add(String name) {
            MappingBuilder builder = MappingBuilder.create(name);
            this.add(builder);
            return builder;
        }

        @ApiStatus.Internal
        public void accept(MappingVisitor visitor) throws IOException {
            for (MappingBuilder builder : this) builder.accept(visitor);
        }
    }

    @Deprecated
    public static String getRemappedFieldName(Class<?> owner, String fieldName) {
        return MappingUtils.mapField(owner, fieldName).name;
    }

    @Deprecated
    public static String getRemappedMethodName(Class<?> owner, String methodName, Class<?>[] parameterTypes) {
        return MappingUtils.mapMethod(owner, methodName, parameterTypes).name;
    }

    /**
     * A shortcut to the Fabric Environment getter.
     */
    @Deprecated
    public static EnvType getEnvironment() {
        return FabricLoader.getInstance().getEnvironmentType();
    }

    @Deprecated
    public static String getNativeNamespace() {
        return MappingsUtilsImpl.getNativeNamespace();
    }
}
