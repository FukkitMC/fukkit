package xyz.fukkit;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

public interface Fukkit {

    ModContainer MOD_CONTAINER = FabricLoader.getInstance().getModContainer("fukkit").get();
}
