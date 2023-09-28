package net.conczin.multiserver;

import com.mojang.logging.LogUtils;
import net.conczin.multiserver.server.CoMinecraftServer;
import net.conczin.multiserver.server.ServerSettings;
import net.conczin.multiserver.utils.Exceptions;
import net.fabricmc.api.DedicatedServerModInitializer;
import org.slf4j.Logger;

public class MultiServer implements DedicatedServerModInitializer {
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitializeServer() {
        ServerCommands.initialize();

        PrintThread thread = new PrintThread();
        //thread.start();

        try {
            MultiServerManager.launchServer("server1", 25000, ServerSettings.create().setThreads(2).build());
        } catch (Exceptions.ServerAlreadyRunningException | Exceptions.PortInUseException e) {
            throw new RuntimeException(e);
        }
    }

    public static class PrintThread extends Thread {
        @Override
        public void run() {
            //noinspection InfiniteLoopStatement
            while (true) {
                float worstAverageTickTime = 0;
                float averageTickTime = 0;
                for (CoMinecraftServer server : MultiServerManager.SERVERS.values()) {
                    averageTickTime += server.getAverageTickTime();
                    worstAverageTickTime = Math.max(worstAverageTickTime, server.getAverageTickTime());
                }
                LOGGER.info("Average tick time: {} ms", averageTickTime / MultiServerManager.SERVERS.size());
                LOGGER.info("Worst tick time: {} ms", worstAverageTickTime);

                try {
                    //noinspection BusyWait
                    Thread.sleep(1000); // Sleep for 1 second
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
