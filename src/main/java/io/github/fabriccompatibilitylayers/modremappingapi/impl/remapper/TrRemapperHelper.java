package io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper;

import fr.catcore.modremapperapi.utils.Constants;
import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.RemappingFlags;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.ModRemappingAPIImpl;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.utils.FileUtils;
import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

@ApiStatus.Internal
public class TrRemapperHelper {
    public static void applyRemapper(TinyRemapper remapper, Map<Path, Path> paths, List<OutputConsumerPath> outputConsumerPaths, List<OutputConsumerPath.ResourceRemapper> resourcePostRemappers, boolean analyzeMapping, String srcNamespace, String targetNamespace) {
        applyRemapper(remapper, paths, outputConsumerPaths, resourcePostRemappers, analyzeMapping, srcNamespace, targetNamespace, null);
    }

    public static void applyRemapper(TinyRemapper remapper, Map<Path, Path> paths, List<OutputConsumerPath> outputConsumerPaths, List<OutputConsumerPath.ResourceRemapper> resourcePostRemappers, boolean analyzeMapping, String srcNamespace, String targetNamespace, @Nullable Consumer<TinyRemapper> action) {
        try {
            Map<Path, InputTag> tagMap = new HashMap<>();

            Constants.MAIN_LOGGER.debug("Creating InputTags!");
            for (Path input : paths.keySet()) {
                InputTag tag = remapper.createInputTag();
                tagMap.put(input, tag);

                if (ModRemappingAPIImpl.getCurrentContext().getRemappingFlags().contains(RemappingFlags.MIXIN)) {
                    if (useHardMixinRemap(input)) ModRemappingAPIImpl.getCurrentContext().getMixinData().getHardMixins().add(tag);
                }

                remapper.readInputsAsync(tag, input);
            }

            Constants.MAIN_LOGGER.debug("Initializing remapping!");
            for (Map.Entry<Path, Path> entry : paths.entrySet()) {
                Constants.MAIN_LOGGER.debug("Starting remapping " + entry.getKey().toString() + " to " + entry.getValue().toString());
                OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(entry.getValue()).build();

                outputConsumerPaths.add(outputConsumer);

                Constants.MAIN_LOGGER.debug("Apply remapper!");
                remapper.apply(outputConsumer, tagMap.get(entry.getKey()));

                Constants.MAIN_LOGGER.debug("Add input as non class file!");
                outputConsumer.addNonClassFiles(entry.getKey(), remapper, resourcePostRemappers);

                Constants.MAIN_LOGGER.debug("Done 1!");
            }

            if (analyzeMapping) ModRemappingAPIImpl.getCurrentContext().getMappingsRegistry().completeMappingsFromTr(remapper.getEnvironment(), srcNamespace);

            if (action != null) action.accept(remapper);
        } catch (Exception e) {
            remapper.finish();
            outputConsumerPaths.forEach(o -> {
                try {
                    o.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            });
            throw new RuntimeException("Failed to remap jar", e);
        } finally {
            remapper.finish();
            outputConsumerPaths.forEach(o -> {
                try {
                    o.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private static boolean useHardMixinRemap(Path inputPath) throws IOException, URISyntaxException {
        try (FileSystem fs = FileUtils.getJarFileSystem(inputPath)) {
            Path manifestPath = fs.getPath("/META-INF/MANIFEST.MF");

            if (!Files.exists(manifestPath)) return false;

            Manifest manifest = new Manifest(Files.newInputStream(manifestPath));
            Attributes mainAttributes = manifest.getMainAttributes();

            return "static".equalsIgnoreCase(mainAttributes.getValue("Fabric-Loom-Mixin-Remap-Type"));
        }
    }
}
