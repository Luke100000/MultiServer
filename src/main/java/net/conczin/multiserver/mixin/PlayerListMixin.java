package net.conczin.multiserver.mixin;

import net.conczin.multiserver.server.CoMinecraftServer;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.players.*;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

import static net.minecraft.server.players.PlayerList.*;

@SuppressWarnings("unused")
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Shadow
    public abstract MinecraftServer getServer();

    @Unique
    private File ms$createFile(File file) {
        if (getServer() instanceof CoMinecraftServer server) {
            return new File(server.getRoot() + "/" + file.getName());
        } else {
            return file;
        }
    }

    @Final
    @Mutable
    @Shadow
    private UserBanList bans;

    @Final
    @Mutable
    @Shadow
    private IpBanList ipBans;

    @Final
    @Mutable
    @Shadow
    private ServerOpList ops;

    @Final
    @Mutable
    @Shadow
    private UserWhiteList whitelist;

    @Inject(method = "<init>(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/core/LayeredRegistryAccess;Lnet/minecraft/world/level/storage/PlayerDataStorage;I)V",           at = @At("TAIL"))
    private void ms$injectInit(MinecraftServer minecraftServer, LayeredRegistryAccess<RegistryLayer> layeredRegistryAccess, PlayerDataStorage playerDataStorage, int i, CallbackInfo ci) {
        bans = new UserBanList(ms$createFile(USERBANLIST_FILE));
        ipBans = new IpBanList(ms$createFile(IPBANLIST_FILE));
        ops = new ServerOpList(ms$createFile(OPLIST_FILE));
        whitelist = new UserWhiteList(ms$createFile(WHITELIST_FILE));
    }
}
