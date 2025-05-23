package io.github.fabriccompatibilitylayers.modremappingapi.impl.compatibility.v0;

import fr.catcore.modremapperapi.remapping.RemapUtil;
import fr.catcore.wfvaio.FabricVariants;
import fr.catcore.wfvaio.WhichFabricVariantAmIOn;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingBuilder;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ModRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.RemapLibrary;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.VisitorInfos;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.mappings.MappingTreeHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@ApiStatus.Internal
public class V0ModRemapper implements ModRemapper {
    private static final boolean BABRIC = WhichFabricVariantAmIOn.getVariant() == FabricVariants.BABRIC || WhichFabricVariantAmIOn.getVariant() == FabricVariants.BABRIC_NEW_FORMAT;

    private final fr.catcore.modremapperapi.api.ModRemapper modRemapper;

    public V0ModRemapper(fr.catcore.modremapperapi.api.ModRemapper modRemapper) {
        this.modRemapper = modRemapper;
    }

    @Override
    public String[] getJarFolders() {
        return modRemapper.getJarFolders();
    }

    @Override
    public void addRemapLibraries(List<RemapLibrary> libraries, EnvType environment) {
        for (var library : modRemapper.getRemapLibraries()) {
            libraries.add(new RemapLibrary(library.url, library.path, library.toExclude, library.fileName));
        }
    }

    public Map<String, List<String>> getExclusions() {
        return modRemapper.getExclusions();
    }

    @Override
    public void registerMappings(MappingBuilder list) {
        var mappingList = new RemapUtil.MappingList();
        modRemapper.getMappingList(mappingList);

        var mappingTree = convertMappingList(mappingList);

        if (mappingTree != null) {
            for (var classMapping : mappingTree.getClasses()) {
                var classBuilder = list.addMapping(
                        classMapping.getName("official"),
                        classMapping.getName("intermediary")
                );

                for (var fieldMapping : classMapping.getFields()) {
                    classBuilder.field(
                            fieldMapping.getName("official"),
                            fieldMapping.getName("intermediary"),
                            fieldMapping.getDesc("official")
                    );
                }

                for (var methodMapping : classMapping.getMethods()) {
                    classBuilder.method(
                            methodMapping.getName("official"),
                            methodMapping.getName("intermediary"),
                            methodMapping.getDesc("official")
                    );
                }
            }
        }
    }

    @Override
    public void registerPreVisitors(VisitorInfos infos) {

    }

    @Override
    public void registerPostVisitors(VisitorInfos infos) {
        modRemapper.registerVisitors((fr.catcore.modremapperapi.remapping.VisitorInfos) infos);
    }

    @Override
    public Optional<String> getDefaultPackage() {
        return modRemapper.getDefaultPackage();
    }

    @Override
    public void afterRemap() {
        modRemapper.afterRemap();
    }

    /**
     * Convert old MappingList to a MemoryMappingTree, fixes some quirks of the previous mapping format on babric
     * where "intermediary" is the main namespace instead of "official".
     * @param mappingList
     * @return
     */
    private MemoryMappingTree convertMappingList(RemapUtil.MappingList mappingList) {
        MemoryMappingTree mappingTree;

        try {
            mappingTree = BABRIC
                ? MappingTreeHelper.createMappingTree("intermediary", "official")
                : MappingTreeHelper.createMappingTree();

            mappingList.accept(mappingTree);
            mappingTree.visitEnd();

            if ("intermediary".equals(mappingTree.getSrcNamespace())) {
                var newTree = new MemoryMappingTree();
                var visitor = new MappingSourceNsSwitch(newTree, "official");

                mappingTree.accept(visitor);
                mappingTree = newTree;
            }
        } catch (IOException e) {
            throw new RuntimeException("Error while generating remappers mappings", e);
        }

        return mappingTree;
    }
}
