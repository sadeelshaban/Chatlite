package com.ChatLite.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ServerConfig {

    private static final Path CONFIG_FILE = Path.of("data", "server-config.properties");

    private static final int DEFAULT_TCP_PORT = 2525;
    private static final int DEFAULT_UDP_PORT = 5001;
    private static final int DEFAULT_MAX_MESSAGE_SIZE = 1024;
    private static final int DEFAULT_CLEANUP_DAYS = 30;

    private static int tcpPort = DEFAULT_TCP_PORT;
    private static int udpPort = DEFAULT_UDP_PORT;
    private static int maxMessageSize = DEFAULT_MAX_MESSAGE_SIZE;
    private static int cleanupDays = DEFAULT_CLEANUP_DAYS;

    private ServerConfig() {
    }

    public static synchronized void load() {
        Properties props = new Properties();

        try {
            Files.createDirectories(CONFIG_FILE.getParent());

            if (Files.exists(CONFIG_FILE)) {
                try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
                    props.load(in);
                }
            }

            tcpPort = parseInt(props.getProperty("tcp.port"), DEFAULT_TCP_PORT, 1, 65535);
            udpPort = parseInt(props.getProperty("udp.port"), DEFAULT_UDP_PORT, 1, 65535);
            maxMessageSize = parseInt(props.getProperty("max.message.size"), DEFAULT_MAX_MESSAGE_SIZE, 1, 65535);
            cleanupDays = parseInt(props.getProperty("cleanup.days"), DEFAULT_CLEANUP_DAYS, 0, 3650);

            save();
        } catch (Exception ex) {
            tcpPort = DEFAULT_TCP_PORT;
            udpPort = DEFAULT_UDP_PORT;
            maxMessageSize = DEFAULT_MAX_MESSAGE_SIZE;
            cleanupDays = DEFAULT_CLEANUP_DAYS;
            ServerLog.error("CONFIG_LOAD", ex.toString());
        }
    }

    public static synchronized void save() {
        Properties props = new Properties();
        props.setProperty("tcp.port", String.valueOf(tcpPort));
        props.setProperty("udp.port", String.valueOf(udpPort));
        props.setProperty("max.message.size", String.valueOf(maxMessageSize));
        props.setProperty("cleanup.days", String.valueOf(cleanupDays));

        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
                props.store(out, "ChatLite server configuration");
            }
        } catch (IOException ex) {
            ServerLog.error("CONFIG_SAVE", ex.toString());
        }
    }

    private static int parseInt(String raw, int fallback, int min, int max) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }

        try {
            int value = Integer.parseInt(raw.trim());
            if (value < min || value > max) {
                return fallback;
            }
            return value;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public static synchronized int getTcpPort() {
        return tcpPort;
    }

    public static synchronized int getUdpPort() {
        return udpPort;
    }

    public static synchronized int getMaxMessageSize() {
        return maxMessageSize;
    }

    public static synchronized int getCleanupDays() {
        return cleanupDays;
    }

    public static synchronized void update(int newUdpPort, int newMaxMessageSize, int newCleanupDays) {
        if (newUdpPort < 1 || newUdpPort > 65535) {
            throw new IllegalArgumentException("UDP port must be between 1 and 65535.");
        }
        if (newMaxMessageSize < 1 || newMaxMessageSize > 65535) {
            throw new IllegalArgumentException("Max message size must be between 1 and 65535.");
        }
        if (newCleanupDays < 0 || newCleanupDays > 3650) {
            throw new IllegalArgumentException("Cleanup days must be between 0 and 3650.");
        }

        udpPort = newUdpPort;
        maxMessageSize = newMaxMessageSize;
        cleanupDays = newCleanupDays;
        save();
    }
}