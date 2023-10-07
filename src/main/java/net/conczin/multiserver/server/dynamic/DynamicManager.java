package net.conczin.multiserver.server.dynamic;

import net.conczin.multiserver.MultiServer;
import net.conczin.multiserver.server.CoMinecraftServer;

public class DynamicManager {
    private static final int TICKS_PER_UPDATE = 200;
    private static final int TICKS_PER_VIEW_UPDATE = 6000;
    private static final float STEP_SIZE = 0.0625f;

    private static final int MIN_VIEW_DISTANCE = 3;
    private static final int MIN_SIMULATION_DISTANCE = 2;
    private static final int MIN_CHUNK_TICK_DISTANCE = 32;

    private final CoMinecraftServer server;

    private int tick;
    private float quality = 1.0f;
    int lastViewDistanceUpdate = 0;

    public DynamicManager(CoMinecraftServer server) {
        this.server = server;

        // initialize settings
        applyQuality();
    }

    public void tick() {
        tick++;
        if (tick % TICKS_PER_UPDATE == 0 && server.getPlayerList().getPlayerCount() > 0) {
            float mspt = server.getAverageTickTime();
            float target = server.getServerSettings().getMspt();
            if (mspt > target + 5.0f && quality > 0.0f) {
                quality = Math.max(0.0f, quality - STEP_SIZE);
                applyQuality();
                MultiServer.LOGGER.info("Decreased quality to " + quality);
            } else if (mspt < Math.max(2.0f, target - 5.0f) && quality < 1.0f) {
                quality = Math.min(1.0f, quality + STEP_SIZE);
                applyQuality();
                MultiServer.LOGGER.info("Increased quality to " + quality);
            }
        }
    }

    public int getViewDistance() {
        return MIN_VIEW_DISTANCE + (int) ((server.getServerSettings().getTargetViewDistance() - MIN_VIEW_DISTANCE) * quality);
    }

    public int getSimulationDistance() {
        return MIN_SIMULATION_DISTANCE + (int) ((server.getServerSettings().getTargetSimulationDistance() - MIN_SIMULATION_DISTANCE) * quality);
    }

    public int getChunkTickDistance() {
        return MIN_CHUNK_TICK_DISTANCE + (int) ((server.getServerSettings().getTargetChunkTickDistance() - MIN_CHUNK_TICK_DISTANCE) * quality);
    }

    private void applyQuality() {
        // View is a bit heavy as it, for whatever reason, forces a refresh on the clients side
        if (lastViewDistanceUpdate == 0 || tick - lastViewDistanceUpdate > TICKS_PER_VIEW_UPDATE) {
            lastViewDistanceUpdate = tick;
            modifyViewDistance(getViewDistance());
        }
        modifySimulationDistance(getSimulationDistance());
    }

    public void modifyViewDistance(int distance) {
        this.server.getPlayerList().setViewDistance(distance);
    }

    public void modifySimulationDistance(int distance) {
        this.server.getPlayerList().setSimulationDistance(distance);
    }
}
