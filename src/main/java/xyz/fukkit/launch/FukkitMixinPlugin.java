package xyz.fukkit.launch;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import com.google.gson.internal.UnsafeAllocator;
import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.tree.RootCommandNode;
import io.netty.util.internal.shaded.org.jctools.util.UnsafeAccess;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.FabricMixinTransformerProxy;

import net.fabricmc.loader.api.FabricLoader;

public class FukkitMixinPlugin implements IMixinConfigPlugin {
	static {
		try {
			if(FabricLoader.getInstance().isDevelopmentEnvironment()) {
				Object knot = FukkitMixinPlugin.class.getClassLoader();
				Method getDelegate = knot.getClass().getDeclaredMethod("getDelegate");
				getDelegate.setAccessible(true);
				Object delegate = getDelegate.invoke(knot);
				Field mixinTransformer = delegate.getClass().getDeclaredField("mixinTransformer");
				mixinTransformer.setAccessible(true);
				UnsafeAllocator allocator = UnsafeAllocator.create();
				Class<?> cls = FukkitMixinTransformerProxy.class;
				Object proxy = allocator.newInstance(cls);
				((FukkitMixinTransformerProxy) proxy).delegate = (FabricMixinTransformerProxy) mixinTransformer.get(delegate);
				mixinTransformer.set(delegate, proxy);
			}
			Class<?> cls = ImmutableStringReader.class;
			try {
				changePackage(cls, "com.mojang.brigadier.tree");
				MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(cls, MethodHandles.lookup());
				bootstrap("com/mojang/brigadier/tree/CommandNode", lookup);
			} finally {
				changePackage(cls, "com.mojang.brigadier");
			}
		} catch(Throwable throwable) {
			throw new RuntimeException(throwable);
		}
	}

	public static void changePackage(Class<?> cls, String pkg) throws Throwable {
		long offset = UnsafeAccess.UNSAFE.objectFieldOffset(Class.class.getDeclaredField("packageName"));
		UnsafeAccess.UNSAFE.getAndSetObject(cls, offset, pkg);
	}

	public static void bootstrap(String cls, MethodHandles.Lookup loader) throws IOException, IllegalAccessException {
        Path path = Paths.get("").toAbsolutePath().getParent().resolve("build/classes/java/main/" + cls + ".class");
        byte[] code = Files.readAllBytes(path);
        Class<?> type = loader.defineClass(code);
        System.out.println();
	}

	@Override
	public void onLoad(String mixinPackage) {

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
