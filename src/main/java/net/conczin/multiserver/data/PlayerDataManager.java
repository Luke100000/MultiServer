package net.conczin.multiserver.data;

import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public class PlayerDataManager {
    public static PlayerData getPlayerData(ServerLevel level, UUID uuid) {
        ServerLevel overworld = level.getServer().overworld();
        return level.getDataStorage().computeIfAbsent(b -> new PlayerData(b, overworld, uuid), () -> new PlayerData(overworld, uuid), "ms_player_data/" + uuid.toString());
    }
}
