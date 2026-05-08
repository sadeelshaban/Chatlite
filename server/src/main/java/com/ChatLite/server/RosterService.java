package com.ChatLite.server;

import java.util.*;

public class RosterService {

    public static class Entry {
        public String username;
        public String status;
        public String ip;
        public long lastSeen;
        public int udpPort;

        public Entry(String u, String st, String i, long t, int udp) {
            username = u;
            status = st;
            ip = i;
            lastSeen = t;
            udpPort = udp;
        }
    }

    private static final Map<String, Entry> table = new HashMap<>();

    public static synchronized void setOnline(String user, String ip) {
        Entry e = table.getOrDefault(user, new Entry(user, "online", ip, System.currentTimeMillis(), -1));
        e.status = "online";
        e.ip = ip;
        e.lastSeen = System.currentTimeMillis();
        table.put(user, e);
    }

    public static synchronized void setOffline(String user) {
        Entry e = table.get(user);
        if (e != null) {
            e.status = "offline";
            e.lastSeen = System.currentTimeMillis();
        }
    }

    public static synchronized void setStatus(String user, String status) {
        Entry e = table.get(user);
        if (e != null) {
            e.status = status;
            e.lastSeen = System.currentTimeMillis();
        }
    }

    public static synchronized void setUdpPort(String user, int port) {
        Entry e = table.get(user);
        if (e != null) e.udpPort = port;
    }

    public static synchronized Integer getUdpPort(String user) {
        Entry e = table.get(user);
        return e == null ? null : e.udpPort;
    }

    public static synchronized Entry get(String user) {
        return table.get(user);
    }

    public static synchronized List<Entry> snapshot() {
        return new ArrayList<>(table.values());
    }
    public static synchronized boolean isOnline(String user) {
        Entry e = table.get(user);
        return e != null && !"offline".equalsIgnoreCase(e.status);
    }

    public static synchronized void remove(String user) {
        table.remove(user);
    }
}
