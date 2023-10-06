package net.conczin.multiserver.server;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ServerSettings {
    private @Nullable UUID owner;
    private int threads = 2;
    private boolean hasConsole = false;

    public ServerSettings() {
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public boolean hasConsole() {
        return hasConsole;
    }

    public void setHasConsole(boolean hasConsole) {
        this.hasConsole = hasConsole;
    }

    public ServerSettings(CompoundTag tag) {
        this.threads = tag.getInt("threads");
    }

    public int getThreads() {
        return threads;
    }

    public @Nullable UUID getOwner() {
        return owner;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("threads", threads);
        return tag;
    }
}
