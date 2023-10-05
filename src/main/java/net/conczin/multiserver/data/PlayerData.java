package net.conczin.multiserver.data;

import net.conczin.multiserver.MultiServer;
import net.conczin.multiserver.server.ServerSettings;
import net.conczin.multiserver.utils.Utils;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.node.Node;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData extends TidySavedData {
    private final UUID uuid;
    private final Map<UUID, PermissionGroup> permissions = new HashMap<>();
    private final PermissionGroup defaultPermissionGroup;
    private final ServerSettings settings;

    public PlayerData(ServerLevel level, UUID uuid) {
        super();
        this.uuid = uuid;
        this.defaultPermissionGroup = PermissionGroup.NONE;
        this.settings = new ServerSettings();

        initPermissions();
    }

    public PlayerData(CompoundTag tag, ServerLevel level, UUID uuid) {
        super();
        this.uuid = uuid;

        tag.getCompound("permissions").getAllKeys().forEach(id -> {
            permissions.put(UUID.fromString(id), PermissionGroup.valueOf(tag.getString(id)));
        });

        this.defaultPermissionGroup = PermissionGroup.valueOf(tag.getString("defaultPermissionGroup"));
        this.settings = new ServerSettings(tag.getCompound("settings"));

        initPermissions();
    }

    public void addPermission(UUID userUuid, String permission, String world) {
        LuckPermsProvider.get().getUserManager().modifyUser(userUuid, user -> {
            user.data().add(Node.builder(permission).withContext(DefaultContextKeys.WORLD_KEY, world).build());
        });
    }

    private void initPermissions() {
        permissions.put(uuid, PermissionGroup.OWNER);

        permissions.forEach((uuid, permissionGroup) -> {
            addPermission(uuid, "minecraft.command.gamemode", getRoot());
            MultiServer.LOGGER.error(LuckPermsProvider.get().getUserManager().getUser(uuid).getNodes().size() + " nodes");
        });
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

    public Map<UUID, PermissionGroup> getPermissions() {
        return permissions;
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
}
