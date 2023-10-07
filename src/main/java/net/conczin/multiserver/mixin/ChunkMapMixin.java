package net.conczin.multiserver.mixin;

import net.conczin.multiserver.server.CoMinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
    @Shadow
    private static double euclideanDistanceSquared(ChunkPos chunkPos, Entity entity) {
        return 0;
    }

    @Shadow @Final
    ServerLevel level;

    @Inject(method = "playerIsCloseEnoughForSpawning", at = @At(value = "RETURN"), cancellable = true)
    private void servercore$withinChunkTickDistance(ServerPlayer serverPlayer, ChunkPos chunkPos, CallbackInfoReturnable<Boolean> cir) {
        if (level.getServer() instanceof CoMinecraftServer server) {
            if (serverPlayer.isSpectator()) {
                cir.setReturnValue(false);
            }
            double maxDistance = server.getDynamicManager().getChunkTickDistance();
            double d = euclideanDistanceSquared(chunkPos, serverPlayer);
            cir.setReturnValue(d < maxDistance * maxDistance);
        }
    }
}
