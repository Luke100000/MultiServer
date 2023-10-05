package net.conczin.multiserver;

import net.conczin.multiserver.server.CoMinecraftServer;

public class HealthMonitor {
    public float getAverageTime() {
        float worstAverageTickTime = 0;
        for (CoMinecraftServer server : MultiServer.serverManager.SERVERS.values()) {
            worstAverageTickTime = Math.max(worstAverageTickTime, server.getAverageTickTime());
        }
        return worstAverageTickTime;
    }

    public float getWorstTickTime() {
        float averageTickTime = 0;
        for (CoMinecraftServer server : MultiServer.serverManager.SERVERS.values()) {
            averageTickTime += server.getAverageTickTime();
        }
        return averageTickTime;
    }
}
