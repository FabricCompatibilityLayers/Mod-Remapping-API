package io.github.fabriccompatibiltylayers.modremappingapi.impl;

import fr.catcore.modremapperapi.utils.Constants;
import fr.catcore.modremapperapi.utils.MappingsUtils;
import fr.catcore.wfvaio.WhichFabricVariantAmIOn;
import io.github.fabriccompatibiltylayers.modremappingapi.api.MappingUtils;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.MappingTreeHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.MappingConfiguration;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.adapter.MappingDstNsReorder;
import net.fabricmc.mappingio.adapter.MappingNsRenamer;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.format.tiny.Tiny1FileReader;
import net.fabricmc.mappingio.format.tiny.Tiny2FileReader;
import net.fabricmc.mappingio.tree.*;
import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.TinyUtils;
import net.fabricmc.tinyremapper.api.TrClass;
import net.fabricmc.tinyremapper.api.TrEnvironment;
import net.fabricmc.tinyremapper.api.TrMethod;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipError;

import static fr.catcore.modremapperapi.utils.MappingsUtils.getTargetNamespace;

@ApiStatus.Internal
public class MappingsUtilsImpl {
    private static boolean initialized = false;
    private static MappingTree VANILLA_MAPPINGS;
    private static VisitableMappingTree MINECRAFT_MAPPINGS;
    private static VisitableMappingTree FULL_MAPPINGS = new MemoryMappingTree();

    private static String sourceNamespace = "official";

    private static MappingTree EXTRA_MAPPINGS;

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

    @ApiStatus.Internal
    public static String getSourceNamespace() {
        return sourceNamespace;
    }

    @ApiStatus.Internal
    public static void setSourceNamespace(String sourceNamespace) {
        MappingsUtilsImpl.sourceNamespace = sourceNamespace;
    }

    @ApiStatus.Internal
    public static void loadExtraMappings(InputStream stream) {
        try {
            EXTRA_MAPPINGS = loadMappings(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isSourceNamespaceObf() {
        return Objects.equals(sourceNamespace, "official");
    }

    @ApiStatus.Internal
    public static MemoryMappingTree loadMappings(InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
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

            return mappingTree;
        }
    }

    private static void loadMappings() {
        if (initialized) return;

        URL url = MappingConfiguration.class.getClassLoader().getResource("mappings/mappings.tiny");

        if (url != null) {
            try {
                URLConnection connection = url.openConnection();

                VANILLA_MAPPINGS = loadMappings(connection.getInputStream());
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

        MemoryMappingTree tempTree = new MemoryMappingTree();
        MappingVisitor visitor = getMappingVisitor(tempTree, switchNamespace, renames);

        try {
            VANILLA_MAPPINGS.accept(visitor);

            if (EXTRA_MAPPINGS == null) {
                tempTree.accept(MINECRAFT_MAPPINGS);
            } else {
                MappingTreeHelper.mergeIntoNew(MINECRAFT_MAPPINGS, tempTree, EXTRA_MAPPINGS);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static @NotNull MappingVisitor getMappingVisitor(MemoryMappingTree tempTree, boolean switchNamespace, Map<String, String> renames) {
        List<String> targetNamespace = new ArrayList<>();
        targetNamespace.add("intermediary");

        if (VANILLA_MAPPINGS.getDstNamespaces().contains("named")) targetNamespace.add("named");

        MappingVisitor visitor = tempTree;

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

    @ApiStatus.Internal
    public static IMappingProvider createProvider(MappingTree mappings, String from, String to) {
        return TinyUtils.createMappingProvider(mappings, from, to);
    }

    @ApiStatus.Internal
    public static void initializeMappingTree(MappingVisitor mappingVisitor) throws IOException {
        initializeMappingTree(mappingVisitor, getSourceNamespace(), MappingsUtils.getTargetNamespace());
    }

    @ApiStatus.Internal
    public static void initializeMappingTree(MappingVisitor mappingVisitor, String src, String target) throws IOException {
        mappingVisitor.visitHeader();

        List<String> namespaces = new ArrayList<>();
        namespaces.add(target);

        mappingVisitor.visitNamespaces(src, namespaces);
    }

    @ApiStatus.Internal
    public static void addMappingsToContext(MappingTree mappingTreeView) {
        try {
            MappingTreeHelper.merge(FULL_MAPPINGS, mappingTreeView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void completeMappingsFromTr(TrEnvironment trEnvironment, String src) {
        int srcNamespace = FULL_MAPPINGS.getNamespaceId(src);
        int trueSrcNamespace = FULL_MAPPINGS.getNamespaceId(FULL_MAPPINGS.getSrcNamespace());

        Map<ExtendedClassMember, List<String>> classMembers = new HashMap<>();

        for (MappingTree.ClassMapping classMapping : FULL_MAPPINGS.getClasses()) {
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
            FULL_MAPPINGS.visitHeader();
            FULL_MAPPINGS.visitNamespaces(FULL_MAPPINGS.getSrcNamespace(), FULL_MAPPINGS.getDstNamespaces());
            FULL_MAPPINGS.visitContent();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (Map.Entry<ExtendedClassMember, List<String>> entry : classMembers.entrySet()) {
            ExtendedClassMember member = entry.getKey();

            for (String child : entry.getValue()) {

                TrClass trClass = trEnvironment.getClass(child);
                if (trClass == null) continue;

                try {
                    if (srcNamespace == trueSrcNamespace) {
                        FULL_MAPPINGS.visitClass(child);
                    } else {
                        FULL_MAPPINGS.visitClass(FULL_MAPPINGS.mapClassName(child, srcNamespace, trueSrcNamespace));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                MappingTree.ClassMapping classMapping = FULL_MAPPINGS.getClass(child, srcNamespace);

                if (classMapping == null) continue;

                TrMethod trMethod = trClass.getMethod(member.name, member.desc);
                if (trMethod == null) continue;

                try {
                    if (srcNamespace == trueSrcNamespace) {
                        FULL_MAPPINGS.visitMethod(member.name, member.desc);
                    } else {
                        MappingTree.MemberMapping memberMapping = FULL_MAPPINGS.getMethod(member.owner, member.name, member.desc, srcNamespace);
                        if (memberMapping == null) continue;

                        FULL_MAPPINGS.visitMethod(memberMapping.getSrcName(), memberMapping.getSrcDesc());

                        FULL_MAPPINGS.visitDstName(MappedElementKind.METHOD, srcNamespace, member.name);
                        FULL_MAPPINGS.visitDstDesc(MappedElementKind.METHOD, srcNamespace, member.desc);
                    }

                    MappingTree.MethodMapping methodMapping = FULL_MAPPINGS.getMethod(member.owner, member.name, member.desc, srcNamespace);
                    if (methodMapping == null) continue;

                    MappingTree.MethodMapping newMethodMapping = classMapping.getMethod(member.name, member.desc, srcNamespace);

                    boolean actualPropagated = false;

                    for (String namespace : FULL_MAPPINGS.getDstNamespaces()) {
                        int targetNamespace = FULL_MAPPINGS.getNamespaceId(namespace);

                        if (targetNamespace == srcNamespace) continue;

                        if (newMethodMapping.getName(targetNamespace) == null) {
                            String targetName = methodMapping.getName(targetNamespace);

                            if (targetName != null) {
                                FULL_MAPPINGS.visitDstName(MappedElementKind.METHOD, targetNamespace, targetName);
                                actualPropagated = true;
                            }
                        }

                        if (newMethodMapping.getDesc(targetNamespace) == null) {
                            String targetDesc = methodMapping.getDesc(targetNamespace);

                            if (targetDesc != null) {
                                FULL_MAPPINGS.visitDstDesc(MappedElementKind.METHOD, targetNamespace, targetDesc);
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
            MappingWriter writer = MappingWriter.create(Constants.FULL_MAPPINGS_FILE.toPath(), MappingFormat.TINY_2_FILE);
            FULL_MAPPINGS.accept(writer);
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
        int srcNamespace = FULL_MAPPINGS.getNamespaceId(getSourceNamespace());
        int targetNamespace = FULL_MAPPINGS.getNamespaceId(getTargetNamespace());

        return FULL_MAPPINGS.mapClassName(className, srcNamespace, targetNamespace);
    }

    public static String unmapClass(String className) {
        int srcNamespace = FULL_MAPPINGS.getNamespaceId(getTargetNamespace());
        int targetNamespace = FULL_MAPPINGS.getNamespaceId(getSourceNamespace());

        return FULL_MAPPINGS.mapClassName(className, srcNamespace, targetNamespace);
    }

    public static MappingUtils.ClassMember mapField(String className, String fieldName, @Nullable String fieldDesc) {
        int srcNamespace = FULL_MAPPINGS.getNamespaceId(getSourceNamespace());
        int targetNamespace = FULL_MAPPINGS.getNamespaceId(getTargetNamespace());

        MappingTree.FieldMapping fieldMapping = FULL_MAPPINGS.getField(className, fieldName, fieldDesc, srcNamespace);

        return mapMember(fieldName, fieldDesc, targetNamespace, fieldMapping);
    }

    public static MappingUtils.ClassMember mapFieldFromRemappedClass(String className, String fieldName, @Nullable String fieldDesc) {
        int srcNamespace = FULL_MAPPINGS.getNamespaceId(getSourceNamespace());
        int targetNamespace = FULL_MAPPINGS.getNamespaceId(getTargetNamespace());

        MappingTree.ClassMapping classMapping = FULL_MAPPINGS.getClass(className, targetNamespace);
        if (classMapping == null) return new MappingUtils.ClassMember(fieldName, fieldDesc);

        MappingTree.FieldMapping fieldMapping = classMapping.getField(fieldName, fieldDesc, srcNamespace);
        return mapMember(fieldName, fieldDesc, targetNamespace, fieldMapping);
    }

    public static MappingUtils.ClassMember mapMethod(String className, String methodName, String methodDesc) {
        int srcNamespace = FULL_MAPPINGS.getNamespaceId(getSourceNamespace());
        int targetNamespace = FULL_MAPPINGS.getNamespaceId(getTargetNamespace());

        MappingTree.MethodMapping methodMapping = FULL_MAPPINGS.getMethod(className, methodName, methodDesc, srcNamespace);

        if (methodMapping == null) {
            MappingTree.ClassMapping classMapping = FULL_MAPPINGS.getClass(className, srcNamespace);
            if (classMapping != null) methodMapping = mapMethodWithPartialDesc(classMapping, methodName, methodDesc, srcNamespace);
        }

        return mapMember(methodName, methodDesc, targetNamespace, methodMapping);
    }

    public static MappingUtils.ClassMember mapMethodFromRemappedClass(String className, String methodName, String methodDesc) {
        int srcNamespace = FULL_MAPPINGS.getNamespaceId(getSourceNamespace());
        int targetNamespace = FULL_MAPPINGS.getNamespaceId(getTargetNamespace());

        MappingTree.ClassMapping classMapping = FULL_MAPPINGS.getClass(className, targetNamespace);
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
        int srcNamespace = FULL_MAPPINGS.getNamespaceId(getSourceNamespace());
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

        int srcNamespace = FULL_MAPPINGS.getNamespaceId(getSourceNamespace());
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

    public static String mapDescriptor(String desc) {
        int srcNamespace = FULL_MAPPINGS.getNamespaceId(getSourceNamespace());
        int targetNamespace = FULL_MAPPINGS.getNamespaceId(getTargetNamespace());

        return FULL_MAPPINGS.mapDesc(desc, srcNamespace, targetNamespace);
    }
}
