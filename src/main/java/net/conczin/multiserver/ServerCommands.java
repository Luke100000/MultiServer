package net.conczin.multiserver;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.conczin.multiserver.server.ServerSettings;
import net.conczin.multiserver.utils.Exceptions;
import net.conczin.multiserver.utils.Utils;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.GameProfileCache;

public class ServerCommands {
    public static void initialize() {
        register(Commands.literal("ms")
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
                                .executes(ServerCommands::stopServer))));

        register(Commands.literal("join")
                .then(Commands.argument("user", StringArgumentType.word())
                        .executes(ServerCommands::joinServer)));
    }

    private static int joinServer(CommandContext<CommandSourceStack> context) {
        GameProfileCache profileCache = context.getSource().getServer().getProfileCache();
        if (profileCache != null) {
            String name = context.getArgument("user", String.class);
            profileCache.getAsync(name, (a) -> {
                a.ifPresent((gameProfile) -> {
                    String root = Utils.uuidToWorldRoot(gameProfile.getId());
                    // todo execute command in the name of the player
                });
            });
        }
        return 0;
    }

    private static void register(LiteralArgumentBuilder<CommandSourceStack> command) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(command));
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
