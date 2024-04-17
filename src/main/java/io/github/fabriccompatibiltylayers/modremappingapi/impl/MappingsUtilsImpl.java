package io.github.fabriccompatibiltylayers.modremappingapi.impl;

import fr.catcore.modremapperapi.ModRemappingAPI;
import fr.catcore.wfvaio.WhichFabricVariantAmIOn;
import io.github.fabriccompatibiltylayers.modremappingapi.api.MappingUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.MappingConfiguration;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.adapter.MappingDstNsReorder;
import net.fabricmc.mappingio.adapter.MappingNsRenamer;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.format.tiny.Tiny1FileReader;
import net.fabricmc.mappingio.format.tiny.Tiny2FileReader;
import net.fabricmc.mappingio.tree.*;
import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.api.TrClass;
import net.fabricmc.tinyremapper.api.TrEnvironment;
import net.fabricmc.tinyremapper.api.TrMethod;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.zip.ZipError;

import static fr.catcore.modremapperapi.utils.MappingsUtils.getTargetNamespace;

@ApiStatus.Internal
public class MappingsUtilsImpl {
    private static boolean initialized = false;
    private static MappingTree VANILLA_MAPPINGS;
    private static MappingTree MINECRAFT_MAPPINGS;
    private static VisitableMappingTree FULL_MAPPINGS = new MemoryMappingTree();

    @ApiStatus.Internal
    public static MappingTree getVanillaMappings() {
        loadMappings();

        return VANILLA_MAPPINGS;
    }

    @ApiStatus.Internal
    public static MappingTree getMinecraftMappings() {
        loadMappings();

        return MINECRAFT_MAPPINGS;
    }

    private static void loadMappings() {
        if (initialized) return;

        URL url = MappingConfiguration.class.getClassLoader().getResource("mappings/mappings.tiny");

        if (url != null) {
            try {
                URLConnection connection = url.openConnection();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    long time = System.currentTimeMillis();
                    MemoryMappingTree mappingTree = new MemoryMappingTree();

                    // We will only ever need to read tiny here
                    // so to strip the other formats from the included copy of mapping IO, don't use MappingReader.read()
                    reader.mark(4096);
                    final MappingFormat format = MappingReader.detectFormat(reader);
                    reader.reset();

                    switch (format) {
                        case TINY_FILE:
                            Tiny1FileReader.read(reader, mappingTree);
                            break;
                        case TINY_2_FILE:
                            Tiny2FileReader.read(reader, mappingTree);
                            break;
                        default:
                            throw new UnsupportedOperationException("Unsupported mapping format: " + format);
                    }

                    Log.debug(LogCategory.MAPPINGS, "Loading mappings took %d ms", System.currentTimeMillis() - time);

                    VANILLA_MAPPINGS = mappingTree;
                }
            } catch (IOException | ZipError e) {
                throw new RuntimeException("Error reading "+url, e);
            }
        }

        adaptVanillaMappings();

        if (VANILLA_MAPPINGS == null) {
            Log.info(LogCategory.MAPPINGS, "Mappings not present!");
            VANILLA_MAPPINGS = new MemoryMappingTree();
        }

        try {
            MINECRAFT_MAPPINGS.accept(FULL_MAPPINGS);
        } catch (IOException e) {
            e.printStackTrace();
        }

        initialized = true;
    }

    private static void adaptVanillaMappings() {
        MINECRAFT_MAPPINGS = new MemoryMappingTree();

        if (VANILLA_MAPPINGS == null) {
            return;
        }

        Map<String, String> renames = new HashMap<>();
        boolean switchNamespace = false;

        switch (WhichFabricVariantAmIOn.getVariant()) {
            case BABRIC:
                renames.put(FabricLoader.getInstance().getEnvironmentType().name().toLowerCase(Locale.ENGLISH), "official");
                switchNamespace = true;
                break;
            case ORNITHE_V2:
                Boolean merged = VersionHelper.predicate(">=1.3");
                if (merged != null && !merged) {
                    renames.put(FabricLoader.getInstance().getEnvironmentType().name().toLowerCase(Locale.ENGLISH) + "Official", "official");
                    switchNamespace = true;
                }
                break;
            case BABRIC_NEW_FORMAT:
                renames.put(FabricLoader.getInstance().getEnvironmentType().name().toLowerCase(Locale.ENGLISH) + "Official", "official");
                switchNamespace = true;
                break;
            default:
                break;
        }

        MappingVisitor visitor = getMappingVisitor(switchNamespace, renames);

        try {
            VANILLA_MAPPINGS.accept(visitor);
        } catch (IOException e) {
            e.printStackTrace();
//            throw new RuntimeException(e);
        }
    }

    private static @NotNull MappingVisitor getMappingVisitor(boolean switchNamespace, Map<String, String> renames) {
        List<String> targetNamespace = new ArrayList<>();
        targetNamespace.add("intermediary");

        if (VANILLA_MAPPINGS.getDstNamespaces().contains("named")) targetNamespace.add("named");

        MappingVisitor visitor = (MappingVisitor) MINECRAFT_MAPPINGS;

        if (switchNamespace) {
            visitor = new MappingSourceNsSwitch(
                    new MappingDstNsReorder(
                            visitor,
                            targetNamespace
                    ),
                    "official"
            );
        }

        visitor = new MappingNsRenamer(visitor, renames);
        return visitor;
    }

    private static IMappingProvider.Member memberOf(String className, String memberName, String descriptor) {
        return new IMappingProvider.Member(className, memberName, descriptor);
    }

    @ApiStatus.Internal
    public static IMappingProvider createProvider(MappingTree mappings, String from, String to) {
        return (acceptor) -> {
            final int fromId = mappings.getNamespaceId(from);
            final int toId = mappings.getNamespaceId(to);

            for (MappingTree.ClassMapping classDef : mappings.getClasses()) {
                String className = classDef.getName(fromId);
                String dstName = classDef.getName(toId);

                if (ModRemappingAPI.BABRIC && className == null) {
                    if (dstName == null) continue;
                    className = dstName;
                }

                if (dstName == null) {
                    dstName = className;
                }

                acceptor.acceptClass(className, dstName);

                for (MappingTree.FieldMapping field : classDef.getFields()) {
                    acceptMember(acceptor::acceptField, className, field, fromId, toId);
                }

                for (MappingTree.MethodMapping method : classDef.getMethods()) {
                    acceptMember(acceptor::acceptMethod, className, method, fromId, toId);
                }
            }
        };
    }

    private static void acceptMember(BiConsumer<IMappingProvider.Member, String> consumer, String className,
                                     MappingTreeView.MemberMappingView memberMappingView, int fromId, int toId) {
        String memberName = memberMappingView.getName(fromId);
        String memberDstName = memberMappingView.getName(toId);
        String memberDesc = memberMappingView.getDesc(fromId);
        String memberDstDesc = memberMappingView.getDesc(toId);

        if (ModRemappingAPI.BABRIC && memberName == null) {
            if (memberDstName == null) return;
            memberName = memberDstName;
        }

        if (ModRemappingAPI.BABRIC && memberDesc == null) {
            if (memberDstDesc == null) return;
            memberDesc = memberDstDesc;
        }

        consumer.accept(memberOf(className, memberName, memberDesc), memberDstName);
    }

    @ApiStatus.Internal
    public static void initializeMappingTree(MappingVisitor mappingVisitor) throws IOException {
        mappingVisitor.visitHeader();

        List<String> namespaces = new ArrayList<>();
        namespaces.add("intermediary");

        if (getMinecraftMappings().getDstNamespaces().contains("named")) {
            namespaces.add("named");
        }

        mappingVisitor.visitNamespaces("official", namespaces);
    }

    @ApiStatus.Internal
    public static void addMappingsToContext(MappingTreeView mappingTreeView) {
        try {
            mappingTreeView.accept(FULL_MAPPINGS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void completeMappingsFromTr(TrEnvironment trEnvironment) {
        int srcNamespace = FULL_MAPPINGS.getNamespaceId("official");
        int targetNamespace = FULL_MAPPINGS.getNamespaceId(getTargetNamespace());

        Map<String, List<ExtendedClassMember>> classMembers = new HashMap<>();

        for (MappingTree.ClassMapping classMapping : FULL_MAPPINGS.getClasses()) {
            String className = classMapping.getName(srcNamespace);

            TrClass trClass = trEnvironment.getClass(className);

            if (trClass == null) continue;

            List<String> children = trClass.getChildren().stream().map(TrClass::getName).collect(Collectors.toList());

            for (MappingTree.MethodMapping methodMapping : classMapping.getMethods()) {
                TrMethod method = trClass.getMethod(methodMapping.getName(srcNamespace), methodMapping.getDesc(srcNamespace));

                if (method != null && method.isVirtual()) {
                    for (String child : children) {
                        if (!classMembers.containsKey(child)) classMembers.put(child, new ArrayList<>());

                        classMembers.get(child).add(new ExtendedClassMember(
                                methodMapping.getName(srcNamespace), methodMapping.getDesc(srcNamespace), className
                        ));
                    }
                }
            }
        }

        int propagated = 0;

        for (Map.Entry<String, List<ExtendedClassMember>> entry : classMembers.entrySet()) {
            TrClass trClass = trEnvironment.getClass(entry.getKey());

            if (trClass == null) continue;

            try {
                FULL_MAPPINGS.visitClass(entry.getKey());
            } catch (IOException e) {
                e.printStackTrace();
            }

            MappingTree.ClassMapping classMapping = FULL_MAPPINGS.getClass(entry.getKey());

            for (ExtendedClassMember member : entry.getValue()) {
                TrMethod method = trClass.getMethod(member.name, member.desc);

                if (method == null) continue;

                if (classMapping.getMethod(member.name, member.desc, srcNamespace) != null) continue;

                try {
                    FULL_MAPPINGS.visitMethod(member.name, member.desc);

                    MappingTree.MethodMapping methodMapping = FULL_MAPPINGS.getMethod(member.owner, member.name, member.desc, srcNamespace);
                    if (methodMapping == null) continue;

                    FULL_MAPPINGS.visitDstName(MappedElementKind.METHOD, targetNamespace, methodMapping.getName(targetNamespace));
                    FULL_MAPPINGS.visitDstDesc(MappedElementKind.METHOD, targetNamespace, methodMapping.getDesc(targetNamespace));

                    propagated++;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println("Propagated: " + propagated + " methods");
    }

    static class ExtendedClassMember extends MappingUtils.ClassMember {
        public final String owner;
        public ExtendedClassMember(String name, @Nullable String desc, String owner) {
            super(name, desc);
            this.owner = owner;
        }
    }

    public static String mapClass(String className) {
        int srcNamespace = FULL_MAPPINGS.getNamespaceId("official");
        int targetNamespace = FULL_MAPPINGS.getNamespaceId(getTargetNamespace());

        return FULL_MAPPINGS.mapClassName(className, srcNamespace, targetNamespace);
    }

    public static String unmapClass(String className) {
        int srcNamespace = FULL_MAPPINGS.getNamespaceId(getTargetNamespace());
        int targetNamespace = FULL_MAPPINGS.getNamespaceId("official");

        return FULL_MAPPINGS.mapClassName(className, srcNamespace, targetNamespace);
    }

    public static MappingUtils.ClassMember mapField(String className, String fieldName, @Nullable String fieldDesc) {
        int srcNamespace = FULL_MAPPINGS.getNamespaceId("official");
        int targetNamespace = FULL_MAPPINGS.getNamespaceId(getTargetNamespace());

        MappingTree.FieldMapping fieldMapping = FULL_MAPPINGS.getField(className, fieldName, fieldDesc, srcNamespace);

        return mapMember(fieldName, fieldDesc, targetNamespace, fieldMapping);
    }

    public static MappingUtils.ClassMember mapFieldFromRemappedClass(String className, String fieldName, @Nullable String fieldDesc) {
        int srcNamespace = FULL_MAPPINGS.getNamespaceId("official");
        int targetNamespace = FULL_MAPPINGS.getNamespaceId(getTargetNamespace());

        MappingTree.ClassMapping classMapping = FULL_MAPPINGS.getClass(className, targetNamespace);
        if (classMapping == null) return new MappingUtils.ClassMember(fieldName, fieldDesc);

        MappingTree.FieldMapping fieldMapping = classMapping.getField(fieldName, fieldDesc, srcNamespace);
        return mapMember(fieldName, fieldDesc, targetNamespace, fieldMapping);
    }

    public static MappingUtils.ClassMember mapMethod(String className, String methodName, String methodDesc) {
        int srcNamespace = FULL_MAPPINGS.getNamespaceId("official");
        int targetNamespace = FULL_MAPPINGS.getNamespaceId(getTargetNamespace());

        MappingTree.MethodMapping methodMapping = FULL_MAPPINGS.getMethod(className, methodName, methodDesc, srcNamespace);

        return mapMember(methodName, methodDesc, targetNamespace, methodMapping);
    }

    public static MappingUtils.ClassMember mapMethodFromRemappedClass(String className, String methodName, String methodDesc) {
        int srcNamespace = FULL_MAPPINGS.getNamespaceId("official");
        int targetNamespace = FULL_MAPPINGS.getNamespaceId(getTargetNamespace());

        MappingTree.ClassMapping classMapping = FULL_MAPPINGS.getClass(className, targetNamespace);
        if (classMapping == null) return new MappingUtils.ClassMember(methodName, methodDesc);

        MappingTree.MethodMapping methodMapping = classMapping.getMethod(methodName, methodDesc, srcNamespace);
        return mapMember(methodName, methodDesc, targetNamespace, methodMapping);
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
        int srcNamespace = FULL_MAPPINGS.getNamespaceId("official");
        int targetNamespace = FULL_MAPPINGS.getNamespaceId(getTargetNamespace());
        MappingTree.ClassMapping classMapping = FULL_MAPPINGS.getClass(owner.getName().replace(".", "/"), targetNamespace);

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

        int srcNamespace = FULL_MAPPINGS.getNamespaceId("official");
        int targetNamespace = FULL_MAPPINGS.getNamespaceId(getTargetNamespace());
        MappingTree.ClassMapping classMapping = FULL_MAPPINGS.getClass(owner.getName().replace(".", "/"), targetNamespace);

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
}
