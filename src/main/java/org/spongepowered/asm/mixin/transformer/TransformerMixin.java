package org.spongepowered.asm.mixin.transformer;

import fr.catcore.modremapperapi.ClassTransformer;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.ext.IExtensionRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Original author is gudenau.
public class TransformerMixin implements IMixinTransformer {
    private IMixinTransformer parent = null;

    private List<String> loaded = new ArrayList<>();

    public TransformerMixin() {}

    @Override
    public void audit(MixinEnvironment environment) {
        this.parent.audit(environment);
    }

    @Override
    public List<String> reload(String mixinClass, ClassNode classNode) {
        return this.parent.reload(mixinClass, classNode);
    }

    @Override
    public boolean computeFramesForClass(MixinEnvironment environment, String name, ClassNode classNode) {
        return this.parent.computeFramesForClass(environment, name, classNode);
    }

    @Override
    public byte[] transformClassBytes(String name, String transformedName, byte[] basicClass) {
        if (!loaded.contains(name)) {
            loaded.add(name);
            byte[] bytes = this.parent.transformClassBytes(name, transformedName, basicClass);
            return ClassTransformer.transform(name, transformedName, bytes);
        }

        return basicClass;
    }

    @Override
    public byte[] transformClass(MixinEnvironment environment, String name, byte[] classBytes) {
        return this.parent.transformClass(environment, name, classBytes);
    }

    @Override
    public boolean transformClass(MixinEnvironment environment, String name, ClassNode classNode) {
        return this.parent.transformClass(environment, name, classNode);
    }

    @Override
    public byte[] generateClass(MixinEnvironment environment, String name) {
        return this.parent.generateClass(environment, name);
    }

    @Override
    public boolean generateClass(MixinEnvironment environment, String name, ClassNode classNode) {
        return this.parent.generateClass(environment, name, classNode);
    }

    @Override
    public IExtensionRegistry getExtensions() {
        return this.parent.getExtensions();
    }
}
