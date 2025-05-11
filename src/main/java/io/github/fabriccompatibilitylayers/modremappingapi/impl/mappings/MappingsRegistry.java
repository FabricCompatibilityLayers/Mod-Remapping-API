package io.github.fabriccompatibilitylayers.modremappingapi.impl.mappings;

import fr.catcore.modremapperapi.utils.Constants;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingUtils;
import net.fabricmc.loader.impl.launch.MappingConfiguration;
import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.tinyremapper.api.TrClass;
import net.fabricmc.tinyremapper.api.TrEnvironment;
import net.fabricmc.tinyremapper.api.TrMethod;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipError;

@ApiStatus.Internal
public abstract class MappingsRegistry {
    public static final MemoryMappingTree VANILLA;

    static {
        URL url = MappingConfiguration.class.getClassLoader().getResource("mappings/mappings.tiny");

        if (url != null) {
            try (InputStream inputStream = url.openConnection().getInputStream()) {
                VANILLA = MappingTreeHelper.readMappings(inputStream);
            } catch (IOException | ZipError e) {
                throw new RuntimeException("Error reading " + url, e);
            }
        } else {
            VANILLA = null;
        }
    }

    public abstract List<String> getVanillaClassNames();
    public abstract MemoryMappingTree getFormattedMappings();
    public abstract void addToFormattedMappings(InputStream stream, Map<String, String> renames) throws IOException;
    public abstract void completeFormattedMappings() throws IOException;
    public abstract void addModMappings(Path path);
    public abstract void generateModMappings();
    public abstract MemoryMappingTree getModsMappings();
    public abstract MemoryMappingTree getAdditionalMappings();
    public abstract void generateAdditionalMappings();
    public abstract MemoryMappingTree getFullMappings();
    public abstract String getSourceNamespace();
    public abstract String getTargetNamespace();
    public abstract void writeFullMappings();
    public abstract List<MappingTree> getRemappingMappings();

    public void addToFullMappings(MappingTree mappingTreeView) {
        try {
            MappingTreeHelper.merge(this.getFullMappings(), mappingTreeView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isSourceNamespaceObf() {
        return Objects.equals(getSourceNamespace(), "official");
    }

    public void completeMappingsFromTr(TrEnvironment trEnvironment, String src) {
        var srcNamespace = getFullMappings().getNamespaceId(src);
        var trueSrcNamespace = getFullMappings().getNamespaceId(getFullMappings().getSrcNamespace());
    
        var classMembers = getClassMembers(trEnvironment, srcNamespace);
    
        gatherChildClassCandidates(trEnvironment, classMembers);
    
        try {
            getFullMappings().visitHeader();
            getFullMappings().visitNamespaces(getFullMappings().getSrcNamespace(), getFullMappings().getDstNamespaces());
            getFullMappings().visitContent();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    
        var propagated = propagateMembers(trEnvironment, classMembers, srcNamespace, trueSrcNamespace);

        Constants.MAIN_LOGGER.info("Propagated: " + propagated + " methods from namespace " + src);
    }

    private int propagateMembers(TrEnvironment trEnvironment, Map<ExtendedClassMember, List<String>> classMembers, int srcNamespace, int trueSrcNamespace) {
        int propagated = 0;

        for (var entry : classMembers.entrySet()) {
            var member = entry.getKey();
    
            for (var child : entry.getValue()) {
                var trClass = trEnvironment.getClass(child);
                if (trClass == null) continue;
    
                if (srcNamespace == trueSrcNamespace) {
                    getFullMappings().visitClass(child);
                } else {
                    getFullMappings().visitClass(getFullMappings().mapClassName(child, srcNamespace, trueSrcNamespace));
                }
    
                var classMapping = getFullMappings().getClass(child, srcNamespace);
                if (classMapping == null) continue;
    
                var trMethod = trClass.getMethod(member.name, member.desc);
                if (trMethod == null) continue;
    
                try {
                    var actualPropagated = tryPropagatingMember(srcNamespace, trueSrcNamespace, member, classMapping);
                    if (actualPropagated == null) continue;

                    if (actualPropagated) propagated++;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return propagated;
    }

    private @Nullable Boolean tryPropagatingMember(int srcNamespace, int trueSrcNamespace, ExtendedClassMember member, MappingTree.ClassMapping classMapping) throws IOException {
        if (srcNamespace == trueSrcNamespace) {
            getFullMappings().visitMethod(member.name, member.desc);
        } else {
            var memberMapping = getFullMappings().getMethod(member.owner, member.name, member.desc, srcNamespace);
            if (memberMapping == null) return null;
    
            getFullMappings().visitMethod(memberMapping.getSrcName(), memberMapping.getSrcDesc());
    
            getFullMappings().visitDstName(MappedElementKind.METHOD, srcNamespace, member.name);
            getFullMappings().visitDstDesc(MappedElementKind.METHOD, srcNamespace, member.desc);
        }
    
        var methodMapping = getFullMappings().getMethod(member.owner, member.name, member.desc, srcNamespace);
        if (methodMapping == null) return null;
    
        var newMethodMapping = classMapping.getMethod(member.name, member.desc, srcNamespace);
    
        boolean actualPropagated = false;
    
        for (var namespace : getFullMappings().getDstNamespaces()) {
            var targetNamespace = getFullMappings().getNamespaceId(namespace);
    
            if (targetNamespace == srcNamespace) continue;
    
            if (newMethodMapping.getName(targetNamespace) == null) {
                var targetName = methodMapping.getName(targetNamespace);
    
                if (targetName != null) {
                    getFullMappings().visitDstName(MappedElementKind.METHOD, targetNamespace, targetName);
                    actualPropagated = true;
                }
            }
    
            if (newMethodMapping.getDesc(targetNamespace) == null) {
                var targetDesc = methodMapping.getDesc(targetNamespace);

                if (targetDesc != null) {
                    getFullMappings().visitDstDesc(MappedElementKind.METHOD, targetNamespace, targetDesc);
                    actualPropagated = true;
                }
            }
        }

        return actualPropagated;
    }

    private Map<ExtendedClassMember, List<String>> getClassMembers(TrEnvironment trEnvironment, int srcNamespace) {
        var classMembers = new HashMap<ExtendedClassMember, List<String>>();
    
        for (var classMapping : getFullMappings().getClasses()) {
            var className = classMapping.getName(srcNamespace);
    
            var trClass = trEnvironment.getClass(className);
    
            if (trClass == null) continue;
    
            var children = trClass.getChildren().stream()
                    .map(TrClass::getName)
                    .collect(Collectors.toList());
    
            for (var methodMapping : classMapping.getMethods()) {
                var methodName = methodMapping.getName(srcNamespace);
                var methodDesc = methodMapping.getDesc(srcNamespace);
    
                if (methodName == null || methodDesc == null) continue;
    
                var method = trClass.getMethod(methodName, methodDesc);
    
                if (method != null && method.isVirtual()) {
                    classMembers.put(new ExtendedClassMember(
                            methodMapping.getName(srcNamespace),
                            methodMapping.getDesc(srcNamespace),
                            className
                    ), children);
                }
            }
        }
    
        return classMembers;
    }
    
    private void gatherChildClassCandidates(TrEnvironment trEnvironment, Map<ExtendedClassMember, List<String>> classMembers) {
        for (var entry : classMembers.entrySet()) {
            var toAdd = new ArrayList<>(entry.getValue());
    
            while (!toAdd.isEmpty()) {
                var trClass = trEnvironment.getClass(toAdd.remove(0));
                if (trClass == null) continue;
    
                var children = trClass.getChildren().stream()
                        .map(TrClass::getName)
                        .toList();
    
                for (var child : children) {
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
}
