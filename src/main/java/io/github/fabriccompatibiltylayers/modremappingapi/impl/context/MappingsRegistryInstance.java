package io.github.fabriccompatibiltylayers.modremappingapi.impl.context;

import fr.catcore.wfvaio.WhichFabricVariantAmIOn;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.InternalCacheHandler;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingBuilder;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.context.v1.V1MappingBuilderImpl;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings.MappingTreeHelper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings.MappingsRegistry;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.FileUtils;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.VersionHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.adapter.MappingNsRenamer;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

public class MappingsRegistryInstance extends MappingsRegistry {
    public List<String> vanillaClassNames = new ArrayList<>();
    private MemoryMappingTree formatted = new MemoryMappingTree();
    private MemoryMappingTree mods, additional;
    private final MemoryMappingTree full = new MemoryMappingTree();

    private String defaultPackage = "";
    private String sourceNamespace = "official";

    private final InternalCacheHandler cacheHandler;

    public MappingsRegistryInstance(InternalCacheHandler cacheHandler) {
        super();
        this.cacheHandler = cacheHandler;

        try {
            this.formatVanillaMappings();
            mods = MappingTreeHelper.createMappingTree(this.sourceNamespace, getTargetNamespace());
            additional = MappingTreeHelper.createMappingTree(this.sourceNamespace, getTargetNamespace());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void formatVanillaMappings() throws IOException {
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

        MappingVisitor visitor = MappingTreeHelper.getNsReorderingVisitor(formatted, switchNamespace, renames);

        MappingsRegistry.VANILLA.accept(visitor);
    }

    @Override
    public List<String> getVanillaClassNames() {
        return vanillaClassNames;
    }

    @Override
    public MemoryMappingTree getFormattedMappings() {
        return formatted;
    }

    @Override
    public void addToFormattedMappings(InputStream stream, Map<String, String> renames) throws IOException {
        MappingTree extra = MappingTreeHelper.readMappings(stream);

        if (!renames.isEmpty()) {
            MemoryMappingTree renamed = new MemoryMappingTree();

            MappingNsRenamer renamer = new MappingNsRenamer(renamed, renames);

            extra.accept(renamer);

            extra = renamed;
        }

        if (!Objects.equals(extra.getSrcNamespace(), formatted.getSrcNamespace()) && extra.getDstNamespaces().contains(formatted.getSrcNamespace())) {
            MemoryMappingTree switched = new MemoryMappingTree();

            MappingSourceNsSwitch switcher = new MappingSourceNsSwitch(switched, formatted.getSrcNamespace());

            extra.accept(switcher);

            extra = switched;
        }

        formatted = MappingTreeHelper.mergeIntoNew(formatted, extra);
    }

    @Override
    public void completeFormattedMappings() throws IOException {
        formatted.accept(full);

        for (MappingTree.ClassMapping classView : formatted.getClasses()) {
            String className = classView.getName(this.getSourceNamespace());

            if (className != null) {
                vanillaClassNames.add("/" + className + ".class");
            }
        }

        try {
            MappingTreeHelper.exportMappings(formatted, this.cacheHandler.resolveMappings("mc_mappings.tiny"));
        } catch (IOException e) {
            throw new RuntimeException("Error while writing formatted mappings", e);
        }
    }

    public void setDefaultPackage(String defaultPackage) {
        this.defaultPackage = defaultPackage;
    }

    @Override
    public void addModMappings(Path path) {
        MappingBuilder mappingBuilder = new V1MappingBuilderImpl(mods);

        try {
            FileUtils.listPathContent(path)
                    .stream()
                    .filter(file -> file.endsWith(".class"))
                    .map(file -> file.replace(".class", ""))
                    .forEach(cl -> mappingBuilder.addMapping(cl, (cl.contains("/") ? "" : this.defaultPackage) + cl));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void generateModMappings() {
        try {
            mods.visitEnd();

            MappingTreeHelper.exportMappings(mods, this.cacheHandler.resolveMappings("remapped_mappings.tiny"));
        } catch (IOException e) {
            throw new RuntimeException("Error while generating mods mappings", e);
        }

        this.addToFullMappings(mods);
    }

    @Override
    public MemoryMappingTree getModsMappings() {
        return mods;
    }

    @Override
    public MemoryMappingTree getAdditionalMappings() {
        return additional;
    }

    @Override
    public void generateAdditionalMappings() {
        additional.visitEnd();

        try {
            MappingTreeHelper.exportMappings(additional, this.cacheHandler.resolveMappings("extra_mappings.tiny"));
        } catch (IOException e) {
            throw new RuntimeException("Error while generating remappers mappings", e);
        }

        this.addToFullMappings(additional);
    }

    @Override
    public MemoryMappingTree getFullMappings() {
        return full;
    }

    @Override
    public String getSourceNamespace() {
        return sourceNamespace;
    }

    public void setSourceNamespace(String sourceNamespace) {
        this.sourceNamespace = sourceNamespace;

        try {
            mods = MappingTreeHelper.createMappingTree(this.sourceNamespace, getTargetNamespace());
            additional = MappingTreeHelper.createMappingTree(this.sourceNamespace, getTargetNamespace());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getTargetNamespace() {
        return FabricLoader.getInstance().getMappingResolver().getCurrentRuntimeNamespace();
    }

    @Override
    public void writeFullMappings() {
        try {
            MappingTreeHelper.exportMappings(full, this.cacheHandler.resolveMappings("full_mappings.tiny"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<MappingTree> getRemappingMappings() {
        return Arrays.asList(
                this.getFormattedMappings(),
                this.getModsMappings(),
                this.getAdditionalMappings()
        );
    }
}
