package io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper;

import fr.catcore.modremapperapi.utils.Constants;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.ModRemappingAPIImpl;
import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@ApiStatus.Internal
public class TrRemapperHelper {
    public static void applyRemapper(TinyRemapper remapper, Map<Path, Path> paths, List<OutputConsumerPath> outputConsumerPaths, List<OutputConsumerPath.ResourceRemapper> resourceRemappers, boolean analyzeMapping, String srcNamespace, String targetNamespace) {
        applyRemapper(remapper, paths, outputConsumerPaths, resourceRemappers, analyzeMapping, srcNamespace, targetNamespace, null);
    }

    public static void applyRemapper(TinyRemapper remapper, Map<Path, Path> paths, List<OutputConsumerPath> outputConsumerPaths, List<OutputConsumerPath.ResourceRemapper> resourceRemappers, boolean analyzeMapping, String srcNamespace, String targetNamespace, @Nullable Consumer<TinyRemapper> action) {
        try {
            Map<Path, InputTag> tagMap = new HashMap<>();

            Constants.MAIN_LOGGER.debug("Creating InputTags!");
            for (Path input : paths.keySet()) {
                InputTag tag = remapper.createInputTag();
                tagMap.put(input, tag);
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
                outputConsumer.addNonClassFiles(entry.getKey(), remapper, resourceRemappers);

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
}
