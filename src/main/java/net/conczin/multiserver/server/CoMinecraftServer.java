package net.conczin.multiserver.server;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import net.conczin.multiserver.server.dynamic.DynamicManager;
import net.conczin.multiserver.server.dynamic.SleepManager;
import net.minecraft.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.*;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.dedicated.DedicatedServerSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.level.progress.LoggerChunkProgressListener;
import net.minecraft.server.network.TextFilter;
import net.minecraft.server.network.TextFilterClient;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.server.rcon.RconConsoleSource;
import net.minecraft.server.rcon.thread.QueryThreadGs4;
import net.minecraft.server.rcon.thread.RconThread;
import net.minecraft.util.monitoring.jmx.MinecraftServerStatistics;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.profiling.jfr.callback.ProfiledDuration;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

public class CoMinecraftServer extends MinecraftServer implements ServerInterface {
    static final Logger LOGGER = LogUtils.getLogger();
    private final List<ConsoleInput> consoleInput = Collections.synchronizedList(Lists.newArrayList());
    private final String root;
    private final ServerSettings serverSettings;
    private final Consumer<CoMinecraftServer> launchCallback;
    @Nullable
    private QueryThreadGs4 queryThreadGs4;
    private final RconConsoleSource rconConsoleSource;
    @Nullable
    private RconThread rconThread;
    private final DedicatedServerSettings settings;
    @Nullable
    private final TextFilterClient textFilterClient;

    public DynamicManager dynamicManager;
    public SleepManager sleepManager = new SleepManager(this);

    public CoMinecraftServer(Thread thread, String root, ServerSettings serverSettings, Consumer<CoMinecraftServer> launchCallback, LevelStorageSource.LevelStorageAccess levelStorageAccess, PackRepository packRepository, WorldStem worldStem, DedicatedServerSettings settings, DataFixer dataFixer, Services services, ChunkProgressListenerFactory chunkProgressListenerFactory) {
        super(thread, levelStorageAccess, packRepository, worldStem, Proxy.NO_PROXY, dataFixer, services, chunkProgressListenerFactory);
        this.root = root;
        this.serverSettings = serverSettings;
        this.launchCallback = launchCallback;
        this.settings = settings;
        this.rconConsoleSource = new RconConsoleSource(this);
        this.textFilterClient = TextFilterClient.createFromConfig(settings.getProperties().textFilteringConfig);
    }

    public static <S extends CoMinecraftServer> S spinCoServer(Function<Thread, S> function) {
        AtomicReference<CoMinecraftServer> atomicReference = new AtomicReference<>();
        Thread serverThread = new Thread(() -> atomicReference.get().runServer(), "Server thread");
        serverThread.setUncaughtExceptionHandler((thread, throwable) -> LOGGER.error("Uncaught exception in server thread", throwable));
        serverThread.setPriority(7);
        S server = function.apply(serverThread);
        atomicReference.set(server);
        serverThread.start();
        return server;
    }

    @Override
    protected void loadLevel() {
        ProfiledDuration profiledDuration = JvmProfiler.INSTANCE.onWorldLoadedStarted();
        this.worldData.setModdedInfo(this.getServerModName(), this.getModdedStatus().shouldReportAsModified());
        this.createLevels(new LoggerChunkProgressListener(1));
        this.forceDifficulty();
        if (profiledDuration != null) {
            profiledDuration.finish();
        }
    }

    @Override
    public boolean initServer() throws IOException {
        // Launch console for admin input
        if (serverSettings.hasConsole()) {
            launchConsole();
        }

        LOGGER.info("Starting minecraft server version {}", SharedConstants.getCurrentVersion().getName());

        DedicatedServerProperties properties = this.settings.getProperties();
        if (this.isSingleplayer()) {
            this.setLocalIp("127.0.0.1");
        } else {
            this.setUsesAuthentication(properties.onlineMode);
            this.setPreventProxyConnections(properties.preventProxyConnections);
            this.setLocalIp(properties.serverIp);
        }

        this.setPvpAllowed(properties.pvp);
        this.setFlightAllowed(properties.allowFlight);
        this.setMotd(properties.motd);
        super.setPlayerIdleTimeout(properties.playerIdleTimeout.get());
        this.setEnforceWhitelist(properties.enforceWhitelist);
        this.worldData.setGameType(properties.gamemode);

        InetAddress inetAddress = null;
        if (!this.getLocalIp().isEmpty()) {
            inetAddress = InetAddress.getByName(this.getLocalIp());
        }

        if (this.getPort() < 0) {
            this.setPort(properties.serverPort);
        }

        this.initializeKeyPair();
        LOGGER.info("Starting Minecraft server on {}:{}", this.getLocalIp().isEmpty() ? "*" : this.getLocalIp(), this.getPort());

        try {
            //noinspection ConstantConditions
            this.getConnection().startTcpServerListener(inetAddress, this.getPort());
        } catch (IOException var10) {
            LOGGER.warn("**** FAILED TO BIND TO PORT!");
            LOGGER.warn("The exception was: {}", var10.toString());
            LOGGER.warn("Perhaps a server is already running on that port?");
            return false;
        }

        if (!OldUsersConverter.serverReadyAfterUserconversion(this)) {
            return false;
        } else {
            synchronized (CoServerPlayerList.class) {
                this.setPlayerList(new CoServerPlayerList(this, this.registries(), this.playerDataStorage));
            }
            this.dynamicManager = new DynamicManager(this);

            long time = Util.getNanos();

            SkullBlockEntity.setup(this.services, this);
            GameProfileCache.setUsesAuthentication(this.usesAuthentication());

            LOGGER.info("Preparing level \"{}\"", this.getLevelIdName());
            this.loadLevel();

            long m = Util.getNanos() - time;
            String string = String.format(Locale.ROOT, "%.3fs", (double) m / 1.0E9);
            LOGGER.info("Done ({})! For help, type \"help\"", string);

            launchCallback.accept(this);

            if (properties.announcePlayerAchievements != null) {
                this.getGameRules().getRule(GameRules.RULE_ANNOUNCE_ADVANCEMENTS).set(properties.announcePlayerAchievements, this);
            }

            if (properties.enableQuery) {
                LOGGER.info("Starting GS4 status listener");
                this.queryThreadGs4 = QueryThreadGs4.create(this);
            }

            if (properties.enableRcon) {
                LOGGER.info("Starting remote control listener");
                this.rconThread = RconThread.create(this);
            }

            if (this.getMaxTickLength() > 0L) {
                Thread thread2 = new Thread(new CoServerWatchDog(this));
                thread2.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandlerWithName(LOGGER));
                thread2.setName("Server Watchdog");
                thread2.setDaemon(true);
                thread2.start();
            }

            if (properties.enableJmxMonitoring) {
                MinecraftServerStatistics.registerJmxMonitoring(this);
                LOGGER.info("JMX monitoring enabled");
            }

            return true;
        }
    }

    private void launchConsole() {
        Thread thread = new Thread("Server console handler") {
            @Override
            public void run() {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

                String string;
                try {
                    while (!CoMinecraftServer.this.isStopped() && CoMinecraftServer.this.isRunning() && (string = bufferedReader.readLine()) != null) {
                        CoMinecraftServer.this.handleConsoleInput(string, CoMinecraftServer.this.createCommandSourceStack());
                    }
                } catch (IOException var4) {
                    CoMinecraftServer.LOGGER.error("Exception handling console input", var4);
                }

            }
        };
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
        thread.start();
    }

    @Override
    public boolean isSpawningAnimals() {
        return this.getProperties().spawnAnimals && super.isSpawningAnimals();
    }

    @Override
    public boolean isSpawningMonsters() {
        return this.settings.getProperties().spawnMonsters && super.isSpawningMonsters();
    }

    @Override
    public boolean areNpcsEnabled() {
        return this.settings.getProperties().spawnNpcs && super.areNpcsEnabled();
    }

    public DedicatedServerProperties getProperties() {
        return this.settings.getProperties();
    }

    @Override
    public void forceDifficulty() {
        this.setDifficulty(this.getProperties().difficulty, true);
    }

    @Override
    public boolean isHardcore() {
        return this.getProperties().hardcore;
    }

    public SystemReport fillServerSystemReport(SystemReport systemReport) {
        systemReport.setDetail("Is Modded", () -> this.getModdedStatus().fullDescription());
        systemReport.setDetail("Type", () -> "CoServer");
        return systemReport;
    }

    @Override
    public void onServerExit() {
        if (this.textFilterClient != null) {
            this.textFilterClient.close();
        }

        if (this.rconThread != null) {
            this.rconThread.stop();
        }

        if (this.queryThreadGs4 != null) {
            this.queryThreadGs4.stop();
        }
    }

    @Override
    public void tickChildren(@NotNull BooleanSupplier booleanSupplier) {
        super.tickChildren(booleanSupplier);

        if (this.dynamicManager != null) {
            this.dynamicManager.tick();
        }

        fixPlayerLocation();

        this.sleepManager.tick();
    }

    private void fixPlayerLocation() {
        double absoluteMaxWorldSize = Math.pow(this.getAbsoluteMaxWorldSize() + 2.0, 2.0);
        for (ServerPlayer player : this.getPlayerList().getPlayers()) {
            double distance = player.getX() * player.getX() + player.getZ() * player.getZ();
            if (distance > absoluteMaxWorldSize) {
                player.sendSystemMessage(Component.literal("Whoops, you reached the end of the world."));
                this.getPlayerList().respawn(player, true);
            }
        }
    }

    @Override
    public boolean isNetherEnabled() {
        return this.getProperties().allowNether;
    }

    public void handleConsoleInput(String string, CommandSourceStack commandSourceStack) {
        this.consoleInput.add(new ConsoleInput(string, commandSourceStack));
    }

    public int getRateLimitPacketsPerSecond() {
        return this.getProperties().rateLimitPacketsPerSecond;
    }

    public boolean isEpollEnabled() {
        return this.getProperties().useNativeTransport;
    }

    @Override
    public CoServerPlayerList getPlayerList() {
        return (CoServerPlayerList) super.getPlayerList();
    }

    public boolean isPublished() {
        return true;
    }

    public String getServerIp() {
        return this.getLocalIp();
    }

    public int getServerPort() {
        return this.getPort();
    }

    public String getServerName() {
        return this.getMotd();
    }

    public boolean isCommandBlockEnabled() {
        return this.getProperties().enableCommandBlock;
    }

    @Override
    public int getSpawnProtectionRadius() {
        return this.getProperties().spawnProtection;
    }

    @Override
    public boolean isUnderSpawnProtection(@NotNull ServerLevel serverLevel, @NotNull BlockPos blockPos, @NotNull Player player) {
        return false;
    }

    @Override
    public boolean repliesToStatus() {
        return this.getProperties().enableStatus;
    }

    @Override
    public boolean hidesOnlinePlayers() {
        return this.getProperties().hideOnlinePlayers;
    }

    public int getOperatorUserPermissionLevel() {
        return this.getProperties().opPermissionLevel;
    }

    public int getFunctionCompilationLevel() {
        return this.getProperties().functionPermissionLevel;
    }

    @Override
    public void setPlayerIdleTimeout(int i) {
        super.setPlayerIdleTimeout(i);
        this.settings.update(properties -> properties.playerIdleTimeout.update(this.registryAccess(), i));
    }

    public boolean shouldRconBroadcast() {
        return this.getProperties().broadcastRconToOps;
    }

    public boolean shouldInformAdmins() {
        return this.getProperties().broadcastConsoleToOps;
    }

    @Override
    public int getAbsoluteMaxWorldSize() {
        return getServerSettings().getWorldSize();
    }

    @Override
    public int getCompressionThreshold() {
        return this.getProperties().networkCompressionThreshold;
    }

    @Override
    public boolean enforceSecureProfile() {
        DedicatedServerProperties properties = this.getProperties();
        return properties.enforceSecureProfile && properties.onlineMode && this.services.profileKeySignatureValidator() != null;
    }

    public long getMaxTickLength() {
        return this.getProperties().maxTickTime;
    }

    @Override
    public int getMaxChainedNeighborUpdates() {
        return this.getProperties().maxChainedNeighborUpdates;
    }

    public String getPluginNames() {
        return "";
    }

    public String runCommand(@NotNull String string) {
        this.rconConsoleSource.prepareForCommand();
        this.executeBlocking(() -> this.getCommands().performPrefixedCommand(this.rconConsoleSource.createCommandSourceStack(), string));
        return this.rconConsoleSource.getCommandResponse();
    }

    public void storeUsingWhiteList(boolean bl) {
        this.settings.update(properties -> properties.whiteList.update(this.registryAccess(), bl));
    }

    public boolean isSingleplayerOwner(@NotNull GameProfile gameProfile) {
        return false;
    }

    @Override
    public int getScaledTrackingDistance(int i) {
        return this.getProperties().entityBroadcastRangePercentage * i / 100;
    }

    public String getLevelIdName() {
        return this.storageSource.getLevelId();
    }

    @Override
    public boolean forceSynchronousWrites() {
        return this.settings.getProperties().syncChunkWrites;
    }

    @Override
    public TextFilter createTextFilterForPlayer(@NotNull ServerPlayer serverPlayer) {
        return this.textFilterClient != null ? this.textFilterClient.createContext(serverPlayer.getGameProfile()) : TextFilter.DUMMY;
    }

    @Override
    @Nullable
    public GameType getForcedGameType() {
        return this.settings.getProperties().forceGameMode ? this.worldData.getGameType() : null;
    }

    @Override
    public Optional<ServerResourcePackInfo> getServerResourcePack() {
        return this.settings.getProperties().serverResourcePackInfo;
    }

    @Override
    public boolean isDedicatedServer() {
        return true;
    }

    public String getRoot() {
        return root;
    }

    public ServerSettings getServerSettings() {
        return serverSettings;
    }

    public DynamicManager getDynamicManager() {
        return dynamicManager;
    }
}
