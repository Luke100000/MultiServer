package net.conczin.multiserver;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.conczin.multiserver.data.PermissionGroup;
import net.conczin.multiserver.data.PlayerData;
import net.conczin.multiserver.data.PlayerDataManager;
import net.conczin.multiserver.server.ServerSettings;
import net.conczin.multiserver.utils.Exceptions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class ServerCommands {
    private static final String HELP = """
            §lMultiServer commands:§r
            /ms join [user] §o– Join your own or another user's server.§r
            /ms leave §o– Return to the lobby.§r
            /ms invite <user> §o– Add someone as member.§r
            /ms invite <user> [permissions] §o– Grant specific permission.§r
            /ms kick <user> §o– Kick someone from your server.§r
            /ms guests <true/false> §o– Allow guests to visit your server.§r
            /ms members §o– See who currently has access to your server.§r""";

    public static void initialize() {
        register(Commands.literal("ms")
                .executes(ServerCommands::help)
                .then(Commands.literal("help")
                        .executes(ServerCommands::help))

                .then(Commands.literal("start")
                        .requires(commandSourceStack -> commandSourceStack.hasPermission(2))
                        .then(Commands.argument("root", StringArgumentType.word())
                                .then(Commands.argument("port", IntegerArgumentType.integer(1024, 65535))
                                        .executes(ServerCommands::startServer))))

                .then(Commands.literal("stop")
                        .requires(commandSourceStack -> commandSourceStack.hasPermission(2))
                        .then(Commands.argument("root", StringArgumentType.word())
                                .executes(ServerCommands::stopServer)))

                .then(Commands.literal("join")
                        .executes(ServerCommands::joinOwn)
                        .then(Commands.argument("user", StringArgumentType.word())
                                .executes(ServerCommands::joinServer)))

                .then(Commands.literal("leave")
                        .executes(ServerCommands::leaveServer))

                .then(Commands.literal("kick")
                        .then(Commands.argument("user", StringArgumentType.word())
                                .executes(ServerCommands::kickMember)))

                .then(Commands.literal("invite")
                        .then(Commands.argument("user", StringArgumentType.word())
                                .executes(ServerCommands::inviteUserAsMember)
                                .then(Commands.argument("permission", StringArgumentType.word())
                                        .executes(ServerCommands::inviteUser))))

                .then(Commands.literal("guests")
                        .then(Commands.argument("allow", BoolArgumentType.bool())
                                .executes(ServerCommands::allowGuests)))

                .then(Commands.literal("members")
                        .executes(ServerCommands::printMembers))

                .then(Commands.literal("performance")
                        .executes(ServerCommands::printPerformance)));
    }

    private static int printPerformance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerSettings settings = PlayerDataManager.getPlayerData(context.getSource().getPlayerOrException().getUUID()).getSettings();
        context.getSource().sendSuccess(() -> Component.literal(String.format(
                "Your server is currently running with %d threads, a target mspt of %d, %d view distance, %d simulation distance and %d chunk tick distance.",
                settings.getThreads(),
                settings.getMspt(),
                settings.getTargetViewDistance(),
                settings.getTargetSimulationDistance(),
                settings.getTargetChunkTickDistance()
        )), false);
        return 0;
    }

    private static int help(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal(HELP), false);
        return 0;
    }

    private static PlayerData getPlayerData(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        UUID playerUUID = player.getUUID();
        return PlayerDataManager.getPlayerData(playerUUID);
    }

    private static int printMembers(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        GameProfileCache profileCache = context.getSource().getServer().getProfileCache();
        if (profileCache != null) {
            PlayerData playerData = getPlayerData(context);
            for (PermissionGroup value : PermissionGroup.values()) {
                List<String> strings = playerData.getPermissions().entrySet().stream().filter(entry -> entry.getValue() == value).map(entry -> {
                    return profileCache.get(entry.getKey()).map(GameProfile::getName).orElse(entry.getKey().toString());
                }).toList();
                if (!strings.isEmpty()) {
                    context.getSource().sendSuccess(() -> Component.literal(value.name() + ": " + String.join(", ", strings)), false);
                }
            }
        }
        return 0;
    }

    private static int allowGuests(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        boolean allow = BoolArgumentType.getBool(context, "allow");
        getPlayerData(context).setDefaultPermissionGroup(allow ? PermissionGroup.GUEST : PermissionGroup.NONE);
        context.getSource().sendSuccess(() -> Component.literal("Guests " + (allow ? "allowed" : "disallowed") + "."), false);
        return 0;
    }

    private static int leaveServer(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> buildTeleportationComponent("lobby", "Click to return to lobby!"), false);
        return 0;
    }

    private static void register(LiteralArgumentBuilder<CommandSourceStack> command) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(command));
    }

    private static int inviteUser(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        // Parse permission
        String perm = StringArgumentType.getString(context, "permission");
        PermissionGroup permission = switch (perm.toLowerCase(Locale.ROOT)) {
            case "moderator" -> PermissionGroup.MODERATOR;
            case "member" -> PermissionGroup.MEMBER;
            case "guest" -> PermissionGroup.GUEST;
            default -> null;
        };
        if (permission == null) {
            context.getSource().sendFailure(Component.literal("Invalid permission group, should be `moderator`, `member` or `guest`."));
            return 1;
        }

        return inviteUserAs(context, permission);
    }

    private static int kickMember(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return inviteUserAs(context, null);
    }

    private static int inviteUserAsMember(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return inviteUserAs(context, PermissionGroup.MEMBER);
    }

    private static int inviteUserAs(CommandContext<CommandSourceStack> context, @Nullable PermissionGroup permission) throws CommandSyntaxException {
        // Update permission
        PlayerData playerData = getPlayerData(context);
        getUUID(context.getSource().getLevel(), StringArgumentType.getString(context, "user")).ifPresentOrElse(uuid -> {
            if (permission == null) {
                playerData.removePermission(uuid);
                context.getSource().sendSuccess(() -> Component.literal("Player kicked."), false);
            } else {
                playerData.setPermission(uuid, permission);
                context.getSource().sendSuccess(() -> Component.literal("Permission set."), false);
            }
        }, () -> {
            context.getSource().sendFailure(Component.literal("Player not found."));
        });
        return 0;
    }

    private static Optional<UUID> getUUID(ServerLevel level, String username) {
        GameProfileCache profileCache = level.getServer().getProfileCache();
        if (profileCache != null) {
            return profileCache.get(username).map(GameProfile::getId);
        }
        return Optional.empty();
    }

    private static int joinOwn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        UUID playerUUID = player.getUUID();

        PlayerData hostPlayerData = PlayerDataManager.getPlayerData(playerUUID);

        joinPlayer(context, hostPlayerData);

        return 0;
    }

    private static int joinServer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        UUID playerUUID = player.getUUID();
        getUUID(context.getSource().getLevel(), StringArgumentType.getString(context, "user")).ifPresentOrElse(uuid -> {
            PlayerData hostPlayerData = PlayerDataManager.getPlayerData(uuid);

            // Check access permission
            PermissionGroup permissions = hostPlayerData.getPermissions(playerUUID);
            if (permissions == PermissionGroup.NONE) {
                context.getSource().sendFailure(Component.literal("You do not have permission to join this server."));
                return;
            }

            joinPlayer(context, hostPlayerData);
        }, () -> {
            context.getSource().sendFailure(Component.literal("Player not found."));
        });
        return 0;
    }

    private static void joinPlayer(CommandContext<CommandSourceStack> context, PlayerData hostPlayerData) {
        if (!MultiServer.SERVER_MANAGER.SERVERS.containsKey(hostPlayerData.getRoot())) {
            context.getSource().sendSuccess(() -> Component.literal("Launching server..."), false);
        }

        if (MultiServer.SERVER_MANAGER.FREE_PORTS.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Server is full right now, please wait until a server slot becomes free."));
            return;
        }

        MultiServer.SERVER_MANAGER.launchServer(hostPlayerData, server -> {
            context.getSource().sendSuccess(() -> buildTeleportationComponent("port_" + server.getPort(), "Click to join!"), false);
        });
    }

    private static MutableComponent buildTeleportationComponent(String server, String text) {
        return Component.literal(text)
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/server " + server)));
    }

    private static int startServer(CommandContext<CommandSourceStack> context) {
        String root = context.getArgument("root", String.class);
        Integer port = context.getArgument("port", Integer.class);

        if (MultiServer.SERVER_MANAGER.SERVERS.containsKey(root)) {
            context.getSource().sendFailure(Component.literal("Server already running."));
            return 1;
        } else {
            context.getSource().sendSuccess(() -> Component.literal("Starting server."), false);

            try {
                MultiServer.SERVER_MANAGER.launchServer(root, port, new ServerSettings(), server -> {
                    context.getSource().sendSuccess(() -> Component.literal("Server started."), false);
                });
            } catch (Exceptions.ServerAlreadyRunningException | Exceptions.PortInUseException e) {
                context.getSource().sendFailure(Component.literal(e.getMessage()));
            }
        }

        return 1;
    }

    private static int stopServer(CommandContext<CommandSourceStack> context) {
        String root = context.getArgument("root", String.class);

        try {
            MultiServer.SERVER_MANAGER.shutdownServer(root);
        } catch (Exceptions.ServerDoesNotExistException e) {
            context.getSource().sendFailure(Component.literal(e.getMessage()));
        }

        return 1;
    }
}
