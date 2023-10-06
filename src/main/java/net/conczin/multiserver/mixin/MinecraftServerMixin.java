package net.conczin.multiserver.mixin;

import net.conczin.multiserver.MultiServer;
import net.conczin.multiserver.server.CoMinecraftServer;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Redirect(method = "<init>(Ljava/lang/Thread;Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;Lnet/minecraft/server/packs/repository/PackRepository;Lnet/minecraft/server/WorldStem;Ljava/net/Proxy;Lcom/mojang/datafixers/DataFixer;Lnet/minecraft/server/Services;Lnet/minecraft/server/level/progress/ChunkProgressListenerFactory;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;backgroundExecutor()Ljava/util/concurrent/ExecutorService;"))
    private ExecutorService multiServer$redirectBackgroundExecutor() {
        MinecraftServer server = ((MinecraftServer) (Object) this);
        if (server instanceof CoMinecraftServer coServerInstance) {
            MultiServer.LOGGER.info("Redirecting executor of {} to a fixed thread pool with 2 threads", coServerInstance.getRoot());
            return Executors.newFixedThreadPool(MultiServer.serverManager.currentSettings.getThreads());
        } else {
            if (server instanceof DedicatedServer dedicatedServer) {
                MultiServer.serverManager.setMainServer(dedicatedServer);
            }
            return Util.backgroundExecutor();
        }
    }
}
