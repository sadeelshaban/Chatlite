package com.ChatLite.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class UdpNotifier {
    private static DatagramSocket socket;
    private static int boundPort = -1;

    public static synchronized void init(int serverUdpPort) {
        try {
            if (socket != null && !socket.isClosed()) return;
            socket = new DatagramSocket(serverUdpPort);
            boundPort = serverUdpPort;
            ServerLog.info("UDP_INIT port=" + serverUdpPort);
        } catch (Exception ex) {
            boundPort = -1;
            ServerLog.error("UDP_INIT", ex.toString());
        }
    }

    public static synchronized int getBoundPort() {
        return boundPort;
    }

    public static void notifyAddress(String ip, int port, String msg) {
        if (ip == null || ip.isBlank() || port <= 0 || msg == null || msg.isBlank()) {
            return;
        }

        DatagramSocket s;
        synchronized (UdpNotifier.class) {
            s = socket;
        }

        if (s == null || s.isClosed()) {
            return;
        }

        try {
            byte[] data = msg.getBytes(StandardCharsets.US_ASCII);
            DatagramPacket dp = new DatagramPacket(
                    data,
                    data.length,
                    InetAddress.getByName(ip),
                    port
            );
            s.send(dp);
        } catch (Exception ignored) {
            // UDP is optional; TCP chat must continue normally.
        }
    }
}