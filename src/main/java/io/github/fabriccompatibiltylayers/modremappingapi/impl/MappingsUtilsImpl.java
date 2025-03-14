package io.github.fabriccompatibiltylayers.modremappingapi.impl;

import fr.catcore.modremapperapi.utils.Constants;
import io.github.fabriccompatibiltylayers.modremappingapi.api.MappingUtils;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings.MappingTreeHelper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings.MappingsRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.tree.*;
import net.fabricmc.tinyremapper.api.TrClass;
import net.fabricmc.tinyremapper.api.TrEnvironment;
import net.fabricmc.tinyremapper.api.TrMethod;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@ApiStatus.Internal
public class MappingsUtilsImpl {
    private static String sourceNamespace = "official";

    @ApiStatus.Internal
    public static String getSourceNamespace() {
        return sourceNamespace;
    }

    @ApiStatus.Internal
    public static void setSourceNamespace(String sourceNamespace) {
        MappingsUtilsImpl.sourceNamespace = sourceNamespace;
    }

    public static boolean isSourceNamespaceObf() {
        return Objects.equals(sourceNamespace, "official");
    }

    public static String getTargetNamespace() {
        return FabricLoader.getInstance().getMappingResolver().getCurrentRuntimeNamespace();
    }

    public static String getNativeNamespace() {
        if (ModRemappingAPIImpl.BABRIC) {
            return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT ? "client" : "server";
        }

        return "official";
    }

    @ApiStatus.Internal
    public static void addMappingsToContext(MappingTree mappingTreeView) {
        try {
            MappingTreeHelper.merge(MappingsRegistry.FULL, mappingTreeView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void completeMappingsFromTr(TrEnvironment trEnvironment, String src) {
        int srcNamespace = MappingsRegistry.FULL.getNamespaceId(src);
        int trueSrcNamespace = MappingsRegistry.FULL.getNamespaceId(MappingsRegistry.FULL.getSrcNamespace());

        Map<ExtendedClassMember, List<String>> classMembers = new HashMap<>();

        for (MappingTree.ClassMapping classMapping : MappingsRegistry.FULL.getClasses()) {
            String className = classMapping.getName(srcNamespace);

            TrClass trClass = trEnvironment.getClass(className);

            if (trClass == null) continue;

            List<String> children = trClass.getChildren().stream().map(TrClass::getName).collect(Collectors.toList());

            for (MappingTree.MethodMapping methodMapping : classMapping.getMethods()) {
                String methodName = methodMapping.getName(srcNamespace);
                String methodDesc = methodMapping.getDesc(srcNamespace);

                if (methodName == null || methodDesc == null) continue;

                TrMethod method = trClass.getMethod(methodName, methodDesc);

                if (method != null && method.isVirtual()) {
                    classMembers.put(new ExtendedClassMember(
                            methodMapping.getName(srcNamespace), methodMapping.getDesc(srcNamespace), className
                    ), children);
                }
            }
        }

        gatherChildClassCandidates(trEnvironment, classMembers);

        int propagated = 0;

        try {
            MappingsRegistry.FULL.visitHeader();
            MappingsRegistry.FULL.visitNamespaces(MappingsRegistry.FULL.getSrcNamespace(), MappingsRegistry.FULL.getDstNamespaces());
            MappingsRegistry.FULL.visitContent();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (Map.Entry<ExtendedClassMember, List<String>> entry : classMembers.entrySet()) {
            ExtendedClassMember member = entry.getKey();

            for (String child : entry.getValue()) {

                TrClass trClass = trEnvironment.getClass(child);
                if (trClass == null) continue;

                if (srcNamespace == trueSrcNamespace) {
                    MappingsRegistry.FULL.visitClass(child);
                } else {
                    MappingsRegistry.FULL.visitClass(MappingsRegistry.FULL.mapClassName(child, srcNamespace, trueSrcNamespace));
                }

                MappingTree.ClassMapping classMapping = MappingsRegistry.FULL.getClass(child, srcNamespace);

                if (classMapping == null) continue;

                TrMethod trMethod = trClass.getMethod(member.name, member.desc);
                if (trMethod == null) continue;

                try {
                    if (srcNamespace == trueSrcNamespace) {
                        MappingsRegistry.FULL.visitMethod(member.name, member.desc);
                    } else {
                        MappingTree.MemberMapping memberMapping = MappingsRegistry.FULL.getMethod(member.owner, member.name, member.desc, srcNamespace);
                        if (memberMapping == null) continue;

                        MappingsRegistry.FULL.visitMethod(memberMapping.getSrcName(), memberMapping.getSrcDesc());

                        MappingsRegistry.FULL.visitDstName(MappedElementKind.METHOD, srcNamespace, member.name);
                        MappingsRegistry.FULL.visitDstDesc(MappedElementKind.METHOD, srcNamespace, member.desc);
                    }

                    MappingTree.MethodMapping methodMapping = MappingsRegistry.FULL.getMethod(member.owner, member.name, member.desc, srcNamespace);
                    if (methodMapping == null) continue;

                    MappingTree.MethodMapping newMethodMapping = classMapping.getMethod(member.name, member.desc, srcNamespace);

                    boolean actualPropagated = false;

                    for (String namespace : MappingsRegistry.FULL.getDstNamespaces()) {
                        int targetNamespace = MappingsRegistry.FULL.getNamespaceId(namespace);

                        if (targetNamespace == srcNamespace) continue;

                        if (newMethodMapping.getName(targetNamespace) == null) {
                            String targetName = methodMapping.getName(targetNamespace);

                            if (targetName != null) {
                                MappingsRegistry.FULL.visitDstName(MappedElementKind.METHOD, targetNamespace, targetName);
                                actualPropagated = true;
                            }
                        }

                        if (newMethodMapping.getDesc(targetNamespace) == null) {
                            String targetDesc = methodMapping.getDesc(targetNamespace);

                            if (targetDesc != null) {
                                MappingsRegistry.FULL.visitDstDesc(MappedElementKind.METHOD, targetNamespace, targetDesc);
                                actualPropagated = true;
                            }
                        }
                    }

                    if (actualPropagated) propagated++;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        Constants.MAIN_LOGGER.info("Propagated: " + propagated + " methods from namespace " + src);
    }



    public static void writeFullMappings() {
        try {
            MappingTreeHelper.exportMappings(MappingsRegistry.FULL, Constants.FULL_MAPPINGS_FILE.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void gatherChildClassCandidates(TrEnvironment trEnvironment, Map<ExtendedClassMember, List<String>> classMembers) {
        for (Map.Entry<ExtendedClassMember, List<String>> entry : classMembers.entrySet()) {
            List<String> toAdd = new ArrayList<>(entry.getValue());

            while (!toAdd.isEmpty()) {
                TrClass trClass = trEnvironment.getClass(toAdd.remove(0));
                if (trClass == null) continue;

                List<String> children = trClass.getChildren().stream().map(TrClass::getName).collect(Collectors.toList());

                for (String child : children) {
                    if (!entry.getValue().contains(child)) {
                        toAdd.add(child);
                        entry.getValue().add(child);
                    }
                }
            }
        }
    }

    static class ExtendedClassMember extends MappingUtils.ClassMember {
        public final String owner;
        public ExtendedClassMember(String name, @Nullable String desc, String owner) {
            super(name, desc);
            this.owner = owner;
        }
    }

    public static String mapClass(String className) {
        int srcNamespace = MappingsRegistry.FULL.getNamespaceId(getSourceNamespace());
        int targetNamespace = MappingsRegistry.FULL.getNamespaceId(getTargetNamespace());

        return MappingsRegistry.FULL.mapClassName(className, srcNamespace, targetNamespace);
    }

    public static String unmapClass(String className) {
        int srcNamespace = MappingsRegistry.FULL.getNamespaceId(getTargetNamespace());
        int targetNamespace = MappingsRegistry.FULL.getNamespaceId(getSourceNamespace());

        return MappingsRegistry.FULL.mapClassName(className, srcNamespace, targetNamespace);
    }

    public static MappingUtils.ClassMember mapField(String className, String fieldName, @Nullable String fieldDesc) {
        int srcNamespace = MappingsRegistry.FULL.getNamespaceId(getSourceNamespace());
        int targetNamespace = MappingsRegistry.FULL.getNamespaceId(getTargetNamespace());

        MappingTree.FieldMapping fieldMapping = MappingsRegistry.FULL.getField(className, fieldName, fieldDesc, srcNamespace);

        return mapMember(fieldName, fieldDesc, targetNamespace, fieldMapping);
    }

    public static MappingUtils.ClassMember mapFieldFromRemappedClass(String className, String fieldName, @Nullable String fieldDesc) {
        int srcNamespace = MappingsRegistry.FULL.getNamespaceId(getSourceNamespace());
        int targetNamespace = MappingsRegistry.FULL.getNamespaceId(getTargetNamespace());

        MappingTree.ClassMapping classMapping = MappingsRegistry.FULL.getClass(className, targetNamespace);
        if (classMapping == null) return new MappingUtils.ClassMember(fieldName, fieldDesc);

        MappingTree.FieldMapping fieldMapping = classMapping.getField(fieldName, fieldDesc, srcNamespace);
        return mapMember(fieldName, fieldDesc, targetNamespace, fieldMapping);
    }

    public static MappingUtils.ClassMember mapMethod(String className, String methodName, String methodDesc) {
        int srcNamespace = MappingsRegistry.FULL.getNamespaceId(getSourceNamespace());
        int targetNamespace = MappingsRegistry.FULL.getNamespaceId(getTargetNamespace());

        MappingTree.MethodMapping methodMapping = MappingsRegistry.FULL.getMethod(className, methodName, methodDesc, srcNamespace);

        if (methodMapping == null) {
            MappingTree.ClassMapping classMapping = MappingsRegistry.FULL.getClass(className, srcNamespace);
            if (classMapping != null) methodMapping = mapMethodWithPartialDesc(classMapping, methodName, methodDesc, srcNamespace);
        }

        return mapMember(methodName, methodDesc, targetNamespace, methodMapping);
    }

    public static MappingUtils.ClassMember mapMethodFromRemappedClass(String className, String methodName, String methodDesc) {
        int srcNamespace = MappingsRegistry.FULL.getNamespaceId(getSourceNamespace());
        int targetNamespace = MappingsRegistry.FULL.getNamespaceId(getTargetNamespace());

        MappingTree.ClassMapping classMapping = MappingsRegistry.FULL.getClass(className, targetNamespace);
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

    public static MappingUtils.ClassMember mapField(Class<?> owner, String fieldName) {
        int srcNamespace = MappingsRegistry.FULL.getNamespaceId(getSourceNamespace());
        int targetNamespace = MappingsRegistry.FULL.getNamespaceId(getTargetNamespace());
        MappingTree.ClassMapping classMapping = MappingsRegistry.FULL.getClass(owner.getName().replace(".", "/"), targetNamespace);

        if (classMapping != null) {
            MappingTree.FieldMapping fieldMapping = classMapping.getField(fieldName, null, srcNamespace);

            if (fieldMapping != null) {
                return mapMember(fieldName, null, targetNamespace, fieldMapping);
            }
        }

        if (owner.getSuperclass() != null) {
            return mapField(owner.getSuperclass(), fieldName);
        }

        return new MappingUtils.ClassMember(fieldName, null);
    }

    public static MappingUtils.ClassMember mapMethod(Class<?> owner, String methodName, Class<?>[] parameterTypes) {
        String argDesc = classTypeToDescriptor(parameterTypes);

        int srcNamespace = MappingsRegistry.FULL.getNamespaceId(getSourceNamespace());
        int targetNamespace = MappingsRegistry.FULL.getNamespaceId(getTargetNamespace());
        MappingTree.ClassMapping classMapping = MappingsRegistry.FULL.getClass(owner.getName().replace(".", "/"), targetNamespace);

        if (classMapping != null) {
            for (MappingTree.MethodMapping methodDef : classMapping.getMethods()) {
                String methodSubName = methodDef.getName(srcNamespace);

                if (Objects.equals(methodSubName, methodName)) {
                    String methodDescriptor = methodDef.getDesc(getTargetNamespace());

                    if (methodDescriptor != null && methodDescriptor.startsWith(argDesc)) {
                        return new MappingUtils.ClassMember(
                                methodDef.getName(getTargetNamespace()),
                                methodDescriptor
                        );
                    }
                }
            }
        }

        if (owner.getSuperclass() != null) {
            return mapMethod(owner.getSuperclass(), methodName, parameterTypes);
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

    public static String mapDescriptor(String desc) {
        int srcNamespace = MappingsRegistry.FULL.getNamespaceId(getSourceNamespace());
        int targetNamespace = MappingsRegistry.FULL.getNamespaceId(getTargetNamespace());

        return MappingsRegistry.FULL.mapDesc(desc, srcNamespace, targetNamespace);
    }
}
