package io.github.fabriccompatibilitylayers.modremappingapi.impl.mappings;

import io.github.fabriccompatibiltylayers.modremappingapi.api.MappingUtils;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.context.BaseModRemapperContext;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.compatibility.v1.ModRemapperV1Context;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.ModRemappingAPIImpl;
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

    public static MappingsRegistry getMappingsRegistry(String contextId) {
        return BaseModRemapperContext.get(contextId).getMappingsRegistry();
    }

    public static String mapClass(MappingsRegistry registry, String className) {
        var srcNamespace = registry.getFullMappings().getNamespaceId(registry.getSourceNamespace());
        var targetNamespace = registry.getFullMappings().getNamespaceId(registry.getTargetNamespace());

        return registry.getFullMappings().mapClassName(className, srcNamespace, targetNamespace);
    }

    public static String unmapClass(MappingsRegistry registry, String className) {
        var srcNamespace = registry.getFullMappings().getNamespaceId(registry.getTargetNamespace());
        var targetNamespace = registry.getFullMappings().getNamespaceId(registry.getSourceNamespace());

        return registry.getFullMappings().mapClassName(className, srcNamespace, targetNamespace);
    }

    public static MappingUtils.ClassMember mapField(MappingsRegistry registry, String className, String fieldName, @Nullable String fieldDesc) {
        var srcNamespace = registry.getFullMappings().getNamespaceId(registry.getSourceNamespace());
        var targetNamespace = registry.getFullMappings().getNamespaceId(registry.getTargetNamespace());

        var fieldMapping = registry.getFullMappings().getField(className, fieldName, fieldDesc, srcNamespace);

        return mapMember(fieldName, fieldDesc, targetNamespace, fieldMapping);
    }

    public static MappingUtils.ClassMember mapFieldFromRemappedClass(MappingsRegistry registry, String className, String fieldName, @Nullable String fieldDesc) {
        var srcNamespace = registry.getFullMappings().getNamespaceId(registry.getSourceNamespace());
        var targetNamespace = registry.getFullMappings().getNamespaceId(registry.getTargetNamespace());

        var classMapping = registry.getFullMappings().getClass(className, targetNamespace);
        if (classMapping == null) return new MappingUtils.ClassMember(fieldName, fieldDesc);

        var fieldMapping = classMapping.getField(fieldName, fieldDesc, srcNamespace);
        return mapMember(fieldName, fieldDesc, targetNamespace, fieldMapping);
    }

    public static MappingUtils.ClassMember mapMethod(MappingsRegistry registry, String className, String methodName, String methodDesc) {
        var srcNamespace = registry.getFullMappings().getNamespaceId(registry.getSourceNamespace());
        var targetNamespace = registry.getFullMappings().getNamespaceId(registry.getTargetNamespace());

        var methodMapping = registry.getFullMappings().getMethod(className, methodName, methodDesc, srcNamespace);

        if (methodMapping == null) {
            var classMapping = registry.getFullMappings().getClass(className, srcNamespace);
            if (classMapping != null) methodMapping = mapMethodWithPartialDesc(classMapping, methodName, methodDesc, srcNamespace);
        }

        return mapMember(methodName, methodDesc, targetNamespace, methodMapping);
    }

    public static MappingUtils.ClassMember mapMethodFromRemappedClass(MappingsRegistry registry, String className, String methodName, String methodDesc) {
        var srcNamespace = registry.getFullMappings().getNamespaceId(registry.getSourceNamespace());
        var targetNamespace = registry.getFullMappings().getNamespaceId(registry.getTargetNamespace());

        var classMapping = registry.getFullMappings().getClass(className, targetNamespace);
        if (classMapping == null) return new MappingUtils.ClassMember(methodName, methodDesc);

        var methodMapping = classMapping.getMethod(methodName, methodDesc, srcNamespace);

        if (methodMapping == null) methodMapping = mapMethodWithPartialDesc(classMapping, methodName, methodDesc, srcNamespace);

        return mapMember(methodName, methodDesc, targetNamespace, methodMapping);
    }

    private static MappingTree.MethodMapping mapMethodWithPartialDesc(MappingTree.ClassMapping classMapping, String methodName, String methodDesc, int namespace) {
        return classMapping.getMethods().stream()
                .filter(methodMapping -> {
                    var name = methodMapping.getName(namespace);
                    var desc = methodMapping.getDesc(namespace);
                    return name != null && name.equals(methodName) && desc != null && desc.startsWith(methodDesc);
                })
                .findFirst()
                .orElse(null);
    }

    @NotNull
    private static MappingUtils.ClassMember mapMember(String memberName, @Nullable String memberDesc, int targetNamespace, MappingTree.MemberMapping memberMapping) {
        if (memberMapping == null) return new MappingUtils.ClassMember(memberName, memberDesc);

        var remappedName = memberMapping.getName(targetNamespace);
        var remappedDesc = memberMapping.getDesc(targetNamespace);

        return new MappingUtils.ClassMember(
                remappedName == null ? memberName : remappedName,
                remappedDesc == null ? memberDesc : remappedDesc
        );
    }

    public static MappingUtils.ClassMember mapField(MappingsRegistry registry, Class<?> owner, String fieldName) {
        var srcNamespace = registry.getFullMappings().getNamespaceId(registry.getSourceNamespace());
        var targetNamespace = registry.getFullMappings().getNamespaceId(registry.getTargetNamespace());
        var classMapping = registry.getFullMappings().getClass(owner.getName().replace(".", "/"), targetNamespace);

        if (classMapping != null) {
            var fieldMapping = classMapping.getField(fieldName, null, srcNamespace);

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
        var argDesc = classTypeToDescriptor(parameterTypes);

        var srcNamespace = registry.getFullMappings().getNamespaceId(registry.getSourceNamespace());
        var targetNamespace = registry.getFullMappings().getNamespaceId(registry.getTargetNamespace());
        var classMapping = registry.getFullMappings().getClass(owner.getName().replace(".", "/"), targetNamespace);

        if (classMapping != null) {
            var matchingMethod = classMapping.getMethods().stream()
                    .filter(methodDef -> {
                        var methodSubName = methodDef.getName(srcNamespace);
                        if (!Objects.equals(methodSubName, methodName)) return false;

                        var methodDescriptor = methodDef.getDesc(targetNamespace);
                        return methodDescriptor != null && methodDescriptor.startsWith(argDesc);
                    })
                    .findFirst();

            if (matchingMethod.isPresent()) {
                var methodDef = matchingMethod.get();
                return new MappingUtils.ClassMember(
                        methodDef.getName(targetNamespace),
                        methodDef.getDesc(targetNamespace)
                );
            }
        }

        if (owner.getSuperclass() != null) {
            return mapMethod(registry, owner.getSuperclass(), methodName, parameterTypes);
        }

        return new MappingUtils.ClassMember(methodName, null);
    }

    private static String classTypeToDescriptor(Class<?>[] classTypes) {
        var desc = new StringBuilder("(");
        Arrays.stream(classTypes).forEach(clazz -> desc.append(Type.getDescriptor(clazz)));
        return desc + ")";
    }

    public static String mapDescriptor(MappingsRegistry registry, String desc) {
        var srcNamespace = registry.getFullMappings().getNamespaceId(registry.getSourceNamespace());
        var targetNamespace = registry.getFullMappings().getNamespaceId(registry.getTargetNamespace());

        return registry.getFullMappings().mapDesc(desc, srcNamespace, targetNamespace);
    }
}
