package com.ChatLite.server;


import java.net.ServerSocket;
import java.net.Socket;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainServer {

    public static int TCP_PORT;
    public static int UDP_PORT;
    public static ClientState store;

    public static void main(String[] args) throws Exception {
        ServerConfig.load();

        TCP_PORT = ServerConfig.getTcpPort();
        UDP_PORT = ServerConfig.getUdpPort();

        store = new ClientState(Path.of("data"));
        UdpNotifier.init(UDP_PORT);

        ServerSocket serverSocket = new ServerSocket(TCP_PORT);
        ExecutorService pool = Executors.newCachedThreadPool();

        System.out.println("ChatLite Server started on port " + TCP_PORT + "...");
        System.out.println("UDP notifier bound on port " + UDP_PORT + "...");
        System.out.println("Waiting for clients...");

        ServerLog.info("SERVER_START tcpPort=" + TCP_PORT + " udpPort=" + UDP_PORT
                + " maxMessageSize=" + ServerConfig.getMaxMessageSize()
                + " cleanupDays=" + ServerConfig.getCleanupDays());

        javax.swing.SwingUtilities.invokeLater(() -> {
            new AdminConsoleGUI().setVisible(true);
        });

        while (true) {
            Socket socket = serverSocket.accept();
            pool.submit(new TcpProtocolHandler(socket));
        }
    }
}