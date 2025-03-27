package io.github.fabriccompatibiltylayers.modremappingapi.impl.context;

import fr.catcore.modremapperapi.utils.Constants;
import fr.catcore.wfvaio.WhichFabricVariantAmIOn;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingBuilder;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.MappingBuilderImpl;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.MappingsUtilsImpl;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings.MappingTreeHelper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings.MappingsRegistry;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.FileUtils;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.VersionHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

public class MappingsRegistryInstance extends MappingsRegistry {
    public List<String> vanillaClassNames = new ArrayList<>();
    private final MemoryMappingTree formatted = new MemoryMappingTree();
    private final MemoryMappingTree mods, additional;
    private final MemoryMappingTree full = new MemoryMappingTree();

    private String defaultPackage = "";
    private String sourceNamespace = "official";

    public MappingsRegistryInstance() {
        super();

        try {
            this.formatVanillaMappings();
            mods = MappingTreeHelper.createMappingTree();
            additional = MappingTreeHelper.createMappingTree();
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
    public void addToFormattedMappings(InputStream stream) throws IOException {
        MappingTree extra = MappingTreeHelper.readMappings(stream);

        MappingTreeHelper.merge(formatted, extra);
    }

    @Override
    public void completeFormattedMappings() throws IOException {
        formatted.accept(full);

        for (MappingTree.ClassMapping classView : formatted.getClasses()) {
            String className = classView.getName(MappingsUtilsImpl.getSourceNamespace());

            if (className != null) {
                vanillaClassNames.add("/" + className + ".class");
            }
        }

        try {
            MappingTreeHelper.exportMappings(formatted, Constants.MC_MAPPINGS_FILE.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Error while writing formatted mappings", e);
        }
    }

    public void setDefaultPackage(String defaultPackage) {
        this.defaultPackage = defaultPackage;
    }

    @Override
    public void addModMappings(Path path) {
        MappingBuilder mappingBuilder = new MappingBuilderImpl(mods);

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

            MappingTreeHelper.exportMappings(mods, Constants.REMAPPED_MAPPINGS_FILE.toPath());
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
            MappingTreeHelper.exportMappings(additional, Constants.EXTRA_MAPPINGS_FILE.toPath());
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
    }

    @Override
    public String getTargetNamespace() {
        return FabricLoader.getInstance().getMappingResolver().getCurrentRuntimeNamespace();
    }

    @Override
    public void writeFullMappings() {
        try {
            MappingTreeHelper.exportMappings(full, Constants.FULL_MAPPINGS_FILE.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
