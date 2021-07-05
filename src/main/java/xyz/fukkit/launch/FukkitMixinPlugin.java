package xyz.fukkit.launch;

import com.google.gson.internal.UnsafeAllocator;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.FabricMixinTransformerProxy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

public class FukkitMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            try {
                Object knot = getClass().getClassLoader();
                Method getDelegate = knot.getClass().getDeclaredMethod("getDelegate");
                getDelegate.setAccessible(true);
                Object delegate = getDelegate.invoke(knot);
                Field mixinTransformer = delegate.getClass().getDeclaredField("mixinTransformer");
                mixinTransformer.setAccessible(true);
                UnsafeAllocator allocator = UnsafeAllocator.create();
                Class<?> cls = FukkitMixinTransformerProxy.class;
                Object proxy = allocator.newInstance(cls);
                ((FukkitMixinTransformerProxy)proxy).delegate = (FabricMixinTransformerProxy) mixinTransformer.get(delegate);
                mixinTransformer.set(delegate, proxy);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}
