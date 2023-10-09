package net.conczin.multiserver;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;

public class MultiServer implements DedicatedServerModInitializer {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MOD_ID = "multiServer";

    public static final MultiServerManager SERVER_MANAGER = new MultiServerManager();

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

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            SERVER_MANAGER.onPlayerJoin(handler.player);
        });
    }
}
