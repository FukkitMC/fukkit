package xyz.fukkit.mixins.server;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import joptsimple.OptionSet;
import net.minecraft.CrashReport;
import net.minecraft.SharedConstants;
import net.minecraft.SystemUtils;
import net.minecraft.commands.CommandDispatcher;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.server.*;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerSettings;
import net.minecraft.server.level.progress.WorldLoadListenerLogger;
import net.minecraft.server.packs.EnumResourcePackType;
import net.minecraft.server.packs.repository.*;
import net.minecraft.server.players.UserCache;
import net.minecraft.util.datafix.DataConverterRegistry;
import net.minecraft.world.level.DataPackConfiguration;
import net.minecraft.world.level.dimension.WorldDimension;
import net.minecraft.world.level.storage.Convertable;
import net.minecraft.world.level.storage.SavedFile;
import net.minecraft.world.level.storage.WorldInfo;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.awt.*;
import java.io.File;
import java.net.Proxy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Mixin(Main.class)
public class MainMixin {

    @Shadow
    @Final
    private static Logger LOGGER;

    /**
     * @author FukkitMC
     * @reason We still need to get loader to pick the right actual Minecraft sources
     */
    @DontObfuscate
    @Overwrite(remap = false)
    public static void main(String[] args) {
        org.bukkit.craftbukkit.Main.main(args);
    }

    private static void main(final OptionSet optionset) { // CraftBukkit - replaces main(String[] astring)
        SharedConstants.a();

        try {
            CrashReport.h();
            DispenserRegistry.init();
            DispenserRegistry.c();
            SystemUtils.l();
            IRegistryCustom.Dimension iregistrycustom_dimension = IRegistryCustom.a();
            Path path = Paths.get("server.properties");
            DedicatedServerSettings dedicatedserversettings = new DedicatedServerSettings(optionset); // CraftBukkit - CLI argument support

            dedicatedserversettings.save();
            Path path1 = Paths.get("eula.txt");
            EULA eula = new EULA(path1);

            if (optionset.has("initSettings")) { // CraftBukkit
                LOGGER.info("Initialized '{}' and '{}'", path.toAbsolutePath(), path1.toAbsolutePath());
                return;
            }

            if (!eula.a()) {
                LOGGER.info("You need to agree to the EULA in order to run the server. Go to eula.txt for more info.");
                return;
            }

            File file = (File) optionset.valueOf("universe"); // CraftBukkit
            YggdrasilAuthenticationService yggdrasilauthenticationservice = new YggdrasilAuthenticationService(Proxy.NO_PROXY);
            MinecraftSessionService minecraftsessionservice = yggdrasilauthenticationservice.createMinecraftSessionService();
            GameProfileRepository gameprofilerepository = yggdrasilauthenticationservice.createProfileRepository();
            UserCache usercache = new UserCache(gameprofilerepository, new File(file, MinecraftServer.USERID_CACHE_FILE.getName()));
            // CraftBukkit start
            String s = (String) Optional.ofNullable((String) optionset.valueOf("world")).orElse(dedicatedserversettings.getProperties().levelName);
            Convertable convertable = Convertable.a(file.toPath());
            Convertable.ConversionSession convertable_conversionsession = convertable.c(s, WorldDimension.OVERWORLD);

            MinecraftServer.convertWorld(convertable_conversionsession);
            WorldInfo worldinfo = convertable_conversionsession.d();

            if (worldinfo != null && worldinfo.p()) {
                LOGGER.info("Loading of worlds with extended height is disabled.");
                return;
            }

            DataPackConfiguration datapackconfiguration = convertable_conversionsession.e();
            boolean flag = optionset.has("safeMode");

            if (flag) {
                LOGGER.warn("Safe mode active, only vanilla datapack will be loaded");
            }

            ResourcePackRepository resourcepackrepository = new ResourcePackRepository(EnumResourcePackType.SERVER_DATA, new ResourcePackSource[]{new ResourcePackSourceVanilla(), new ResourcePackSourceFolder(convertable_conversionsession.getWorldFolder(SavedFile.DATAPACK_DIR).toFile(), PackSource.WORLD)});
            // CraftBukkit start
            File bukkitDataPackFolder = new File(convertable_conversionsession.getWorldFolder(SavedFile.DATAPACK_DIR).toFile(), "bukkit");
            if (!bukkitDataPackFolder.exists()) {
                bukkitDataPackFolder.mkdirs();
            }
            File mcMeta = new File(bukkitDataPackFolder, "pack.mcmeta");
            try {
                com.google.common.io.Files.write("{\n"
                        + "    \"pack\": {\n"
                        + "        \"description\": \"Data pack for resources provided by Bukkit plugins\",\n"
                        + "        \"pack_format\": " + SharedConstants.getGameVersion().getPackVersion() + "\n"
                        + "    }\n"
                        + "}\n", mcMeta, com.google.common.base.Charsets.UTF_8);
            } catch (java.io.IOException ex) {
                throw new RuntimeException("Could not initialize Bukkit datapack", ex);
            }
            // CraftBukkit end
            DataPackConfiguration datapackconfiguration1 = MinecraftServer.a(resourcepackrepository, datapackconfiguration == null ? DataPackConfiguration.DEFAULT : datapackconfiguration, flag);
            CompletableFuture completablefuture = DataPackResources.a(resourcepackrepository.f(), iregistrycustom_dimension, CommandDispatcher.ServerType.DEDICATED, dedicatedserversettings.getProperties().functionPermissionLevel, SystemUtils.f(), Runnable::run);

            DataPackResources datapackresources;

            try {
                datapackresources = (DataPackResources) completablefuture.get();
            } catch (Exception exception) {
                LOGGER.warn("Failed to load datapacks, can't proceed with server load. You can either fix your datapacks or reset to vanilla with --safeMode", exception);
                resourcepackrepository.close();
                return;
            }

            datapackresources.j();
            final DedicatedServer dedicatedserver = (DedicatedServer) MinecraftServer.a((thread) -> {
                DedicatedServer dedicatedserver1 = new DedicatedServer(optionset, datapackconfiguration1, thread, iregistrycustom_dimension, convertable_conversionsession, resourcepackrepository, datapackresources, null, dedicatedserversettings, DataConverterRegistry.a(), minecraftsessionservice, gameprofilerepository, usercache, WorldLoadListenerLogger::new);
                boolean flag1 = !optionset.has("nogui") && !optionset.nonOptionArguments().contains("nogui");

                if (flag1 && !GraphicsEnvironment.isHeadless()) {
                    dedicatedserver1.bi();
                }

                if (optionset.has("port")) {
                    int port = (Integer) optionset.valueOf("port");
                    if (port > 0) {
                        dedicatedserver1.setPort(port);
                    }
                }

                return dedicatedserver1;
            });
        } catch (Exception exception1) {
            LOGGER.fatal("Failed to start the minecraft server", exception1);
        }
    }
}
