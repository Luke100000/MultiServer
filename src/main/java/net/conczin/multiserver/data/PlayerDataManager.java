package net.conczin.multiserver.data;

import net.conczin.multiserver.MultiServer;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public class PlayerDataManager {
    public static PlayerData getPlayerData(UUID uuid) {
        ServerLevel overworld = MultiServer.SERVER_MANAGER.getLobbyServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(b -> new PlayerData(b, uuid), () -> new PlayerData(uuid), "ms_player_data/" + uuid.toString());
    }
}
