package fr.catcore.modremapperapi;

import com.google.common.collect.ImmutableSet;
import fr.catcore.modremapperapi.api.IClassTransformer;
import net.mine_diver.spasm.api.transform.RawClassTransformer;
import net.mine_diver.spasm.api.transform.TransformationPhase;
import net.mine_diver.spasm.impl.SpASM;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ClassTransformer implements RawClassTransformer {
    private static final Set<IClassTransformer> PRE_TRANSFORMERS = new HashSet<>();
    private static final Set<IClassTransformer> POST_TRANSFORMERS = new HashSet<>();

    public static byte[] transform(String name, String transformedName, byte[] basicClass) {
        Set<IClassTransformer> transformers = new HashSet<>();

        Set<IClassTransformer> transformerPool = PRE_TRANSFORMERS;

        if (SpASM.getCurrentPhase() == TransformationPhase.AFTER_MIXINS) {
            transformerPool = POST_TRANSFORMERS;
        }

        for (IClassTransformer transformer : transformerPool) {
            if (transformer.handlesClass(name, transformedName)) {
                transformers.add(transformer);
            }
        }

        byte[] modifiedClass = basicClass;

        for (IClassTransformer transformer : transformers) {
            modifiedClass = transformer.transformClass(name, transformedName, modifiedClass);
        }

        return modifiedClass;
    }

    @Deprecated
    public static void registerTransformer(IClassTransformer transformer) {
        registerPreTransformer(transformer);
    }

    public static void registerPreTransformer(IClassTransformer transformer) {
        PRE_TRANSFORMERS.add(transformer);
    }

    public static void registerPostTransformer(IClassTransformer transformer) {
        POST_TRANSFORMERS.add(transformer);
    }

    @Override
    public @NotNull Optional<byte[]> transform(@NotNull ClassLoader classLoader, @NotNull String className, byte @NotNull [] bytes) {
        byte[] modifiedBytes = bytes;
        modifiedBytes = transform(className, className, modifiedBytes);

        if (modifiedBytes != null) {
            return Optional.of(modifiedBytes);
        }

        return Optional.empty();
    }

    @Override
    public @NotNull ImmutableSet<TransformationPhase> getPhases() {
        return ALL_PHASES;
    }
}
