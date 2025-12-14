package io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper;

import com.google.gson.Gson;
import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.ModCandidate;
import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.RemappingFlags;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.context.ModRemapperContext;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.mappings.MappingTreeHelper;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.mappings.MappingsRegistry;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper.asm.mixin.RefmapBaseMixinExtension;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper.minecraft.MinecraftRemapper;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper.resource.RefmapJson;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper.resource.RefmapRemapper;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper.visitor.MixinPostApplyVisitorProvider;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.utils.FileUtils;
import net.fabricmc.classtweaker.api.ClassTweaker;
import net.fabricmc.classtweaker.api.ClassTweakerReader;
import net.fabricmc.classtweaker.api.ClassTweakerWriter;
import net.fabricmc.classtweaker.api.visitor.ClassTweakerVisitor;
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
        var mappingsRegistry = context.getMappingsRegistry();
    
        var trees = mappingsRegistry.getRemappingMappings();
    
        var builder = TinyRemapper
                .newRemapper()
                .renameInvalidLocals(true)
                .ignoreFieldDesc(false)
                .propagatePrivate(true)
                .ignoreConflicts(true);
    
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            builder.fixPackageAccess(true);
        }
    
        for (var tree : trees) {
            builder.withMappings(MappingTreeHelper.createMappingProvider(tree, mappingsRegistry.getSourceNamespace(), mappingsRegistry.getTargetNamespace()));
        }
    
        context.addToRemapperBuilder(builder);
    
        if (context.getRemappingFlags().contains(RemappingFlags.MIXIN)) {
            builder.extension(new RefmapBaseMixinExtension(inputTag -> !context.getMixinData().getHardMixins().contains(inputTag)));
    
            var mixinPostApplyVisitorProvider = new MixinPostApplyVisitorProvider();
            builder.extraPostApplyVisitor(mixinPostApplyVisitorProvider);
            builder.extension(new MixinExtension(context.getMixinData().getHardMixins()::contains));
        }
    
        var remapper = builder.build();

        try {
            MinecraftRemapper.addMinecraftJar(remapper, mappingsRegistry, context.getCacheHandler());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        context.getLibraryHandler().addLibrariesToRemapClasspath(remapper);

        return remapper;
    }

    public static void remapMods(TinyRemapper remapper, Map<ModCandidate, Path> paths, ModRemapperContext context) {
        var outputConsumerPaths = new ArrayList<OutputConsumerPath>();
    
        if (context.getRemappingFlags().contains(RemappingFlags.MIXIN)) {
            try {
                analyzeRefMaps(paths.keySet(), context);
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    
        var resourcePostRemappers = new ArrayList<OutputConsumerPath.ResourceRemapper>(NonClassCopyMode.FIX_META_INF.remappers);
        if (context.getRemappingFlags().contains(RemappingFlags.MIXIN)) resourcePostRemappers.add(new RefmapRemapper());
    
        var consumer = getRemapperConsumer(paths, context);
    
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
            for (var entry : paths.entrySet()) {
                var candidate = entry.getKey();
                var jarPath = entry.getValue();
    
                if (candidate.getAccessWidenerPath() != null && candidate.getAccessWidener() != null) {
                    try (var fs = FileUtils.getJarFileSystem(jarPath)) {
                        Files.delete(fs.getPath(candidate.getAccessWidenerPath()));
                        Files.write(fs.getPath(candidate.getAccessWidenerPath()), candidate.getAccessWidener());
                    } catch (Throwable t) {
                        throw new RuntimeException("Error while writing remapped access widener for '" + candidate.getId() + "'", t);
                    }
                }
            }
        }
    }

    private static @Nullable Consumer<TinyRemapper> getRemapperConsumer(Map<ModCandidate, Path> paths, ModRemapperContext context) {
        if (context.getRemappingFlags().contains(RemappingFlags.ACCESS_WIDENER)) {
            return (currentRemapper) -> {
                for (var entry : paths.entrySet()) {
                    var candidate = entry.getKey();
    
                    if (candidate.getAccessWidenerPath() != null) {
                        try (var jarFs = FileUtils.getJarFileSystem(candidate.getPath())) {
                            candidate.setAccessWidener(remapAccessWidener(
                                Files.readAllBytes(jarFs.getPath(candidate.getAccessWidenerPath())), 
                                currentRemapper.getRemapper(), 
                                context.getMappingsRegistry().getTargetNamespace()
                            ));
                        } catch (Throwable t) {
                            throw new RuntimeException("Error while remapping access widener for '" + candidate.getId() + "'", t);
                        }
                    }
                }
            };
        }
    
        return null;
    }

    private static byte[] remapAccessWidener(byte[] data, Remapper remapper, String targetNamespace) {
        var header = ClassTweakerReader.readHeader(data);
        var writer = ClassTweakerWriter.create(header.getVersion());
        var remappingDecorator = ClassTweakerVisitor.remap(writer, remapper, header.getNamespace(), targetNamespace);
        var reader = ClassTweakerReader.create(remappingDecorator);
        reader.read(data);
        return writer.getOutput();
    }

    private static final Gson GSON = new Gson();

    private static void analyzeRefMaps(Set<ModCandidate> candidates, ModRemapperContext context) throws IOException, URISyntaxException {
        for (var candidate : candidates) {
            var path = candidate.getPath();
            var files = FileUtils.listPathContent(path);
            
            var refmaps = files.stream()
                .filter(file -> file.contains("refmap") && file.endsWith(".json"))
                .toList();
    
            if (!refmaps.isEmpty()) {
                try (var fs = FileUtils.getJarFileSystem(path)) {
                    for (var refmap : refmaps) {
                        var refmapPath = fs.getPath(refmap);
                        var refmapJson = GSON.fromJson(new String(Files.readAllBytes(refmapPath)), RefmapJson.class);
    
                        refmapJson.remap(
                            context.getMappingsRegistry().getFullMappings(),
                            context.getMappingsRegistry().getSourceNamespace(), 
                            context.getMappingsRegistry().getTargetNamespace()
                        );

                        context.getMixinData().getMixinRefmapData().putAll(refmapJson.mappings);
                    }
                }
            }
        }
    }
}
