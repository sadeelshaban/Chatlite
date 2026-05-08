package com.ChatLite.server;

import java.util.*;

public class ChatService {
    private static final Set<String> ROOMS = new LinkedHashSet<>();
    private static final Map<String, Set<String>> MEMBERS = new HashMap<>();

    static {
        ROOMS.add("general");
        ROOMS.add("networks");
        ROOMS.add("java");
    }

    public static synchronized List<String> listRooms() {
        return new ArrayList<>(ROOMS);
    }

    public static synchronized boolean join(String user, String room) {
        if (user == null || user.isBlank() || room == null || room.isBlank()) return false;

        String normalizedRoom = room.trim().toLowerCase(Locale.ROOT);
        String normalizedUser = user.trim().toLowerCase(Locale.ROOT);

        if (!ROOMS.contains(normalizedRoom)) return false;

        MEMBERS.computeIfAbsent(normalizedRoom, r -> new LinkedHashSet<>()).add(normalizedUser);
        return true;
    }

    public static synchronized boolean leave(String user, String room) {
        Set<String> set = MEMBERS.get(room);
        if (set == null) return false;
        return set.remove(user);
    }

    public static synchronized Set<String> membersOf(String room) {
        return new LinkedHashSet<>(MEMBERS.getOrDefault(room, Set.of()));
    }

    public static synchronized void leaveAll(String user) {
        for (Set<String> set : MEMBERS.values()) set.remove(user);
    }

    public static synchronized boolean createRoom(String room) {
        if (room == null) return false;

        String normalized = room.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) return false;
        if (ROOMS.contains(normalized)) return false;

        ROOMS.add(normalized);
        MEMBERS.putIfAbsent(normalized, new LinkedHashSet<>());
        return true;
    }
    public static synchronized boolean deleteRoom(String room) {
        if (room == null) return false;

        String normalized = room.trim().toLowerCase(Locale.ROOT);
        if (!ROOMS.contains(normalized)) return false;

        ROOMS.remove(normalized);
        MEMBERS.remove(normalized);
        return true;
    }
}
