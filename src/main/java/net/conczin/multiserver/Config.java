package net.conczin.multiserver;

import java.util.List;
import java.util.Map;

public final class Config extends JsonConfig {
    private static final Config INSTANCE = loadOrCreate();

    public static Config getInstance() {
        return INSTANCE;
    }

    public String templateWorld = "template";
    public String discordLink = "https://discord.gg/AajGgsKfhn";
    public String discordGuild = "1160515751992639488";
    public int metricsPort = 8000;

    public List<Integer> freePorts = List.of(
            25001,
            25002,
            25003,
            25004,
            25005,
            25006,
            25007,
            25008,
            25009
    );
    public List<Integer> premiumPorts = List.of(
            25010,
            25011,
            25012,
            25013,
            25014
    );

    public Map<String, String> roleColors = Map.of(
            "Supporter", "ORANGE",
            "Diamond", "AQUA",
            "Gold", "GOLD",
            "Iron", "GRAY",
            "default", "WHITE"
    );
    public Map<String, String> roleWelcome = Map.of(
            "Supporter", "Thanks for being a supporter!",
            "Diamond", "Thanks for being a diamond member!",
            "Gold", "Thanks for being a gold member!",
            "Iron", "Thanks for being an iron member!",
            "default", "Welcome to the server! Please consider donating at patreon.com/MultiServer to keep this server running! Fancy boni awaits!"
    );
    public Map<String, Integer> roleThreads = Map.of(
            "Supporter", 2,
            "Diamond", 4,
            "Gold", 3,
            "Iron", 2,
            "default", 1
    );
    public Map<String, Integer> roleViewDistance = Map.of(
            "Supporter", 16,
            "Diamond", 22,
            "Gold", 18,
            "Iron", 14,
            "default", 10
    );
    public Map<String, Integer> roleSimulationDistance = Map.of(
            "Supporter", 11,
            "Diamond", 14,
            "Gold", 12,
            "Iron", 10,
            "default", 8
    );
    public Map<String, Integer> roleChunkTickDistance = Map.of(
            "Supporter", 128,
            "Diamond", 128,
            "Gold", 128,
            "Iron", 128,
            "default", 96
    );
    public Map<String, Integer> roleMSPTTarget = Map.of(
            "Supporter", 27,
            "Diamond", 35,
            "Gold", 30,
            "Iron", 25,
            "default", 15
    );
    public Map<String, Boolean> premiumSlots = Map.of(
            "Supporter", true,
            "Diamond", true,
            "Gold", true,
            "Iron", true,
            "default", false
    );
    public Map<String, Integer> worldSize = Map.of(
            "Supporter", 1000,
            "Diamond", 2000,
            "Gold", 1500,
            "Iron", 1000,
            "default", 500
    );
}
