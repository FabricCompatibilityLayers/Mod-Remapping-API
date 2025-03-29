package io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper;

import io.github.fabriccompatibiltylayers.modremappingapi.impl.context.ModRemapperContext;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings.MappingTreeHelper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings.MappingsRegistry;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.minecraft.MinecraftRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.resource.RefmapRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.visitor.MixinPostApplyVisitor;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.extension.mixin.MixinExtension;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

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
            MixinPostApplyVisitor mixinPostApplyVisitor = new MixinPostApplyVisitor();
            builder.extraPostApplyVisitor(mixinPostApplyVisitor);
            builder.extension(new MixinExtension(EnumSet.of(MixinExtension.AnnotationTarget.HARD)));
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

    public static void remapMods(TinyRemapper remapper, Map<Path, Path> paths, MappingsRegistry mappingsRegistry) {
        List<OutputConsumerPath> outputConsumerPaths = new ArrayList<>();

        List<OutputConsumerPath.ResourceRemapper> resourceRemappers = new ArrayList<>(NonClassCopyMode.FIX_META_INF.remappers);
        resourceRemappers.add(new RefmapRemapper());

        TrRemapperHelper.applyRemapper(remapper, paths, outputConsumerPaths, resourceRemappers, true, mappingsRegistry.getSourceNamespace(), mappingsRegistry.getTargetNamespace());
    }
}
