package net.conczin.multiserver;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.conczin.multiserver.server.ServerSettings;
import net.conczin.multiserver.utils.Exceptions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ServerCommands {
    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                Commands.literal("ms")
                        .requires(commandSourceStack -> commandSourceStack.hasPermission(2))
                        .executes(context -> {
                            context.getSource().sendSuccess(() -> Component.literal("MultiServer commands:"), false);
                            context.getSource().sendSuccess(() -> Component.literal("/ms start <root> <port>"), false);
                            context.getSource().sendSuccess(() -> Component.literal("/ms stop <root>"), false);
                            return 1;
                        })
                        .then(Commands.literal("start")
                                .then(Commands.argument("root", StringArgumentType.word())
                                        .then(Commands.argument("port", IntegerArgumentType.integer(1024, 65535))
                                                .executes(ServerCommands::startServer))))
                        .then(Commands.literal("stop")
                                .then(Commands.argument("root", StringArgumentType.word())
                                        .executes(ServerCommands::stopServer)))));
    }

    private static int startServer(CommandContext<CommandSourceStack> context) {
        String root = context.getArgument("root", String.class);
        Integer port = context.getArgument("port", Integer.class);

        ServerSettings settings = ServerSettings.create().setThreads(2).build();

        try {
            MultiServerManager.launchServer(root, port, settings);
        } catch (Exceptions.ServerAlreadyRunningException | Exceptions.PortInUseException e) {
            context.getSource().sendFailure(Component.literal(e.getMessage()));
        }

        return 1;
    }

    private static int stopServer(CommandContext<CommandSourceStack> context) {
        String root = context.getArgument("root", String.class);

        try {
            MultiServerManager.shutdownServer(root);
        } catch (Exceptions.ServerDoesNotExistException e) {
            context.getSource().sendFailure(Component.literal(e.getMessage()));
        }

        return 1;
    }
}
