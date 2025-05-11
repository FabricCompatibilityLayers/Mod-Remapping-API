package io.github.fabriccompatibilitylayers.modremappingapi.impl.utils;

import com.google.common.collect.ImmutableSet;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ClassTransformer;
import net.mine_diver.spasm.api.transform.RawClassTransformer;
import net.mine_diver.spasm.api.transform.TransformationPhase;
import net.mine_diver.spasm.impl.SpASM;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@ApiStatus.Internal
public class TransformerRegistry implements RawClassTransformer {
    private static final Set<ClassTransformer> PRE_TRANSFORMERS = new HashSet<>();
    private static final Set<ClassTransformer> POST_TRANSFORMERS = new HashSet<>();

    @ApiStatus.Internal
    public static byte[] transform(String name, String transformedName, byte[] basicClass) {
        Set<ClassTransformer> transformers = new HashSet<>();

        Set<ClassTransformer> transformerPool = PRE_TRANSFORMERS;

        if (SpASM.getCurrentPhase() == TransformationPhase.AFTER_MIXINS) {
            transformerPool = POST_TRANSFORMERS;
        }

        for (ClassTransformer transformer : transformerPool) {
            if (transformer.handlesClass(name, transformedName)) {
                transformers.add(transformer);
            }
        }

        byte[] modifiedClass = basicClass;

        for (ClassTransformer transformer : transformers) {
            modifiedClass = transformer.transformClass(name, transformedName, modifiedClass);
        }

        return modifiedClass;
    }

    @ApiStatus.Internal
    public static void registerPreTransformer(ClassTransformer transformer) {
        PRE_TRANSFORMERS.add(transformer);
    }

    @ApiStatus.Internal
    public static void registerPostTransformer(ClassTransformer transformer) {
        POST_TRANSFORMERS.add(transformer);
    }

    @Override
    public @NotNull Optional<byte[]> transform(@NotNull ClassLoader classLoader, @NotNull String className, byte @NotNull [] bytes) {
        byte[] modifiedBytes = bytes;
        modifiedBytes = transform(className, className, modifiedBytes);
        return Optional.ofNullable(modifiedBytes);
    }

    @Override
    public @NotNull ImmutableSet<TransformationPhase> getPhases() {
        return ALL_PHASES;
    }
}
