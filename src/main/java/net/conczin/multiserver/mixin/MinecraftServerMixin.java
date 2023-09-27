package net.conczin.multiserver.mixin;

import net.conczin.multiserver.MultiServer;
import net.conczin.multiserver.MultiServerManager;
import net.conczin.multiserver.server.CoServerInstance;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Redirect(method = "<init>(Ljava/lang/Thread;Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;Lnet/minecraft/server/packs/repository/PackRepository;Lnet/minecraft/server/WorldStem;Ljava/net/Proxy;Lcom/mojang/datafixers/DataFixer;Lnet/minecraft/server/Services;Lnet/minecraft/server/level/progress/ChunkProgressListenerFactory;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;backgroundExecutor()Ljava/util/concurrent/ExecutorService;"))
    private ExecutorService redirectSomeMethod() {
        if (((MinecraftServer) (Object) this) instanceof CoServerInstance coServerInstance) {
            MultiServer.LOGGER.info("Redirecting executor of {} to a fixed thread pool with 2 threads", coServerInstance.getRoot());
            return Executors.newFixedThreadPool(MultiServerManager.currentSettings.getThreads());
        } else {
            return Util.backgroundExecutor();
        }
    }
}
