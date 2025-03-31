package io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper;

import fr.catcore.modremapperapi.utils.Constants;
import net.fabricmc.loader.api.FabricLoader;

public class SoftLockFixer {
    public static void preloadClasses() {
        for (String clazz : new String[]{
                "java.io.IOException",
                "java.net.URI",
                "java.net.URISyntaxException",
                "java.nio.file.FileSystem",
                "java.nio.file.FileVisitResult",
                "java.nio.file.Files",
                "java.nio.file.Path",
                "java.nio.file.SimpleFileVisitor",
                "java.nio.file.attribute.BasicFileAttributes",
                "java.util.ArrayDeque",
                "java.util.ArrayList",
                "java.util.Collection",
                "java.util.Collections",
                "java.util.HashMap",
                "java.util.HashSet",
                "java.util.IdentityHashMap",
                "java.util.List",
                "java.util.Map",
                "java.util.Objects",
                "java.util.Optional",
                "java.util.Queue",
                "java.util.Set",
                "java.util.concurrent.CompletableFuture",
                "java.util.concurrent.ConcurrentHashMap",
                "java.util.concurrent.ExecutionException",
                "java.util.concurrent.ExecutorService",
                "java.util.concurrent.Executors",
                "java.util.concurrent.Future",
                "java.util.concurrent.TimeUnit",
                "java.util.concurrent.atomic.AtomicReference",
                "java.util.function.BiConsumer",
                "java.util.function.Supplier",
                "java.util.regex.Pattern",
                "java.util.stream.Collectors",
                "java.util.zip.ZipError",

                "org.objectweb.asm.ClassReader",
                "org.objectweb.asm.ClassVisitor",
                "org.objectweb.asm.ClassWriter",
                "org.objectweb.asm.FieldVisitor",
                "org.objectweb.asm.MethodVisitor",
                "org.objectweb.asm.Opcodes",
                "org.objectweb.asm.commons.Remapper",
                "org.objectweb.asm.util.CheckClassAdapter",

                "fr.catcore.modremapperapi.api.RemapLibrary",
                "fr.catcore.modremapperapi.api.ModRemapper",
                "fr.catcore.modremapperapi.utils.BArrayList",
                "fr.catcore.modremapperapi.utils.CollectionUtils",
                "fr.catcore.modremapperapi.utils.Constants",
                "fr.catcore.modremapperapi.utils.FileUtils",
                "fr.catcore.modremapperapi.utils.MappingsUtils",
                "fr.catcore.modremapperapi.utils.MixinUtils",
                "fr.catcore.modremapperapi.remapping.MappingBuilder",
                "fr.catcore.modremapperapi.remapping.MappingBuilder$Entry",
                "fr.catcore.modremapperapi.remapping.MappingBuilder$Type",
                "fr.catcore.modremapperapi.remapping.RemapUtil",
                "fr.catcore.modremapperapi.remapping.RemapUtil$MappingList",
                "fr.catcore.modremapperapi.remapping.VisitorInfos",
                "fr.catcore.modremapperapi.remapping.VisitorInfos$MethodNamed",
                "fr.catcore.modremapperapi.remapping.VisitorInfos$MethodValue",
                "fr.catcore.modremapperapi.remapping.VisitorInfos$Type",
                "io.github.fabriccompatibiltylayers.modremappingapi.impl.DefaultModRemapper",
                "io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.resource.RefmapJson",
                "io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.visitor.MixinPostApplyVisitor",
                "io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.asm.MRAClassVisitor",
                "io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.asm.MRAMethodVisitor",
                "io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.visitor.MRAApplyVisitor",
                "io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.resource.RefmapRemapper",
                "io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings.MappingsRegistry",
                "io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings.MappingTreeHelper",
                "io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.ModTrRemapper",
                "io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.TrRemapperHelper",

                "net.fabricmc.loader.impl.launch.FabricLauncher",
                "net.fabricmc.loader.impl.launch.FabricLauncherBase",
                "net.fabricmc.loader.api.ObjectShare",

                getLibClassName("tinyremapper", "AsmClassRemapper"),
                getLibClassName("tinyremapper", "AsmClassRemapper$AsmAnnotationRemapper"),
                getLibClassName("tinyremapper", "AsmClassRemapper$AsmAnnotationRemapper$AsmArrayAttributeAnnotationRemapper"),
                getLibClassName("tinyremapper", "AsmClassRemapper$AsmFieldRemapper"),
                getLibClassName("tinyremapper", "AsmClassRemapper$AsmMethodRemapper"),
                getLibClassName("tinyremapper", "AsmClassRemapper$AsmRecordComponentRemapper"),
                getLibClassName("tinyremapper", "AsmRemapper"),
                getLibClassName("tinyremapper", "BridgeHandler"),
                getLibClassName("tinyremapper", "ClassInstance"),
                getLibClassName("tinyremapper", "FileSystemReference"),
                getLibClassName("tinyremapper", "IMappingProvider"),
                getLibClassName("tinyremapper", "IMappingProvider$MappingAcceptor"),
                getLibClassName("tinyremapper", "IMappingProvider$Member"),
                getLibClassName("tinyremapper", "InputTag"),
                getLibClassName("tinyremapper", "MemberInstance"),
                getLibClassName("tinyremapper", "MetaInfFixer"),
                getLibClassName("tinyremapper", "MetaInfRemover"),
                getLibClassName("tinyremapper", "NonClassCopyMode"),
                getLibClassName("tinyremapper", "OutputConsumerPath"),
                getLibClassName("tinyremapper", "OutputConsumerPath$1"),
                getLibClassName("tinyremapper", "OutputConsumerPath$Builder"),
                getLibClassName("tinyremapper", "OutputConsumerPath$ResourceRemapper"),
                getLibClassName("tinyremapper", "PackageAccessChecker"),
                getLibClassName("tinyremapper", "Propagator"),
                getLibClassName("tinyremapper", "TinyRemapper"),
                getLibClassName("tinyremapper", "TinyRemapper$1"),
                getLibClassName("tinyremapper", "TinyRemapper$1$1"),
                getLibClassName("tinyremapper", "TinyRemapper$2"),
                getLibClassName("tinyremapper", "TinyRemapper$3"),
                getLibClassName("tinyremapper", "TinyRemapper$4"),
                getLibClassName("tinyremapper", "TinyRemapper$5"),
                getLibClassName("tinyremapper", "TinyRemapper$AnalyzeVisitorProvider"),
                getLibClassName("tinyremapper", "TinyRemapper$ApplyVisitorProvider"),
                getLibClassName("tinyremapper", "TinyRemapper$Builder"),
                getLibClassName("tinyremapper", "TinyRemapper$CLIExtensionProvider"),
                getLibClassName("tinyremapper", "TinyRemapper$Direction"),
                getLibClassName("tinyremapper", "TinyRemapper$Extension"),
                getLibClassName("tinyremapper", "TinyRemapper$LinkedMethodPropagation"),
                getLibClassName("tinyremapper", "TinyRemapper$MrjState"),
                getLibClassName("tinyremapper", "TinyRemapper$Propagation"),
                getLibClassName("tinyremapper", "TinyRemapper$StateProcessor"),
                getLibClassName("tinyremapper", "TinyUtils"),
                getLibClassName("tinyremapper", "TinyUtils$MappingAdapter"),
                getLibClassName("tinyremapper", "VisitTrackingClassRemapper"),
                getLibClassName("tinyremapper", "VisitTrackingClassRemapper$VisitKind"),
                getLibClassName("tinyremapper", "extension.mixin.common.IMappable"),
                getLibClassName("tinyremapper", "extension.mixin.common.MapUtility"),
                getLibClassName("tinyremapper", "extension.mixin.common.ResolveUtility"),
                getLibClassName("tinyremapper", "extension.mixin.common.StringUtility"),
                getLibClassName("tinyremapper", "extension.mixin.common.data.Annotation"),
                getLibClassName("tinyremapper", "extension.mixin.common.data.AnnotationElement"),
                getLibClassName("tinyremapper", "extension.mixin.common.data.CommonData"),
                getLibClassName("tinyremapper", "extension.mixin.common.data.Constant"),
                getLibClassName("tinyremapper", "extension.mixin.common.data.Message"),
                getLibClassName("tinyremapper", "extension.mixin.common.data.MxClass"),
                getLibClassName("tinyremapper", "extension.mixin.common.data.MxMember"),
                getLibClassName("tinyremapper", "extension.mixin.common.data.Pair"),
                getLibClassName("tinyremapper", "extension.mixin.soft.SoftTargetMixinClassVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.SoftTargetMixinMethodVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.AccessorAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.InvokerAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.MixinAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.AtAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.AtAnnotationVisitor$AtConstructorMappable"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.AtAnnotationVisitor$AtSecondPassAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.AtAnnotationVisitor$AtSecondPassAnnotationVisitor$1"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.CommonInjectionAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.CommonInjectionAnnotationVisitor$InjectMethodMappable"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.DefinitionAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.DefinitionAnnotationVisitor$MemberRemappingVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.DefinitionsAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.DefinitionsAnnotationVisitor$DefinitionRemappingVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.DescAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.InjectAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.ModifyArgAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.ModifyArgsAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.ModifyConstantAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.ModifyExpressionValueAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.ModifyReceiverAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.ModifyReturnValueAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.ModifyVariableAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.RedirectAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.SliceAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.WrapMethodAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.WrapOperationAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.WrapWithConditionAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.annotation.injection.WrapWithConditionV2AnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.soft.data.MemberInfo"),
                getLibClassName("tinyremapper", "extension.mixin.soft.util.NamedMappable"),
                getLibClassName("tinyremapper", "extension.mixin.hard.HardTargetMixinClassVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.hard.HardTargetMixinFieldVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.hard.HardTargetMixinMethodVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.ImplementsAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.ImplementsAnnotationVisitor$1"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.ImplementsAnnotationVisitor$InterfaceAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.ImplementsAnnotationVisitor$SoftImplementsMappable"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.MixinAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.MixinAnnotationVisitor$1"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.MixinAnnotationVisitor$2"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.OverwriteAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.OverwriteAnnotationVisitor$OverwriteMappable"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.ShadowAnnotationVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.hard.annotation.ShadowAnnotationVisitor$ShadowPrefixMappable"),
                getLibClassName("tinyremapper", "extension.mixin.hard.data.SoftInterface"),
                getLibClassName("tinyremapper", "extension.mixin.hard.data.SoftInterface$Remap"),
                getLibClassName("tinyremapper", "extension.mixin.hard.util.CamelPrefixString"),
                getLibClassName("tinyremapper", "extension.mixin.hard.util.ConvertibleMappable"),
                getLibClassName("tinyremapper", "extension.mixin.hard.util.HardTargetMappable"),
                getLibClassName("tinyremapper", "extension.mixin.hard.util.IConvertibleString"),
                getLibClassName("tinyremapper", "extension.mixin.hard.util.IdentityString"),
                getLibClassName("tinyremapper", "extension.mixin.hard.util.PrefixString"),
                getLibClassName("tinyremapper", "extension.mixin.MixinExtension"),
                getLibClassName("tinyremapper", "extension.mixin.MixinExtension$AnalyzeVisitorProvider"),
                getLibClassName("tinyremapper", "extension.mixin.MixinExtension$AnnotationTarget"),
                getLibClassName("tinyremapper", "extension.mixin.MixinExtension$CLIProvider"),
                getLibClassName("tinyremapper", "extension.mixin.MixinExtension$PreApplyVisitorProvider"),
                getLibClassName("tinyremapper", "api.TrClass"),
                getLibClassName("tinyremapper", "api.TrEnvironment"),
                getLibClassName("tinyremapper", "api.TrField"),
                getLibClassName("tinyremapper", "api.TrLogger"),
                getLibClassName("tinyremapper", "api.TrLogger$Level"),
                getLibClassName("tinyremapper", "api.TrMember"),
                getLibClassName("tinyremapper", "api.TrMember$MemberType"),
                getLibClassName("tinyremapper", "api.TrMethod"),
                getLibClassName("tinyremapper", "api.TrRemapper"),

                getLibClassName("mappingio", "MappingReader"),
                getLibClassName("mappingio", "MappingReader$1"),
                getLibClassName("mappingio", "FlatMappingVisitor"),
                getLibClassName("mappingio", "MappedElementKind"),
                getLibClassName("mappingio", "MappingFlag"),
                getLibClassName("mappingio", "MappingUtil"),
                getLibClassName("mappingio", "MappingVisitor"),
                getLibClassName("mappingio", "MappingWriter"),
                getLibClassName("mappingio", "MappingWriter$1")
        }) {
            try {
                Constants.MAIN_LOGGER.debug("Preloading class: " + clazz);
                Class.forName(clazz);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static String getLibClassName(String lib, String string) {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            return "net.fabricmc." + lib + "." + string;
        }

        return "fr.catcore.modremapperapi.impl.lib." + lib + "." + string;
    }
}
