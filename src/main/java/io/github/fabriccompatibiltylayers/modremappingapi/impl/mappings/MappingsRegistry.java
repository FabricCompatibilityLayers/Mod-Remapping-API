package io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings;

import fr.catcore.modremapperapi.utils.Constants;
import fr.catcore.wfvaio.WhichFabricVariantAmIOn;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingBuilder;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ModRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.MappingBuilderImpl;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.MappingsUtilsImpl;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.FileUtils;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.VersionHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.MappingConfiguration;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipError;

import static fr.catcore.modremapperapi.remapping.RemapUtil.defaultPackage;

@ApiStatus.Internal
public class MappingsRegistry {
    public static List<String> VANILLA_CLASS_LIST = new ArrayList<>();

    public static final MemoryMappingTree VANILLA;
    public static MemoryMappingTree FORMATTED = new MemoryMappingTree();
    public static boolean generated = false;

    public static MemoryMappingTree MODS;
    public static MemoryMappingTree ADDITIONAL;

    static {
        try {
            MODS = MappingTreeHelper.createMappingTree();
            ADDITIONAL = MappingTreeHelper.createMappingTree();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static MemoryMappingTree FULL = new MemoryMappingTree();

    static {
        URL url = MappingConfiguration.class.getClassLoader().getResource("mappings/mappings.tiny");

        if (url != null) {
            try {
                URLConnection connection = url.openConnection();

                VANILLA = MappingTreeHelper.readMappings(connection.getInputStream());
            } catch (IOException | ZipError e) {
                throw new RuntimeException("Error reading "+url, e);
            }
        } else {
            VANILLA = null;
        }
    }

    public static void generateFormattedMappings(@Nullable InputStream extraStream) throws IOException {
        generated = true;

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
        MappingVisitor visitor = MappingTreeHelper.getNsReorderingVisitor(tempTree, switchNamespace, renames);

        VANILLA.accept(visitor);

        if (extraStream == null) {
            tempTree.accept(FORMATTED);
        } else {
            MappingTree extra = MappingTreeHelper.readMappings(extraStream);

            MappingTreeHelper.mergeIntoNew(
                    FORMATTED,
                    tempTree,
                    extra
            );
        }

        FORMATTED.accept(FULL);

        for (MappingTree.ClassMapping classView : FORMATTED.getClasses()) {
            String className = classView.getName(MappingsUtilsImpl.getSourceNamespace());

            if (className != null) {
                VANILLA_CLASS_LIST.add("/" + className + ".class");
            }
        }

        try {
            MappingTreeHelper.exportMappings(MappingsRegistry.FORMATTED, Constants.MC_MAPPINGS_FILE.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Error while writing formatted mappings", e);
        }
    }

    public static void addModMappings(Path path) {
        MappingBuilder mappingBuilder = new MappingBuilderImpl(MODS);

        try {
            FileUtils.listPathContent(path)
                    .stream()
                    .filter(file -> file.endsWith(".class"))
                    .map(file -> file.replace(".class", ""))
                    .forEach(cl -> mappingBuilder.addMapping(cl, (cl.contains("/") ? "" : defaultPackage) + cl));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void generateModMappings() {
        try {
            MODS.visitEnd();

            MappingTreeHelper.exportMappings(MODS, Constants.REMAPPED_MAPPINGS_FILE.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Error while generating mods mappings", e);
        }

        MappingsUtilsImpl.addMappingsToContext(MODS);
    }

    public static void registerAdditionalMappings(List<ModRemapper> remappers) {
        MappingBuilder builder = new MappingBuilderImpl(ADDITIONAL);

        for (ModRemapper remapper : remappers) {
            remapper.registerMappings(builder);
        }

        ADDITIONAL.visitEnd();

        try {
            MappingTreeHelper.exportMappings(ADDITIONAL, Constants.EXTRA_MAPPINGS_FILE.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Error while generating remappers mappings", e);
        }

        MappingsUtilsImpl.addMappingsToContext(ADDITIONAL);
    }
}
