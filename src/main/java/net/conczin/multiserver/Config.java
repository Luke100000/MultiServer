package net.conczin.multiserver;

public final class Config extends JsonConfig {
    private static final Config INSTANCE = loadOrCreate();

    public static Config getInstance() {
        return INSTANCE;
    }

    public int firstPort = 25001;
    public int lastPort = 25032;
}
