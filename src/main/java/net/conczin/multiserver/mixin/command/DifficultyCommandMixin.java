package net.conczin.multiserver.mixin.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.DifficultyCommand;
import net.minecraft.world.Difficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(DifficultyCommand.class)
public abstract class DifficultyCommandMixin {
    @Inject(method = "register(Lcom/mojang/brigadier/CommandDispatcher;)V", at = @At("HEAD"), cancellable = true)
    private static void ms$register(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        LiteralArgumentBuilder<CommandSourceStack> literalArgumentBuilder = Commands.literal("difficulty");
        for (Difficulty difficulty : Difficulty.values()) {
            literalArgumentBuilder.then(Commands.literal(difficulty.getKey()).executes(commandContext -> DifficultyCommand.setDifficulty(commandContext.getSource(), difficulty)));
        }
        dispatcher.register((literalArgumentBuilder.requires(commandSourceStack -> commandSourceStack.hasPermission(1))).executes(commandContext -> {
            Difficulty difficulty = commandContext.getSource().getLevel().getDifficulty();
            commandContext.getSource().sendSuccess(() -> Component.translatable("commands.difficulty.query", difficulty.getDisplayName()), false);
            return difficulty.getId();
        }));
        ci.cancel();
    }
}
