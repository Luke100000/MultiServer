package net.conczin.multiserver.utils;

import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class Utils {
    public static String playerToWordRoot(Player player) {
        return uuidToWorldRoot(player.getUUID());
    }
    public static String uuidToWorldRoot(UUID player) {
        return "u_" + player.toString();
    }
}
