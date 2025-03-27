package fr.catcore.modremapperapi;

import fr.catcore.modremapperapi.api.IClassTransformer;

/**
 * @deprecated Use utility methods on {@link io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ClassTransformer} instead.
 */
@Deprecated
public class ClassTransformer {
    /**
     * @deprecated Deprecated in favor of {@link ClassTransformer#registerPreTransformer(IClassTransformer)}.
     * @param transformer
     */
    @Deprecated
    public static void registerTransformer(IClassTransformer transformer) {
        registerPreTransformer(transformer);
    }

    /**
     * @deprecated Use {@link io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ClassTransformer#registerPreTransformer(io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ClassTransformer)} instead.
     */
    @Deprecated
    public static void registerPreTransformer(IClassTransformer transformer) {
        io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ClassTransformer.registerPreTransformer(transformer);
    }

    /**
     * @deprecated Use {@link io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ClassTransformer#registerPostTransformer(io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ClassTransformer)} instead.
     */
    @Deprecated
    public static void registerPostTransformer(IClassTransformer transformer) {
        io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ClassTransformer.registerPostTransformer(transformer);
    }
}
