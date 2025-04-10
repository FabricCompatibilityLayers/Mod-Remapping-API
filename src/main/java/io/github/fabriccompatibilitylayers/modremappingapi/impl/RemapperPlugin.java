package io.github.fabriccompatibilitylayers.modremappingapi.impl;

import org.jetbrains.annotations.ApiStatus;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

@ApiStatus.Internal
public class RemapperPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage){}

    @Override
    public String getRefMapperConfig(){
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName){
        return false;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets){}

    @Override
    public List<String> getMixins(){
        try {
            ((Runnable)Class.forName("io.github.fabriccompatibilitylayers.modremappingapi.impl.ModRemappingApiInit").newInstance()).run();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo){}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo){}
}
