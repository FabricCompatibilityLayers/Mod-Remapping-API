package io.github.fabriccompatibiltylayers.modremappingapi.api.v1;

import io.github.fabriccompatibiltylayers.modremappingapi.impl.TransformerRegistry;

// Original author is gudenau.
public interface ClassTransformer {
    boolean handlesClass(String name, String transformedName);
    byte[] transformClass(String name, String transformedName, byte[] original);

    static void registerPreTransformer(ClassTransformer transformer) {
        TransformerRegistry.registerPreTransformer(transformer);
    }

    static void registerPostTransformer(ClassTransformer transformer) {
        TransformerRegistry.registerPostTransformer(transformer);
    }
}
