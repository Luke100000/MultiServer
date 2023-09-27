package net.conczin.multiserver.utils;

public class Exceptions {
    public static class ServerDoesNotExistException extends Exception {
        public ServerDoesNotExistException() {
            super("Server does not exist");
        }
    }

    public static class ServerAlreadyRunningException extends Exception {
        public ServerAlreadyRunningException() {
            super("Port already in use");
        }
    }

    public static class PortInUseException extends Exception {
        public PortInUseException() {
            super("Port already in use");
        }
    }
}
