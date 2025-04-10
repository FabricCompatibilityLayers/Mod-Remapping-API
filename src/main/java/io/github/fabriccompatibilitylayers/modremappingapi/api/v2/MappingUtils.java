package io.github.fabriccompatibilitylayers.modremappingapi.api.v2;

import io.github.fabriccompatibilitylayers.modremappingapi.impl.mappings.MappingsUtilsImpl;
import org.jetbrains.annotations.Nullable;

public interface MappingUtils {
    /**
     *
     * @param className original class name
     * @return remapped class name
     */
    static String mapClass(String contextId, String className) {
        return MappingsUtilsImpl.mapClass(MappingsUtilsImpl.getMappingsRegistry(contextId), className);
    }

    /**
     *
     * @param className remapped class name
     * @return original class name
     */
    static String unmapClass(String contextId, String className) {
        return MappingsUtilsImpl.unmapClass(MappingsUtilsImpl.getMappingsRegistry(contextId), className);
    }

    /**
     *
     * @param className original class name
     * @param fieldName
     * @param fieldDesc
     * @return
     */
    static ClassMember mapField(String contextId, String className, String fieldName, @Nullable String fieldDesc) {
        return MappingsUtilsImpl.mapField(MappingsUtilsImpl.getMappingsRegistry(contextId), className, fieldName, fieldDesc);
    }

    /**
     *
     * @param className remapped class name
     * @param fieldName
     * @param fieldDesc
     * @return
     */
    static ClassMember mapFieldFromRemappedClass(String contextId, String className, String fieldName, @Nullable String fieldDesc) {
        return MappingsUtilsImpl.mapFieldFromRemappedClass(MappingsUtilsImpl.getMappingsRegistry(contextId), className, fieldName, fieldDesc);
    }

    /**
     *
     * @param className original class name
     * @param methodName
     * @param methodDesc
     * @return
     */
    static ClassMember mapMethod(String contextId, String className, String methodName, String methodDesc) {
        return MappingsUtilsImpl.mapMethod(MappingsUtilsImpl.getMappingsRegistry(contextId), className, methodName, methodDesc);
    }

    /**
     *
     * @param className remapped class name
     * @param methodName
     * @param methodDesc
     * @return
     */
    static ClassMember mapMethodFromRemappedClass(String contextId, String className, String methodName, String methodDesc) {
        return MappingsUtilsImpl.mapMethodFromRemappedClass(MappingsUtilsImpl.getMappingsRegistry(contextId), className, methodName, methodDesc);
    }

    static ClassMember mapField(String contextId, Class<?> owner, String fieldName) {
        return MappingsUtilsImpl.mapField(MappingsUtilsImpl.getMappingsRegistry(contextId), owner, fieldName);
    }

    static ClassMember mapMethod(String contextId, Class<?> owner, String methodName, Class<?>[] parameterTypes) {
        return MappingsUtilsImpl.mapMethod(MappingsUtilsImpl.getMappingsRegistry(contextId), owner, methodName, parameterTypes);
    }

    /**
     *
     * @param desc original descriptor
     * @return remapped descriptor
     */
    static String mapDescriptor(String contextId, String desc) {
        return MappingsUtilsImpl.mapDescriptor(MappingsUtilsImpl.getMappingsRegistry(contextId), desc);
    }

    interface ClassMember {
        String getName();
        @Nullable String getDesc();
    }
}
