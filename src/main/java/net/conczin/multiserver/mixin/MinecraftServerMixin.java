package net.conczin.multiserver.mixin;

import net.conczin.multiserver.MultiServer;
import net.conczin.multiserver.server.CoMinecraftServer;
import net.conczin.multiserver.utils.Exceptions;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Redirect(method = "<init>(Ljava/lang/Thread;Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;Lnet/minecraft/server/packs/repository/PackRepository;Lnet/minecraft/server/WorldStem;Ljava/net/Proxy;Lcom/mojang/datafixers/DataFixer;Lnet/minecraft/server/Services;Lnet/minecraft/server/level/progress/ChunkProgressListenerFactory;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;backgroundExecutor()Ljava/util/concurrent/ExecutorService;"))
    private ExecutorService multiServer$redirectBackgroundExecutor() {
        MinecraftServer server = ((MinecraftServer) (Object) this);
        if (server instanceof CoMinecraftServer) {
            return Executors.newFixedThreadPool(MultiServer.SERVER_MANAGER.currentSettings.getThreads());
        } else {
            if (server instanceof DedicatedServer dedicatedServer) {
                MultiServer.SERVER_MANAGER.setMainServer(dedicatedServer);
            }
            return Util.backgroundExecutor();
        }
    }

    @Inject(method = "halt(Z)V", at = @At("HEAD"))
    private void multiServer$injectHalt(boolean bl, CallbackInfo ci) {
        //noinspection ConstantConditions
        if (((MinecraftServer) (Object) this) instanceof DedicatedServer) {
            List<String> servers = MultiServer.SERVER_MANAGER.SERVERS.values().stream().map(CoMinecraftServer::getRoot).toList();
            servers.forEach(root -> {
                try {
                    MultiServer.SERVER_MANAGER.shutdownServer(root);
                } catch (Exceptions.ServerDoesNotExistException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
