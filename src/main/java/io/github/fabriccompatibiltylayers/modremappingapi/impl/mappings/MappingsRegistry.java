package io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings;

import fr.catcore.modremapperapi.utils.Constants;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingBuilder;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ModRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.MappingBuilderImpl;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.MappingsUtilsImpl;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.FileUtils;
import net.fabricmc.loader.impl.launch.MappingConfiguration;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipError;

@ApiStatus.Internal
public abstract class MappingsRegistry {
    public static final MemoryMappingTree VANILLA;

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

    public abstract List<String> getVanillaClassNames();
    public abstract MemoryMappingTree getFormattedMappings();
    public abstract void addToFormattedMappings(InputStream stream) throws IOException;
    public abstract void completeFormattedMappings() throws IOException;
    public abstract void addModMappings(Path path);
    public abstract void generateModMappings();
    public abstract MemoryMappingTree getModsMappings();
    public abstract MemoryMappingTree getAdditionalMappings();

    public static MemoryMappingTree ADDITIONAL;

    static {
        try {
            ADDITIONAL = MappingTreeHelper.createMappingTree();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static MemoryMappingTree FULL = new MemoryMappingTree();

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
