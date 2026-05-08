package com.ChatLite.client;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class UdpListener implements Runnable {
    private final int port;
    private final Consumer<String> onMessage;
    private volatile boolean running = true;
    private DatagramSocket socket;

    public UdpListener(int port, Consumer<String> onMessage) {
        this.port = port; this.onMessage = onMessage;
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(port);
            byte[] buf = new byte[2048];
            while (running) {
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                socket.receive(p);
                String msg = new String(p.getData(), 0, p.getLength(), StandardCharsets.US_ASCII).trim();
                if (onMessage != null) onMessage.accept(msg);
            }
        } catch (Exception ex) {
            if (running) {
                ex.printStackTrace();
            }
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
