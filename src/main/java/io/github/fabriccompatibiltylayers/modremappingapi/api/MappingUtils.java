package io.github.fabriccompatibiltylayers.modremappingapi.api;

import io.github.fabriccompatibiltylayers.modremappingapi.impl.MappingsUtilsImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface MappingUtils {
    static String mapClass(String className) {
        return MappingsUtilsImpl.mapClass(className);
    }

    static String unmapClass(String className) {
        return MappingsUtilsImpl.unmapClass(className);
    }

    static MappingUtils.ClassMember mapField(String className, String fieldName, @Nullable String fieldDesc) {
        return MappingsUtilsImpl.mapField(className, fieldName, fieldDesc);
    }

    static MappingUtils.ClassMember mapFieldFromRemappedClass(String className, String fieldName, @Nullable String fieldDesc) {
        return MappingsUtilsImpl.mapFieldFromRemappedClass(className, fieldName, fieldDesc);
    }

    static MappingUtils.ClassMember mapMethod(String className, String methodName, String methodDesc) {
        return MappingsUtilsImpl.mapMethod(className, methodName, methodDesc);
    }

    static MappingUtils.ClassMember mapMethodFromRemappedClass(String className, String methodName, String methodDesc) {
        return MappingsUtilsImpl.mapMethodFromRemappedClass(className, methodName, methodDesc);
    }

    static MappingUtils.ClassMember mapField(Class<?> owner, String fieldName) {
        return MappingsUtilsImpl.mapField(owner, fieldName);
    }

    static MappingUtils.ClassMember mapMethod(Class<?> owner, String methodName, Class<?>[] parameterTypes) {
        return MappingsUtilsImpl.mapMethod(owner, methodName, parameterTypes);
    }

    class ClassMember {
        public final @NotNull String name;
        public final @Nullable String desc;

        public ClassMember(@NotNull String name, @Nullable String desc) {
            assert name != null;

            this.name = name;
            this.desc = desc;
        }
    }
}
