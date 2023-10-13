package net.conczin.multiserver.mixin;

import net.conczin.multiserver.MultiServer;
import net.fabricmc.fabric.impl.biome.modification.BiomeModificationImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("UnstableApiUsage")
@Mixin(BiomeModificationImpl.class)
public class BiomeModificationImlMixin {
    @Inject(method = "finalizeWorldGen(Lnet/minecraft/core/RegistryAccess;)V", at = @At("HEAD"), cancellable = true)
    private void wut$inject(CallbackInfo ci) {
        if (MultiServer.SERVER_MANAGER.getLobbyServer() != null) {
            // TODO: This is a hacky way to prevent fabric from crying about reusing the registries
            ci.cancel();
        }
    }
}
