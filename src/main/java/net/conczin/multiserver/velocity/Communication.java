package net.conczin.multiserver.velocity;

import net.conczin.multiserver.MultiServer;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;

public class Communication {
    public static final int PORT = 17777;

    Socket socket;
    PrintWriter out;

    public Communication() throws IOException {
        try {
            socket = new Socket("localhost", PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (ConnectException e) {
            MultiServer.LOGGER.error("Failed to connect to Velocity", e);
        }
    }

    public void send(String message) {
        if (out != null) {
            out.println(message);
        } else {
            MultiServer.LOGGER.error("Failed to send a message to Velocity: " + message);
        }
    }
}
