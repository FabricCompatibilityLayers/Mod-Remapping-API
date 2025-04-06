package io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper;

import com.google.gson.Gson;
import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.ModCandidate;
import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.RemappingFlags;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.context.ModRemapperContext;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings.MappingTreeHelper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings.MappingsRegistry;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.asm.mixin.RefmapBaseMixinExtension;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.minecraft.MinecraftRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.resource.RefmapJson;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.resource.RefmapRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.visitor.MixinPostApplyVisitorProvider;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.FileUtils;
import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.accesswidener.AccessWidenerRemapper;
import net.fabricmc.accesswidener.AccessWidenerWriter;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.extension.mixin.MixinExtension;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.commons.Remapper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@ApiStatus.Internal
public class ModTrRemapper {
    public static TinyRemapper makeRemapper(ModRemapperContext context) {
        MappingsRegistry mappingsRegistry = context.getMappingsRegistry();

        List<MappingTree> trees = mappingsRegistry.getRemappingMappings();

        TinyRemapper.Builder builder = TinyRemapper
                .newRemapper()
                .renameInvalidLocals(true)
                .ignoreFieldDesc(false)
                .propagatePrivate(true)
                .ignoreConflicts(true);

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            builder.fixPackageAccess(true);
        }

        for (MappingTree tree : trees) {
            builder.withMappings(MappingTreeHelper.createMappingProvider(tree, mappingsRegistry.getSourceNamespace(), mappingsRegistry.getTargetNamespace()));
        }

        context.addToRemapperBuilder(builder);

        if (context.getRemappingFlags().contains(RemappingFlags.MIXIN)) {
            builder.extension(new RefmapBaseMixinExtension(inputTag -> !context.getMixinData().getHardMixins().contains(inputTag)));

            MixinPostApplyVisitorProvider mixinPostApplyVisitorProvider = new MixinPostApplyVisitorProvider();
            builder.extraPostApplyVisitor(mixinPostApplyVisitorProvider);
            builder.extension(new MixinExtension(context.getMixinData().getHardMixins()::contains));
        }

        TinyRemapper remapper = builder.build();

        try {
            MinecraftRemapper.addMinecraftJar(remapper, mappingsRegistry);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        context.getLibraryHandler().addLibrariesToRemapClasspath(remapper);

        return remapper;
    }

    public static void remapMods(TinyRemapper remapper, Map<io.github.fabriccompatibilitylayers.modremappingapi.api.v2.ModCandidate, Path> paths, ModRemapperContext context) {
        List<OutputConsumerPath> outputConsumerPaths = new ArrayList<>();

        if (context.getRemappingFlags().contains(RemappingFlags.MIXIN)) {
            try {
                analyzeRefMaps(paths.keySet(), context);
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        List<OutputConsumerPath.ResourceRemapper> resourcePostRemappers = new ArrayList<>(NonClassCopyMode.FIX_META_INF.remappers);
        if (context.getRemappingFlags().contains(RemappingFlags.MIXIN)) resourcePostRemappers.add(new RefmapRemapper());

        Consumer<TinyRemapper> consumer = getRemapperConsumer(paths, context);

        TrRemapperHelper.applyRemapper(
                remapper,
                paths.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().getPath(), Map.Entry::getValue)),
                outputConsumerPaths,
                resourcePostRemappers,
                true,
                context.getMappingsRegistry().getSourceNamespace(),
                context.getMappingsRegistry().getTargetNamespace(),
                consumer
        );

        if (context.getRemappingFlags().contains(RemappingFlags.ACCESS_WIDENER)) {
            for (Map.Entry<io.github.fabriccompatibilitylayers.modremappingapi.api.v2.ModCandidate, Path> entry : paths.entrySet()) {
                ModCandidate candidate = entry.getKey();
                Path jarPath = entry.getValue();

                if (candidate.getAccessWidenerPath() != null && candidate.getAccessWidener() != null) {
                    try (FileSystem fs = FileUtils.getJarFileSystem(jarPath)) {
                        Files.delete(fs.getPath(candidate.getAccessWidenerPath()));
                        Files.write(fs.getPath(candidate.getAccessWidenerPath()), candidate.getAccessWidener());
                    } catch (Throwable t) {
                        throw new RuntimeException("Error while writing remapped access widener for '" + candidate.getId() + "'", t);
                    }
                }
            }
        }
    }

    private static @Nullable Consumer<TinyRemapper> getRemapperConsumer(Map<io.github.fabriccompatibilitylayers.modremappingapi.api.v2.ModCandidate, Path> paths, ModRemapperContext context) {
        Consumer<TinyRemapper> consumer = null;

        if (context.getRemappingFlags().contains(RemappingFlags.ACCESS_WIDENER)) {
            consumer = (currentRemapper) -> {
                for (Map.Entry<io.github.fabriccompatibilitylayers.modremappingapi.api.v2.ModCandidate, Path> entry : paths.entrySet()) {
                    io.github.fabriccompatibilitylayers.modremappingapi.api.v2.ModCandidate candidate = entry.getKey();

                    if (candidate.getAccessWidenerPath() != null) {
                        try (FileSystem jarFs = FileUtils.getJarFileSystem(candidate.getPath())) {
                            candidate.setAccessWidener(remapAccessWidener(Files.readAllBytes(jarFs.getPath(candidate.getAccessWidenerPath())), currentRemapper.getRemapper(), context.getMappingsRegistry().getTargetNamespace()));
                        } catch (Throwable t) {
                            throw new RuntimeException("Error while remapping access widener for '" + candidate.getId() + "'", t);
                        }
                    }
                }
            };
        }

        return consumer;
    }

    private static byte[] remapAccessWidener(byte[] data, Remapper remapper, String targetNamespace) {
        AccessWidenerWriter writer = new AccessWidenerWriter();
        AccessWidenerRemapper remappingDecorator = new AccessWidenerRemapper(writer, remapper, "intermediary", targetNamespace);
        AccessWidenerReader accessWidenerReader = new AccessWidenerReader(remappingDecorator);
        accessWidenerReader.read(data, "intermediary");
        return writer.write();
    }

    private static Gson GSON = new Gson();

    private static void analyzeRefMaps(Set<ModCandidate> candidates, ModRemapperContext context) throws IOException, URISyntaxException {
        for (ModCandidate candidate : candidates) {
            Path path = candidate.getPath();

            List<String> files = FileUtils.listPathContent(path);

            List<String> refmaps = new ArrayList<>();

            for (String file : files) {
                if (file.contains("refmap") && file.endsWith(".json")) {
                    refmaps.add(file);
                }
            }

            if (!refmaps.isEmpty()) {
                try (FileSystem fs = FileUtils.getJarFileSystem(path)) {
                    for (String refmap : refmaps) {
                        Path refmapPath = fs.getPath(refmap);

                        RefmapJson refmapJson = GSON.fromJson(new String(Files.readAllBytes(refmapPath)), RefmapJson.class);

                        refmapJson.remap(context.getMappingsRegistry().getFullMappings(), context.getMappingsRegistry().getSourceNamespace(), context.getMappingsRegistry().getTargetNamespace());

                        context.getMixinData().getMixinRefmapData().putAll(refmapJson.mappings);
                    }
                }
            }
        }
    }
}
