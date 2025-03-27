package io.github.fabriccompatibiltylayers.modremappingapi.impl;

import io.github.fabriccompatibiltylayers.modremappingapi.api.MappingUtils;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.context.v1.ModRemapperV1Context;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings.MappingsRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.mappingio.tree.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.*;

@ApiStatus.Internal
public class MappingsUtilsImpl {
    public static String getTargetNamespace() {
        return FabricLoader.getInstance().getMappingResolver().getCurrentRuntimeNamespace();
    }

    public static String getNativeNamespace() {
        if (ModRemappingAPIImpl.BABRIC) {
            return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT ? "client" : "server";
        }

        return "official";
    }

    public static MappingsRegistry getV1Registry() {
        return ModRemapperV1Context.INSTANCE.getMappingsRegistry();
    }

    public static String mapClass(MappingsRegistry registry, String className) {
        int srcNamespace = registry.getFullMappings().getNamespaceId(registry.getSourceNamespace());
        int targetNamespace = registry.getFullMappings().getNamespaceId(registry.getTargetNamespace());

        return registry.getFullMappings().mapClassName(className, srcNamespace, targetNamespace);
    }

    public static String unmapClass(MappingsRegistry registry, String className) {
        int srcNamespace = registry.getFullMappings().getNamespaceId(registry.getTargetNamespace());
        int targetNamespace = registry.getFullMappings().getNamespaceId(registry.getSourceNamespace());

        return registry.getFullMappings().mapClassName(className, srcNamespace, targetNamespace);
    }

    public static MappingUtils.ClassMember mapField(MappingsRegistry registry, String className, String fieldName, @Nullable String fieldDesc) {
        int srcNamespace = registry.getFullMappings().getNamespaceId(registry.getSourceNamespace());
        int targetNamespace = registry.getFullMappings().getNamespaceId(registry.getTargetNamespace());

        MappingTree.FieldMapping fieldMapping = registry.getFullMappings().getField(className, fieldName, fieldDesc, srcNamespace);

        return mapMember(fieldName, fieldDesc, targetNamespace, fieldMapping);
    }

    public static MappingUtils.ClassMember mapFieldFromRemappedClass(MappingsRegistry registry, String className, String fieldName, @Nullable String fieldDesc) {
        int srcNamespace = registry.getFullMappings().getNamespaceId(registry.getSourceNamespace());
        int targetNamespace = registry.getFullMappings().getNamespaceId(registry.getTargetNamespace());

        MappingTree.ClassMapping classMapping = registry.getFullMappings().getClass(className, targetNamespace);
        if (classMapping == null) return new MappingUtils.ClassMember(fieldName, fieldDesc);

        MappingTree.FieldMapping fieldMapping = classMapping.getField(fieldName, fieldDesc, srcNamespace);
        return mapMember(fieldName, fieldDesc, targetNamespace, fieldMapping);
    }

    public static MappingUtils.ClassMember mapMethod(MappingsRegistry registry, String className, String methodName, String methodDesc) {
        int srcNamespace = registry.getFullMappings().getNamespaceId(registry.getSourceNamespace());
        int targetNamespace = registry.getFullMappings().getNamespaceId(registry.getTargetNamespace());

        MappingTree.MethodMapping methodMapping = registry.getFullMappings().getMethod(className, methodName, methodDesc, srcNamespace);

        if (methodMapping == null) {
            MappingTree.ClassMapping classMapping = registry.getFullMappings().getClass(className, srcNamespace);
            if (classMapping != null) methodMapping = mapMethodWithPartialDesc(classMapping, methodName, methodDesc, srcNamespace);
        }

        return mapMember(methodName, methodDesc, targetNamespace, methodMapping);
    }

    public static MappingUtils.ClassMember mapMethodFromRemappedClass(MappingsRegistry registry, String className, String methodName, String methodDesc) {
        int srcNamespace = registry.getFullMappings().getNamespaceId(registry.getSourceNamespace());
        int targetNamespace = registry.getFullMappings().getNamespaceId(registry.getTargetNamespace());

        MappingTree.ClassMapping classMapping = registry.getFullMappings().getClass(className, targetNamespace);
        if (classMapping == null) return new MappingUtils.ClassMember(methodName, methodDesc);

        MappingTree.MethodMapping methodMapping = classMapping.getMethod(methodName, methodDesc, srcNamespace);

        if (methodMapping == null) methodMapping = mapMethodWithPartialDesc(classMapping, methodName, methodDesc, srcNamespace);

        return mapMember(methodName, methodDesc, targetNamespace, methodMapping);
    }

    private static MappingTree.MethodMapping mapMethodWithPartialDesc(MappingTree.ClassMapping classMapping, String methodName, String methodDesc, int namespace) {
        for (MappingTree.MethodMapping methodMapping : classMapping.getMethods()) {
            String name = methodMapping.getName(namespace);
            String desc = methodMapping.getDesc(namespace);

            if (name != null && name.equals(methodName) && desc != null && desc.startsWith(methodDesc)) {
                return methodMapping;
            }
        }

        return null;
    }

    @NotNull
    private static MappingUtils.ClassMember mapMember(String memberName, @Nullable String memberDesc, int targetNamespace, MappingTree.MemberMapping memberMapping) {
        if (memberMapping == null) return new MappingUtils.ClassMember(memberName, memberDesc);

        String remappedName = memberMapping.getName(targetNamespace);
        String remappedDesc = memberMapping.getDesc(targetNamespace);

        return new MappingUtils.ClassMember(
                remappedName == null ? memberName : remappedName,
                remappedDesc == null ? memberDesc : remappedDesc
        );
    }

    public static MappingUtils.ClassMember mapField(MappingsRegistry registry, Class<?> owner, String fieldName) {
        int srcNamespace = registry.getFullMappings().getNamespaceId(registry.getSourceNamespace());
        int targetNamespace = registry.getFullMappings().getNamespaceId(registry.getTargetNamespace());
        MappingTree.ClassMapping classMapping = registry.getFullMappings().getClass(owner.getName().replace(".", "/"), targetNamespace);

        if (classMapping != null) {
            MappingTree.FieldMapping fieldMapping = classMapping.getField(fieldName, null, srcNamespace);

            if (fieldMapping != null) {
                return mapMember(fieldName, null, targetNamespace, fieldMapping);
            }
        }

        if (owner.getSuperclass() != null) {
            return mapField(registry, owner.getSuperclass(), fieldName);
        }

        return new MappingUtils.ClassMember(fieldName, null);
    }

    public static MappingUtils.ClassMember mapMethod(MappingsRegistry registry, Class<?> owner, String methodName, Class<?>[] parameterTypes) {
        String argDesc = classTypeToDescriptor(parameterTypes);

        int srcNamespace = registry.getFullMappings().getNamespaceId(registry.getSourceNamespace());
        int targetNamespace = registry.getFullMappings().getNamespaceId(registry.getTargetNamespace());
        MappingTree.ClassMapping classMapping = registry.getFullMappings().getClass(owner.getName().replace(".", "/"), targetNamespace);

        if (classMapping != null) {
            for (MappingTree.MethodMapping methodDef : classMapping.getMethods()) {
                String methodSubName = methodDef.getName(srcNamespace);

                if (Objects.equals(methodSubName, methodName)) {
                    String methodDescriptor = methodDef.getDesc(registry.getTargetNamespace());

                    if (methodDescriptor != null && methodDescriptor.startsWith(argDesc)) {
                        return new MappingUtils.ClassMember(
                                methodDef.getName(registry.getTargetNamespace()),
                                methodDescriptor
                        );
                    }
                }
            }
        }

        if (owner.getSuperclass() != null) {
            return mapMethod(registry, owner.getSuperclass(), methodName, parameterTypes);
        }

        return new MappingUtils.ClassMember(methodName, null);
    }

    private static String classTypeToDescriptor(Class<?>[] classTypes) {
        StringBuilder desc = new StringBuilder("(");

        for (Class<?> clas : classTypes) {
            desc.append(Type.getDescriptor(clas));
        }

        return desc + ")";
    }

    public static String mapDescriptor(MappingsRegistry registry, String desc) {
        int srcNamespace = registry.getFullMappings().getNamespaceId(registry.getSourceNamespace());
        int targetNamespace = registry.getFullMappings().getNamespaceId(registry.getTargetNamespace());

        return registry.getFullMappings().mapDesc(desc, srcNamespace, targetNamespace);
    }
}
