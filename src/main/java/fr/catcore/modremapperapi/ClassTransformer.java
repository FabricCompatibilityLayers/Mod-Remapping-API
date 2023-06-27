package fr.catcore.modremapperapi;

import fr.catcore.modremapperapi.api.IClassTransformer;
import net.mine_diver.spasm.api.transform.TransformationResult;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.util.HashSet;
import java.util.Set;

public class ClassTransformer implements net.mine_diver.spasm.api.transform.ClassTransformer {
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
    public @NotNull TransformationResult transform(@NotNull ClassLoader classLoader, @NotNull ClassNode classNode) {
        try {
            String className = classNode.name;

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            classNode.accept(writer);

            byte[] bytes = transform(className, className, writer.toByteArray());

            if (bytes != null) {
                if (classNode.interfaces != null && !classNode.interfaces.isEmpty()) {
                    classNode.interfaces.clear();
                }
                if (classNode.innerClasses != null && !classNode.innerClasses.isEmpty()) {
                    classNode.innerClasses.clear();
                }
                if (classNode.attrs != null && !classNode.attrs.isEmpty()) {
                    classNode.attrs.clear();
                }
                if (classNode.fields != null && !classNode.fields.isEmpty()) {
                    classNode.fields.clear();
                }
                if (classNode.invisibleAnnotations != null && !classNode.invisibleAnnotations.isEmpty()) {
                    classNode.invisibleAnnotations.clear();
                }
                if (classNode.invisibleTypeAnnotations != null && !classNode.invisibleTypeAnnotations.isEmpty()) {
                    classNode.invisibleTypeAnnotations.clear();
                }
                if (classNode.methods != null && !classNode.methods.isEmpty()) {
                    classNode.methods.clear();
                }
                if (classNode.nestMembers != null && !classNode.nestMembers.isEmpty()) {
                    classNode.nestMembers.clear();
                }
                if (classNode.permittedSubclasses != null && !classNode.permittedSubclasses.isEmpty()) {
                    classNode.permittedSubclasses.clear();
                }
                if (classNode.recordComponents != null && !classNode.recordComponents.isEmpty()) {
                    classNode.recordComponents.clear();
                }
                if (classNode.visibleAnnotations != null && !classNode.visibleAnnotations.isEmpty()) {
                    classNode.visibleAnnotations.clear();
                }
                if (classNode.visibleTypeAnnotations != null && !classNode.visibleTypeAnnotations.isEmpty()) {
                    classNode.visibleTypeAnnotations.clear();
                }

                ClassReader reader = new ClassReader(bytes);
                reader.accept(classNode, 0);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            return TransformationResult.PASS;
        }

        return TransformationResult.SUCCESS;
    }
}
