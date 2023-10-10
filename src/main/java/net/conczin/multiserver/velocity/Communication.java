package net.conczin.multiserver.velocity;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class Communication {
    public static final int PORT = 17777;

    Socket socket;
    PrintWriter out;

    public Communication() throws IOException {
        socket = new Socket("localhost", PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    public void send(String message) {
        out.println(message);
    }
}
