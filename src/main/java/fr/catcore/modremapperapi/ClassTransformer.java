package fr.catcore.modremapperapi;

import fr.catcore.modremapperapi.api.IClassTransformer;
import net.mine_diver.spasm.api.transform.RawClassTransformer;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ClassTransformer implements RawClassTransformer {
    private static final Set<IClassTransformer> TRANSFORMERS = new HashSet<>();

    public static byte[] transform(String name, String transformedName, byte[] basicClass){
        Set<IClassTransformer> transformers = new HashSet<>();
        for(IClassTransformer transformer : TRANSFORMERS){
            if(transformer.handlesClass(name, transformedName)){
                transformers.add(transformer);
            }
        }
        byte[] modifiedClass = basicClass;
        for(IClassTransformer transformer : transformers){
            modifiedClass = transformer.transformClass(name, transformedName, modifiedClass);
        }
        return modifiedClass;
    }

    public static void registerTransformer(IClassTransformer transformer){
        TRANSFORMERS.add(transformer);
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
}
