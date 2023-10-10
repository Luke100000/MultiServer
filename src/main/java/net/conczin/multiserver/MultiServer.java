package net.conczin.multiserver;

import com.mojang.logging.LogUtils;
import net.conczin.multiserver.command.ServerCommands;
import net.conczin.multiserver.velocity.Communication;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;

import java.io.IOException;

public class MultiServer implements DedicatedServerModInitializer {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MOD_ID = "multiServer";

    public static final MultiServerManager SERVER_MANAGER = new MultiServerManager();

    public static Communication communication;

    @Override
    public void onInitializeServer() {
        ServerCommands.initialize();

        // Launch prometheus metrics endpoint
        int port = Config.getInstance().metricsPort;
        if (port > 0) {
            try {
                MetricsEndpoint.launch(port);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Register server tick event
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            SERVER_MANAGER.tick();
        });

        // Register player join event
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            SERVER_MANAGER.onPlayerJoin(handler.player);
        });

        // Open velocity communication
        try {
            communication = new Communication();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
