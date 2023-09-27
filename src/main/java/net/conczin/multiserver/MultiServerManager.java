package net.conczin.multiserver;

import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpecBuilder;
import net.conczin.multiserver.server.CoMinecraftServer;
import net.conczin.multiserver.server.ServerSettings;
import net.conczin.multiserver.utils.Exceptions;
import net.minecraft.CrashReport;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.*;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.dedicated.DedicatedServerSettings;
import net.minecraft.server.level.progress.LoggerChunkProgressListener;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.worldupdate.WorldUpgrader;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.WorldData;

import java.io.File;
import java.net.Proxy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.BooleanSupplier;

public class MultiServerManager {
    public static final HashSet<Integer> PORTS = new HashSet<>();
    public static final HashMap<String, CoMinecraftServer> SERVERS = new HashMap<>();
    public static final HashMap<String, ServerSettings> SETTINGS = new HashMap<>();
    public static ServerSettings currentSettings;

    public static String[] args;

    /**
     * @param root     the root directory of the server to launch
     * @param port     the port to bind the server to
     * @param settings the settings to launch the server with
     * @throws Exceptions.ServerAlreadyRunningException a server with the given root is already running
     * @throws Exceptions.PortInUseException            the given port is already in use
     */
    public static void launchServer(String root, int port, ServerSettings settings) throws Exceptions.ServerAlreadyRunningException, Exceptions.PortInUseException {
        if (SERVERS.containsKey(root)) throw new Exceptions.ServerAlreadyRunningException();
        if (PORTS.contains(port)) throw new Exceptions.PortInUseException();

        SETTINGS.put(root, settings);
        CoMinecraftServer dedicatedServer = launchServer(args, root, port);

        if (dedicatedServer != null) {
            SERVERS.put(root, dedicatedServer);
            PORTS.add(port);
        } else {
            SETTINGS.remove(root, settings);
        }
    }

    /**
     * Shuts the server down and frees the port and root dir
     *
     * @param root the root directory of the server to shut down
     * @throws Exceptions.ServerDoesNotExistException a server with the given root does not exist
     */
    public static void shutdownServer(String root) throws Exceptions.ServerDoesNotExistException {
        if (!SERVERS.containsKey(root)) throw new Exceptions.ServerDoesNotExistException();

        CoMinecraftServer server = SERVERS.get(root);
        server.halt(false);

        SERVERS.remove(root);
        PORTS.remove(server.getServerPort());
    }

    public static CoMinecraftServer launchServer(String[] strings, String root, int port) {
        SharedConstants.tryDetectVersion();

        OptionParser optionParser = new OptionParser();
        OptionSpecBuilder demo = optionParser.accepts("demo");
        OptionSpecBuilder bonusChest = optionParser.accepts("bonusChest");
        OptionSpecBuilder forceUpgrade = optionParser.accepts("forceUpgrade");
        OptionSpecBuilder eraseCache = optionParser.accepts("eraseCache");
        OptionSpecBuilder safeMode = optionParser.accepts("safeMode", "Loads level with vanilla datapack only");
        ArgumentAcceptingOptionSpec<String> worldName = optionParser.accepts("world").withRequiredArg();

        try {
            WorldStem worldStem;
            boolean bl;
            OptionSet optionSet = optionParser.parse(strings);
            CrashReport.preload();
            Bootstrap.bootStrap();
            Bootstrap.validate();
            Util.startTimerHackThread();
            Path path2 = Paths.get("server.properties");
            DedicatedServerSettings dedicatedServerSettings = new DedicatedServerSettings(path2);
            dedicatedServerSettings.forceSave();
            File file = new File(root);
            Services services = Services.create(new YggdrasilAuthenticationService(Proxy.NO_PROXY), file);
            String string = Optional.ofNullable(optionSet.valueOf(worldName)).orElse(dedicatedServerSettings.getProperties().levelName);
            LevelStorageSource levelStorageSource = LevelStorageSource.createDefault(file.toPath());
            LevelStorageSource.LevelStorageAccess levelStorageAccess = levelStorageSource.validateAndCreateAccess(string);
            LevelSummary levelSummary = levelStorageAccess.getSummary();
            if (levelSummary != null) {
                if (levelSummary.requiresManualConversion()) {
                    MultiServer.LOGGER.info("This world must be opened in an older version (like 1.6.4) to be safely converted");
                    return null;
                }
                if (!levelSummary.isCompatible()) {
                    MultiServer.LOGGER.info("This world was created by an incompatible version.");
                    return null;
                }
            }
            bl = optionSet.has(safeMode);
            if (bl) {
                MultiServer.LOGGER.warn("Safe mode active, only vanilla datapack will be loaded");
            }
            PackRepository packRepository = ServerPacksSource.createPackRepository(levelStorageAccess);
            worldStem = getWorldStem(demo, bonusChest, bl, optionSet, dedicatedServerSettings, levelStorageAccess, packRepository);

            RegistryAccess.Frozen frozen = worldStem.registries().compositeAccess();
            if (optionSet.has(forceUpgrade)) {
                forceUpgrade(levelStorageAccess, DataFixers.getDataFixer(), optionSet.has(eraseCache), () -> true, frozen.registryOrThrow(Registries.LEVEL_STEM));
            }
            WorldData worldData = worldStem.worldData();
            levelStorageAccess.saveDataTag(frozen, worldData);
            final CoMinecraftServer dedicatedServer = CoMinecraftServer.spinCoServer(thread -> {
                currentSettings = SETTINGS.get(root);
                CoMinecraftServer server = new CoMinecraftServer(thread, root, levelStorageAccess, packRepository, worldStem, dedicatedServerSettings, DataFixers.getDataFixer(), services, LoggerChunkProgressListener::new);
                server.setPort(port);
                server.setDemo(optionSet.has(demo));
                return server;
            });
            Thread thread2 = new Thread("Server Shutdown Thread") {
                @Override
                public void run() {
                    dedicatedServer.halt(true);
                }
            };
            thread2.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(MultiServer.LOGGER));
            Runtime.getRuntime().addShutdownHook(thread2);
            return dedicatedServer;
        } catch (Exception exception2) {
            MultiServer.LOGGER.error(LogUtils.FATAL_MARKER, "Failed to start the minecraft server", exception2);
            return null;
        }
    }

    private static WorldStem getWorldStem(OptionSpecBuilder demo, OptionSpecBuilder bonusChest, boolean bl, OptionSet optionSet, DedicatedServerSettings dedicatedServerSettings, LevelStorageSource.LevelStorageAccess levelStorageAccess, PackRepository packRepository) throws InterruptedException, ExecutionException {
        WorldStem worldStem;
        WorldLoader.InitConfig initConfig = loadOrCreateConfig(dedicatedServerSettings.getProperties(), levelStorageAccess, bl, packRepository);
        worldStem = Util.blockUntilDone(executor -> WorldLoader.load(initConfig, dataLoadContext -> {
            WorldDimensions worldDimensions;
            WorldOptions worldOptions;
            LevelSettings levelSettings;
            Registry<LevelStem> registry = dataLoadContext.datapackDimensions().registryOrThrow(Registries.LEVEL_STEM);
            RegistryOps<Tag> dynamicOps = RegistryOps.create(NbtOps.INSTANCE, dataLoadContext.datapackWorldgen());
            Pair<WorldData, WorldDimensions.Complete> pair = levelStorageAccess.getDataTag(dynamicOps, dataLoadContext.dataConfiguration(), registry, dataLoadContext.datapackWorldgen().allRegistriesLifecycle());
            if (pair != null) {
                return new WorldLoader.DataLoadOutput<>(pair.getFirst(), pair.getSecond().dimensionsRegistryAccess());
            }
            if (optionSet.has(demo)) {
                levelSettings = MinecraftServer.DEMO_SETTINGS;
                worldOptions = WorldOptions.DEMO_OPTIONS;
                worldDimensions = WorldPresets.createNormalWorldDimensions(dataLoadContext.datapackWorldgen());
            } else {
                DedicatedServerProperties dedicatedServerProperties = dedicatedServerSettings.getProperties();
                levelSettings = new LevelSettings(dedicatedServerProperties.levelName, dedicatedServerProperties.gamemode, dedicatedServerProperties.hardcore, dedicatedServerProperties.difficulty, false, new GameRules(), dataLoadContext.dataConfiguration());
                worldOptions = optionSet.has(bonusChest) ? dedicatedServerProperties.worldOptions.withBonusChest(true) : dedicatedServerProperties.worldOptions;
                worldDimensions = dedicatedServerProperties.createDimensions(dataLoadContext.datapackWorldgen());
            }
            WorldDimensions.Complete complete = worldDimensions.bake(registry);
            Lifecycle lifecycle = complete.lifecycle().add(dataLoadContext.datapackWorldgen().allRegistriesLifecycle());
            return new WorldLoader.DataLoadOutput<>(new PrimaryLevelData(levelSettings, worldOptions, complete.specialWorldProperty(), lifecycle), complete.dimensionsRegistryAccess());
        }, WorldStem::new, Util.backgroundExecutor(), executor)).get();
        return worldStem;
    }

    private static WorldLoader.InitConfig loadOrCreateConfig(DedicatedServerProperties dedicatedServerProperties, LevelStorageSource.LevelStorageAccess levelStorageAccess, boolean bl, PackRepository packRepository) {
        WorldDataConfiguration worldDataConfiguration2;
        boolean bl2;
        WorldDataConfiguration worldDataConfiguration = levelStorageAccess.getDataConfiguration();
        if (worldDataConfiguration != null) {
            bl2 = false;
            worldDataConfiguration2 = worldDataConfiguration;
        } else {
            bl2 = true;
            worldDataConfiguration2 = new WorldDataConfiguration(dedicatedServerProperties.initialDataPackConfiguration, FeatureFlags.DEFAULT_FLAGS);
        }
        WorldLoader.PackConfig packConfig = new WorldLoader.PackConfig(packRepository, worldDataConfiguration2, bl, bl2);
        return new WorldLoader.InitConfig(packConfig, Commands.CommandSelection.DEDICATED, dedicatedServerProperties.functionPermissionLevel);
    }

    private static void forceUpgrade(LevelStorageSource.LevelStorageAccess levelStorageAccess, DataFixer dataFixer, boolean bl, BooleanSupplier booleanSupplier, Registry<LevelStem> registry) {
        MultiServer.LOGGER.info("Forcing world upgrade!");
        WorldUpgrader worldUpgrader = new WorldUpgrader(levelStorageAccess, dataFixer, registry, bl);
        net.minecraft.network.chat.Component component = null;
        while (!worldUpgrader.isFinished()) {
            int i;
            Component component2 = worldUpgrader.getStatus();
            if (component != component2) {
                component = component2;
                MultiServer.LOGGER.info(worldUpgrader.getStatus().getString());
            }
            if ((i = worldUpgrader.getTotalChunks()) > 0) {
                int j = worldUpgrader.getConverted() + worldUpgrader.getSkipped();
                MultiServer.LOGGER.info("{}% completed ({} / {} chunks)...", Mth.floor((float) j / (float) i * 100.0f), j, i);
            }
            if (!booleanSupplier.getAsBoolean()) {
                worldUpgrader.cancel();
                continue;
            }
            try {
                //noinspection BusyWait
                Thread.sleep(1000L);
            } catch (InterruptedException ignored) {
                //nop
            }
        }
    }
}
