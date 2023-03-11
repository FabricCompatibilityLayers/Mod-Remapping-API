package fr.catcore.modremapperapi;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.TransformerMixin;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

// Original author is gudenau.
public class TransformerPlugin implements IMixinConfigPlugin {
    static{
        try{
            ClassLoader classLoader = TransformerPlugin.class.getClassLoader();

            // Get Class<KnotClassLoader>
            Class<?> KnotClassLoader = classLoader.getClass();
            if(!KnotClassLoader.getName().equals("net.fabricmc.loader.impl.launch.knot.KnotClassLoader")){
                throw new RuntimeException("ClassLoader was not an instance of KnotClassLoader");
            }

            // Get Class<KnotClassDelegate>
            Field KnotClassLoader$delegate = KnotClassLoader.getDeclaredField("delegate");
            Class<?> KnotClassDelegate = KnotClassLoader$delegate.getType();
            if(!KnotClassDelegate.getName().equals("net.fabricmc.loader.impl.launch.knot.KnotClassDelegate")){
                throw new RuntimeException("KnotClassLoader.delegate is not an instance of KnotClassDelegate");
            }

            // Get KnotClassDelegate instance
            KnotClassLoader$delegate.setAccessible(true);
            Object delegate = KnotClassLoader$delegate.get(classLoader);

            // Get transformer
            Field KnotClassDelegate$mixinTransformer = KnotClassDelegate.getDeclaredField("mixinTransformer");
            KnotClassDelegate$mixinTransformer.setAccessible(true);
            Object transformer = KnotClassDelegate$mixinTransformer.get(delegate);

            // Get Class<MixinTransformer>
            Class<?> MixinTransformer = transformer.getClass();
            if(!MixinTransformer.getName().equals("org.spongepowered.asm.mixin.transformer.MixinTransformer")){
                throw new RuntimeException("FabricMixinTransformerProxy.transformer is not an instance of MixinTransformer");
            }

            // Generate proxy
            Unsafe unsafe = getUnsafe();

            Class<?> MixinTransformerProxy = TransformerMixin.class;

            // Create proxy
            Object proxy = new TransformerMixin();
            Field MixinTransformerProxy$parent = MixinTransformerProxy.getDeclaredField("parent");
            long MixinTransformerProxy$parent$cookie = unsafe.objectFieldOffset(MixinTransformerProxy$parent);
            unsafe.putObject(proxy, MixinTransformerProxy$parent$cookie, transformer);

            // Set proxy
            KnotClassDelegate$mixinTransformer.set(delegate, proxy);
        }catch(IllegalArgumentException | ReflectiveOperationException t){
            throw new RuntimeException("Something went wrong setting up the transformer", t);
        }
    }

    private static Unsafe getUnsafe(){
        try {
            Field Unsafe$theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            Unsafe$theUnsafe.setAccessible(true);
            return (Unsafe) Unsafe$theUnsafe.get(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to get Unsafe instance", e);
        }
    }

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
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo){}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo){}
}
