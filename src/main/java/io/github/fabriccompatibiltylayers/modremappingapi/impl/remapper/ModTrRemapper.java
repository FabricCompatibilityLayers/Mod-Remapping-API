package io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper;

import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ModRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.LibraryHandler;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.MappingsUtilsImpl;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.ModRemappingAPIImpl;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.VisitorInfosImpl;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings.MappingTreeHelper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings.MappingsRegistry;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.minecraft.MinecraftRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.resource.RefmapRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.visitor.MRAApplyVisitor;
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
    public static TinyRemapper makeRemapper(List<ModRemapper> remappers) {
        MappingsRegistry mappingsRegistry = ModRemappingAPIImpl.getCurrentContext().getMappingsRegistry();

        List<MappingTree> trees = Arrays.asList(
                mappingsRegistry.getFormattedMappings(),
                mappingsRegistry.getModsMappings(), mappingsRegistry.getAdditionalMappings());

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
            builder.withMappings(MappingTreeHelper.createMappingProvider(tree, MappingsUtilsImpl.getSourceNamespace(), MappingsUtilsImpl.getTargetNamespace()));
        }

        MRAApplyVisitor preApplyVisitor = new MRAApplyVisitor();
        MRAApplyVisitor postApplyVisitor = new MRAApplyVisitor();
        MixinPostApplyVisitor mixinPostApplyVisitor = new MixinPostApplyVisitor();

        VisitorInfosImpl preInfos = new VisitorInfosImpl();
        VisitorInfosImpl postInfos = new VisitorInfosImpl();

        for (ModRemapper modRemapper : remappers) {
            modRemapper.registerPreVisitors(preInfos);
            modRemapper.registerPostVisitors(postInfos);
        }

        preApplyVisitor.setInfos(preInfos);
        postApplyVisitor.setInfos(postInfos);

        builder.extraPreApplyVisitor(preApplyVisitor);
        builder.extraPostApplyVisitor(postApplyVisitor);
        builder.extraPostApplyVisitor(mixinPostApplyVisitor);

        builder.extension(new MixinExtension(EnumSet.of(MixinExtension.AnnotationTarget.HARD)));

        TinyRemapper remapper = builder.build();

        try {
            MinecraftRemapper.addMinecraftJar(remapper);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LibraryHandler.addLibrariesToRemapClasspath(remapper);

        return remapper;
    }

    public static void remapMods(TinyRemapper remapper, Map<Path, Path> paths) {
        List<OutputConsumerPath> outputConsumerPaths = new ArrayList<>();

        List<OutputConsumerPath.ResourceRemapper> resourceRemappers = new ArrayList<>(NonClassCopyMode.FIX_META_INF.remappers);
        resourceRemappers.add(new RefmapRemapper());

        TrRemapperHelper.applyRemapper(remapper, paths, outputConsumerPaths, resourceRemappers, true, MappingsUtilsImpl.getSourceNamespace(), MappingsUtilsImpl.getTargetNamespace());
    }
}
