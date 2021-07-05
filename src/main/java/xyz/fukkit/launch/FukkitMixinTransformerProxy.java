package xyz.fukkit.launch;

import org.spongepowered.asm.mixin.transformer.FabricMixinTransformerProxy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FukkitMixinTransformerProxy extends FabricMixinTransformerProxy {

    FabricMixinTransformerProxy delegate;

    @Override
    public byte[] transformClassBytes(String name, String transformedName, byte[] basicClass) {
        if (name.startsWith("net.minecraft")) {
            Path path = Paths.get("../build/classes/java/main/" + name.replace('.', '/') + ".class");

            if (Files.exists(path)) {
                try {
                    basicClass = Files.readAllBytes(path);
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            }
        }

        return delegate.transformClassBytes(name, transformedName, basicClass);
    }
}
