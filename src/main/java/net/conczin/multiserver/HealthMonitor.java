package net.conczin.multiserver;

import com.google.common.collect.Iterables;
import net.conczin.multiserver.server.CoMinecraftServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class HealthMonitor {
    private final MultiServerManager manager;

    public HealthMonitor(MultiServerManager manager) {
        this.manager = manager;
    }

    public List<PrometheusStats> getStats() {
        List<PrometheusStats> stats = new LinkedList<>();

        // Java stats
        stats.add(new PrometheusStats("java_total_memory", Runtime.getRuntime().totalMemory(), Map.of()));
        stats.add(new PrometheusStats("java_max_memory", Runtime.getRuntime().maxMemory(), Map.of()));
        stats.add(new PrometheusStats("java_free_memory", Runtime.getRuntime().freeMemory(), Map.of()));
        stats.add(new PrometheusStats("java_processor_count", Runtime.getRuntime().availableProcessors(), Map.of()));

        // Member stats
        Map<String, Integer> roles = new HashMap<>();
        for (CoMinecraftServer server : manager.SERVERS.values()) {
            server.getPlayerList().getPlayers().forEach(player -> {
                String name = player.getTeam() == null ? "default" : player.getTeam().getName();
                roles.put(name, roles.getOrDefault(name, 0) + 1);
            });
        }
        for (Map.Entry<String, Integer> entry : roles.entrySet()) {
            stats.add(new PrometheusStats("role", entry.getValue(), Map.of("role", entry.getKey())));
        }

        // Manager stats
        stats.add(new PrometheusStats("manager_server_count", manager.SERVERS.size(), Map.of()));

        // Server stats
        populateForServer(stats, manager.getLobbyServer(), "lobby");
        for (CoMinecraftServer server : manager.SERVERS.values()) {
            populateForServer(stats, server, server.getRoot());
        }

        return stats;
    }

    private static void populateForServer(List<PrometheusStats> stats, MinecraftServer server, String name) {
        Map<String, String> globalTags = Map.of("server", name);
        stats.add(new PrometheusStats("minecraft_avg_mspt", server.getAverageTickTime(), globalTags));
        stats.add(new PrometheusStats("minecraft_player_count", server.getPlayerCount(), globalTags));
        for (ServerLevel level : server.getAllLevels()) {
            Map<String, String> tags = Map.of("server", name, "dimension", level.dimension().location().toString());
            stats.add(new PrometheusStats("minecraft_chunk_count", level.getChunkSource().getLoadedChunksCount(), tags));
            stats.add(new PrometheusStats("minecraft_entity_count", Iterables.size(level.getAllEntities()), tags));
        }
    }

    public String getPrometheusReport() {
        return getStats().stream().map(PrometheusStats::toString).reduce((a, b) -> a + "\n" + b).orElse("");
    }

    static class PrometheusStats {
        public String name;
        public Map<String, String> tags;
        public float value;

        public PrometheusStats(String name, float value, Map<String, String> tags) {
            this.name = name;
            this.value = value;
            this.tags = tags;
        }

        @Override
        public String toString() {
            return name + "{" + tags.entrySet().stream().map(e -> e.getKey() + "=\"" + e.getValue() + "\"").reduce((a, b) -> a + "," + b).orElse("") + "} " + value;
        }
    }
}
