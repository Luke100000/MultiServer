package net.conczin.multiserver.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

@Plugin(id = "multiserver", name = "MultiServer", version = "1.0.0", url = "https://example.org", description = "MultiServer commands.", authors = {"Luke100000"})
public class VelocityPlugin {
    private static volatile boolean running = true;
    private final ProxyServer proxy;
    private final Map<String, ServerInfo> servers = new HashMap<>();

    @Inject
    public VelocityPlugin(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        new Thread(this::listen).start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> running = false));
    }

    private void listen() {
        try (ServerSocket serverSocket = new ServerSocket(Communication.PORT)) {
            System.out.println("Server waiting for connection...");
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected.");

            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            while (running) {
                String receivedMessage = in.readLine();
                if (receivedMessage != null) {
                    String[] args = receivedMessage.split(" ");

                    // Register server
                    if (args.length == 4 && args[0].equals("server") && args[1].equals("register")) {
                        ServerInfo info = new ServerInfo(args[2], new InetSocketAddress(Integer.parseInt(args[3])));
                        servers.put(args[2], info);
                        proxy.registerServer(info);
                    }

                    // Unregister server
                    if (args.length == 3 && args[0].equals("server") && args[1].equals("unregister") && servers.containsKey(args[2])) {
                        proxy.unregisterServer(servers.get(args[2]));
                    }

                    // Teleport to server
                    if (args.length == 3 && args[0].equals("teleport")) {
                        proxy.getServer(args[2]).ifPresent(server -> {
                            proxy.getPlayer(args[1]).ifPresent(p -> p.createConnectionRequest(server).fireAndForget());
                        });
                    }
                } else {
                    //noinspection BusyWait
                    Thread.sleep(100);
                }
            }

            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}