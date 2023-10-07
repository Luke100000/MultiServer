package net.conczin.multiserver.server;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ServerSettings {
    private @Nullable UUID owner;
    private int threads = 2;
    private boolean hasConsole = false;
    private int mspt = 25;

    private int targetViewDistance = 20;
    private int targetSimulationDistance = 10;
    private int targetChunkTickDistance = 128;

    public ServerSettings() {
    }

    public ServerSettings(CompoundTag tag) {
        this.threads = tag.getInt("threads");
        this.hasConsole = tag.getBoolean("hasConsole");
        this.mspt = tag.getInt("mspt");
        this.targetViewDistance = tag.getInt("maxViewDistance");
        this.targetSimulationDistance = tag.getInt("maxSimulationDistance");
        this.targetChunkTickDistance = tag.getInt("maxChunkTickDistance");
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

    public int getThreads() {
        return threads;
    }

    public @Nullable UUID getOwner() {
        return owner;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
    }

    public int getMspt() {
        return mspt;
    }

    public void setMspt(int mspt) {
        this.mspt = mspt;
    }

    public int getTargetViewDistance() {
        return targetViewDistance;
    }

    public void setTargetViewDistance(int targetViewDistance) {
        this.targetViewDistance = targetViewDistance;
    }

    public int getTargetSimulationDistance() {
        return targetSimulationDistance;
    }

    public void setTargetSimulationDistance(int targetSimulationDistance) {
        this.targetSimulationDistance = targetSimulationDistance;
    }

    public int getTargetChunkTickDistance() {
        return targetChunkTickDistance;
    }

    public void setTargetChunkTickDistance(int targetChunkTickDistance) {
        this.targetChunkTickDistance = targetChunkTickDistance;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("threads", threads);
        tag.putBoolean("hasConsole", hasConsole);
        tag.putInt("mspt", mspt);
        tag.putInt("targetViewDistance", targetViewDistance);
        tag.putInt("targetSimulationDistance", targetSimulationDistance);
        tag.putInt("targetChunkTickDistance", targetChunkTickDistance);
        return tag;
    }
}
