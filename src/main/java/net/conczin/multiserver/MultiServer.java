package net.conczin.multiserver;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.DedicatedServerModInitializer;
import org.slf4j.Logger;

public class MultiServer implements DedicatedServerModInitializer {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MOD_ID = "multiServer";

    public static final MultiServerManager serverManager = new MultiServerManager();

    @Override
    public void onInitializeServer() {
        ServerCommands.initialize();
    }
}
