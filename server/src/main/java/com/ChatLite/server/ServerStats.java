package com.ChatLite.server;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerStats {

    public static class UserStats {
        public final String username;
        public int sentCount;
        public int receivedCount;
        public long trafficBytes;

        public UserStats(String username) {
            this.username = username;
        }

        public int getTotalMessages() {
            return sentCount + receivedCount;
        }
    }

    private static final Map<String, UserStats> STATS = new ConcurrentHashMap<>();

    private static UserStats getOrCreate(String username) {
        return STATS.computeIfAbsent(username.toLowerCase(Locale.ROOT), UserStats::new);
    }

    public static synchronized void recordSent(String username, String message) {
        if (username == null || username.isBlank()) return;
        UserStats s = getOrCreate(username);
        s.sentCount++;
        s.trafficBytes += message == null ? 0 : message.getBytes(StandardCharsets.UTF_8).length;
    }

    public static synchronized void recordReceived(String username, String message) {
        if (username == null || username.isBlank()) return;
        UserStats s = getOrCreate(username);
        s.receivedCount++;
        s.trafficBytes += message == null ? 0 : message.getBytes(StandardCharsets.UTF_8).length;
    }

    public static synchronized List<UserStats> snapshot() {
        List<UserStats> out = new ArrayList<>(STATS.values());
        out.sort(Comparator.comparing(a -> a.username));
        return out;
    }

    public static synchronized void clearAll() {
        STATS.clear();
    }
}