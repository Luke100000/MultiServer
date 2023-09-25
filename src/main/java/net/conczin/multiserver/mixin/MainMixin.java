package net.conczin.multiserver.mixin;

import net.conczin.multiserver.MultiServer;
import net.minecraft.server.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public class MainMixin {
    @Inject(method = "main([Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void multiServer$injectMain(String[] args, CallbackInfo ci) {
        for (int i = 0; i < 16; i++) {
            MultiServer.launchServer(args, "server" + i, 25000 + i);
        }
        ci.cancel();
    }
}
