package net.conczin.multiserver.server.dynamic;

import net.conczin.multiserver.Config;
import net.conczin.multiserver.MultiServer;
import net.conczin.multiserver.server.CoMinecraftServer;
import net.conczin.multiserver.utils.Exceptions;

public class SleepManager {
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
            if (idle > Config.getInstance().ticksUntilSleep) {
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
