package net.conczin.multiserver.server;

import net.minecraft.nbt.CompoundTag;

public class ServerSettings {
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

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("threads", threads);
        return tag;
    }
}
