package net.conczin.multiserver.server.dynamic;

import net.conczin.multiserver.MultiServer;
import net.conczin.multiserver.server.CoMinecraftServer;
import net.conczin.multiserver.utils.Exceptions;

public class SleepManager {
    private static final int TICKS_UNTIL_SLEEP = 20 * 60 * 5; // 5 minutes
    private int idle = 0;

    private final CoMinecraftServer server;

    public SleepManager(CoMinecraftServer server) {
        this.server = server;
    }

    public void tick() {
        if (server.getPlayerList().getPlayerCount() > 0) {
            idle = 0;
        } else {
            idle++;
            if (idle > TICKS_UNTIL_SLEEP) {
                MultiServer.LOGGER.info("Server is idle, shutting down");
                try {
                    MultiServer.SERVER_MANAGER.shutdownServer(server.getRoot());
                } catch (Exceptions.ServerDoesNotExistException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
