package net.conczin.multiserver.mixin;

import net.conczin.multiserver.server.CoServerPlayerList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.io.File;

import static net.minecraft.server.players.PlayerList.*;

@SuppressWarnings("unused")
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Shadow
    @Final
    private MinecraftServer server;

    @Shadow
    public abstract MinecraftServer getServer();

    @Unique
    private File ms$createFile(File file) {
        if (CoServerPlayerList.CURRENT_SERVER != null) {
            return new File(CoServerPlayerList.CURRENT_SERVER.getRoot() + "/" + file.getName());
        } else {
            return file;
        }
    }

    @Final
    @Shadow
    private final UserBanList bans = new UserBanList(ms$createFile(USERBANLIST_FILE));

    @Final
    @Shadow
    private final IpBanList ipBans = new IpBanList(ms$createFile(IPBANLIST_FILE));

    @Final
    @Shadow
    private final ServerOpList ops = new ServerOpList(ms$createFile(OPLIST_FILE));

    @Final
    @Shadow
    private final UserWhiteList whitelist = new UserWhiteList(ms$createFile(WHITELIST_FILE));
}
