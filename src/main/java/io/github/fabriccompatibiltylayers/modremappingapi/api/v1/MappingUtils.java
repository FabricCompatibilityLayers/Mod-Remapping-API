package io.github.fabriccompatibiltylayers.modremappingapi.api.v1;

import io.github.fabriccompatibiltylayers.modremappingapi.impl.MappingsUtilsImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @deprecated Use {@link io.github.fabriccompatibilitylayers.modremappingapi.api.v2.MappingUtils} instead.
 */
@Deprecated
public interface MappingUtils {
    /**
     *
     * @param className original class name
     * @return remapped class name
     */
    static String mapClass(String className) {
        return MappingsUtilsImpl.mapClass(MappingsUtilsImpl.getV1Registry(), className);
    }

    /**
     *
     * @param className remapped class name
     * @return original class name
     */
    static String unmapClass(String className) {
        return MappingsUtilsImpl.unmapClass(MappingsUtilsImpl.getV1Registry(), className);
    }

    /**
     *
     * @param className original class name
     * @param fieldName
     * @param fieldDesc
     * @return
     */
    static ClassMember mapField(String className, String fieldName, @Nullable String fieldDesc) {
        return MappingsUtilsImpl.mapField(MappingsUtilsImpl.getV1Registry(), className, fieldName, fieldDesc);
    }

    /**
     *
     * @param className remapped class name
     * @param fieldName
     * @param fieldDesc
     * @return
     */
    static ClassMember mapFieldFromRemappedClass(String className, String fieldName, @Nullable String fieldDesc) {
        return MappingsUtilsImpl.mapFieldFromRemappedClass(MappingsUtilsImpl.getV1Registry(), className, fieldName, fieldDesc);
    }

    /**
     *
     * @param className original class name
     * @param methodName
     * @param methodDesc
     * @return
     */
    static ClassMember mapMethod(String className, String methodName, String methodDesc) {
        return MappingsUtilsImpl.mapMethod(MappingsUtilsImpl.getV1Registry(), className, methodName, methodDesc);
    }

    /**
     *
     * @param className remapped class name
     * @param methodName
     * @param methodDesc
     * @return
     */
    static ClassMember mapMethodFromRemappedClass(String className, String methodName, String methodDesc) {
        return MappingsUtilsImpl.mapMethodFromRemappedClass(MappingsUtilsImpl.getV1Registry(), className, methodName, methodDesc);
    }

    static ClassMember mapField(Class<?> owner, String fieldName) {
        return MappingsUtilsImpl.mapField(MappingsUtilsImpl.getV1Registry(), owner, fieldName);
    }

    static ClassMember mapMethod(Class<?> owner, String methodName, Class<?>[] parameterTypes) {
        return MappingsUtilsImpl.mapMethod(MappingsUtilsImpl.getV1Registry(), owner, methodName, parameterTypes);
    }

    /**
     *
     * @param desc original descriptor
     * @return remapped descriptor
     */
    static String mapDescriptor(String desc) {
        return MappingsUtilsImpl.mapDescriptor(MappingsUtilsImpl.getV1Registry(), desc);
    }

    class ClassMember implements io.github.fabriccompatibilitylayers.modremappingapi.api.v2.MappingUtils.ClassMember {
        public final @NotNull String name;
        public final @Nullable String desc;

        public ClassMember(@NotNull String name, @Nullable String desc) {
            this.name = name;
            this.desc = desc;
        }

        @Override
        public @NotNull String getName() {
            return this.name;
        }

        @Override
        public @Nullable String getDesc() {
            return this.desc;
        }
    }
}
