package net.conczin.multiserver.server;

public class ServerSettings {
    private final int threads;

    public static ServerSettings.Builder create() {
        return new ServerSettings.Builder();
    }

    private ServerSettings(Builder builder) {
        this.threads = builder.threads;
    }

    public int getThreads() {
        return threads;
    }

    public static class Builder {
        private int threads = 2;

        public Builder setThreads(int threads) {
            this.threads = threads;
            return this;
        }

        public ServerSettings build() {
            return new ServerSettings(this);
        }
    }
}
