package net.conczin.multiserver.data;

import net.conczin.multiserver.Config;
import net.conczin.multiserver.MultiServer;
import net.conczin.multiserver.server.CoMinecraftServer;
import net.conczin.multiserver.server.ServerSettings;
import net.conczin.multiserver.utils.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.level.GameType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PlayerData extends TidySavedData {
    private final UUID uuid;
    private final Map<UUID, PermissionGroup> permissions = new HashMap<>();
    private PermissionGroup defaultPermissionGroup;
    private final ServerSettings settings;

    public PlayerData(UUID uuid) {
        super();

        this.uuid = uuid;
        this.defaultPermissionGroup = PermissionGroup.NONE;
        this.settings = new ServerSettings();
        this.settings.setOwner(uuid);

        initPermissions();
        setDirty();
    }

    public PlayerData(CompoundTag tag, UUID uuid) {
        super();

        this.uuid = uuid;

        CompoundTag permissionTag = tag.getCompound("permissions");
        permissionTag.getAllKeys().forEach(id -> {
            permissions.put(UUID.fromString(id), PermissionGroup.valueOf(permissionTag.getString(id)));
        });

        this.defaultPermissionGroup = PermissionGroup.valueOf(tag.getString("defaultPermissionGroup"));

        this.settings = new ServerSettings(tag.getCompound("settings"));
        this.settings.setOwner(uuid);

        initPermissions();
    }

    public void updatePermissionsForPlayer(CoMinecraftServer server, UUID uuid) {
        PermissionGroup permissionGroup = getPermissions(uuid);
        GameProfileCache profileCache = server.getProfileCache();

        if (profileCache != null) {
            // Owner and Moderators will be op
            if (Config.getInstance().opOwners) {
                profileCache.get(uuid).ifPresent(profile -> {
                    if (permissionGroup == PermissionGroup.OWNER || permissionGroup == PermissionGroup.MODERATOR) {
                        server.getPlayerList().op(profile);
                    } else {
                        server.getPlayerList().deop(profile);
                    }
                });
            }

            // Guests will be in adventure mode
            if (permissionGroup == PermissionGroup.GUEST) {
                getPlayer(server, uuid).ifPresent(player -> {
                    player.setGameMode(GameType.ADVENTURE);
                });
            }

            // Non-member will get kicked, they should not be here in the first place
            if (permissionGroup == PermissionGroup.NONE) {
                getPlayer(server, uuid).ifPresent(player -> {
                    player.connection.disconnect(Component.literal("You are not a member of this server!"));
                });
            }
        }
    }

    private static Optional<ServerPlayer> getPlayer(CoMinecraftServer server, UUID uuid) {
        for (ServerLevel l : server.getAllLevels()) {
            if (l.getPlayerByUUID(uuid) instanceof ServerPlayer player) {
                return Optional.of(player);
            }
        }
        return Optional.empty();
    }

    public void initPermissions() {
        // The instance creator is always the owner
        permissions.put(uuid, PermissionGroup.OWNER);

        CoMinecraftServer server = MultiServer.SERVER_MANAGER.SERVERS.get(getRoot());
        if (server != null) {
            initPermissions(server);
        }
    }

    private void initPermissions(CoMinecraftServer server) {
        // Manage permissions
        permissions.keySet().forEach(uuid -> updatePermissionsForPlayer(server, uuid));
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag permissionTag = new CompoundTag();
        permissions.forEach((uuid, permissionGroup) -> permissionTag.putString(uuid.toString(), permissionGroup.name()));
        tag.put("permissions", permissionTag);

        tag.putString("defaultPermissionGroup", defaultPermissionGroup.name());
        tag.put("settings", settings.save());
        return tag;
    }

    public ServerSettings getSettings() {
        return settings;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getRoot() {
        return Utils.uuidToWorldRoot(uuid);
    }

    public PermissionGroup getPermissions(UUID playerUUID) {
        return permissions.getOrDefault(playerUUID, defaultPermissionGroup);
    }

    public PermissionGroup getDefaultPermissionGroup() {
        return defaultPermissionGroup;
    }

    public void setDefaultPermissionGroup(PermissionGroup defaultPermissionGroup) {
        this.defaultPermissionGroup = defaultPermissionGroup;
        initPermissions();
    }

    public void setPermission(UUID uuid, PermissionGroup permission) {
        permissions.put(uuid, permission);
        initPermissions();
        setDirty();
    }

    public void removePermission(UUID uuid) {
        permissions.remove(uuid);
        initPermissions();
        setDirty();
    }

    public Map<UUID, PermissionGroup> getPermissions() {
        return permissions;
    }
}
