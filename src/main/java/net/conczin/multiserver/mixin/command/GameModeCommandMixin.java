package net.conczin.multiserver.mixin.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameModeArgument;
import net.minecraft.server.commands.GameModeCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import java.util.Collection;
import java.util.Collections;

@Mixin(GameModeCommand.class)
public abstract class GameModeCommandMixin {
    @Shadow
    private static int setMode(CommandContext<CommandSourceStack> commandContext, Collection<ServerPlayer> collection, GameType gameType) {
        return 0;
    }

    @Inject(method = "register(Lcom/mojang/brigadier/CommandDispatcher;)V", at = @At("HEAD"), cancellable = true)
    private static void ms$register(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        dispatcher.register(Commands.literal("gamemode").requires(commandSourceStack -> commandSourceStack.hasPermission(1)).then((Commands.argument("gamemode", GameModeArgument.gameMode()).executes(commandContext -> setMode(commandContext, Collections.singleton(commandContext.getSource().getPlayerOrException()), GameModeArgument.getGameMode(commandContext, "gamemode")))).then(Commands.argument("target", EntityArgument.players()).executes(commandContext -> setMode(commandContext, EntityArgument.getPlayers(commandContext, "target"), GameModeArgument.getGameMode(commandContext, "gamemode"))))));
        ci.cancel();
    }
}
