package io.github.fabriccompatibiltylayers.modremappingapi.api;

import io.github.fabriccompatibiltylayers.modremappingapi.impl.MappingsUtilsImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @deprecated Deprecated in favor of {@link io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingUtils}
 */
@Deprecated
public interface MappingUtils {
    /**
     * @deprecated Deprecated in favor of {@link io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingUtils#mapClass(String)}
     * @param className original class name
     * @return remapped class name
     */
    @Deprecated
    static String mapClass(String className) {
        return io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingUtils.mapClass(className);
    }

    /**
     * @deprecated Deprecated in favor of {@link io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingUtils#unmapClass(String)}
     * @param className remapped class name
     * @return original class name
     */
    @Deprecated
    static String unmapClass(String className) {
        return io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingUtils.unmapClass(className);
    }

    /**
     * @deprecated Deprecated in favor of {@link io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingUtils#mapField(String, String, String)}
     * @param className original class name
     * @param fieldName
     * @param fieldDesc
     * @return
     */
    @Deprecated
    static MappingUtils.ClassMember mapField(String className, String fieldName, @Nullable String fieldDesc) {
        return MappingsUtilsImpl.mapField(className, fieldName, fieldDesc);
    }

    /**
     * @deprecated Deprecated in favor of {@link io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingUtils#mapFieldFromRemappedClass(String, String, String)}
     * @param className remapped class name
     * @param fieldName
     * @param fieldDesc
     * @return
     */
    @Deprecated
    static MappingUtils.ClassMember mapFieldFromRemappedClass(String className, String fieldName, @Nullable String fieldDesc) {
        return MappingsUtilsImpl.mapFieldFromRemappedClass(className, fieldName, fieldDesc);
    }

    /**
     * @deprecated Deprecated in favor of {@link io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingUtils#mapMethod(String, String, String)}
     * @param className original class name
     * @param methodName
     * @param methodDesc
     * @return
     */
    @Deprecated
    static MappingUtils.ClassMember mapMethod(String className, String methodName, String methodDesc) {
        return MappingsUtilsImpl.mapMethod(className, methodName, methodDesc);
    }

    /**
     * @deprecated Deprecated in favor of {@link io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingUtils#mapMethodFromRemappedClass(String, String, String)}
     * @param className remapped class name
     * @param methodName
     * @param methodDesc
     * @return
     */
    @Deprecated
    static MappingUtils.ClassMember mapMethodFromRemappedClass(String className, String methodName, String methodDesc) {
        return MappingsUtilsImpl.mapMethodFromRemappedClass(className, methodName, methodDesc);
    }

    /**
     * @deprecated Deprecated in favor of {@link io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingUtils#mapField(Class, String)}
     * @param owner
     * @param fieldName
     * @return
     */
    @Deprecated
    static MappingUtils.ClassMember mapField(Class<?> owner, String fieldName) {
        return MappingsUtilsImpl.mapField(owner, fieldName);
    }

    /**
     * @deprecated Deprecated in favor of {@link io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingUtils#mapMethod(Class, String, Class[])}
     * @param owner
     * @param methodName
     * @param parameterTypes
     * @return
     */
    @Deprecated
    static MappingUtils.ClassMember mapMethod(Class<?> owner, String methodName, Class<?>[] parameterTypes) {
        return MappingsUtilsImpl.mapMethod(owner, methodName, parameterTypes);
    }

    /**
     * @deprecated Deprecated in favor of {@link io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingUtils#mapDescriptor(String)}
     * @param desc original descriptor
     * @return remapped descriptor
     */
    @Deprecated
    static String mapDescriptor(String desc) {
        return io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingUtils.mapDescriptor(desc);
    }

    /**
     * @deprecated Deprecated in favor of {@link io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingUtils.ClassMember}
     */
    @Deprecated
    class ClassMember extends io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingUtils.ClassMember {
        public ClassMember(@NotNull String name, @Nullable String desc) {
            super(name, desc);
        }
    }
}
