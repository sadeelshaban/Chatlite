package com.ChatLite.server;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class UserStore {

    private static final Path FILE = Path.of("data/users.txt");
    private static final Map<String, String> users = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    static {
        load();
    }

    private static void load() {
        try {
            if (!Files.exists(FILE)) return;

            for (String line : Files.readAllLines(FILE)) {
                String s = line == null ? "" : line.trim();
                if (s.isEmpty()) continue;

                if (s.contains(":")) {
                    String[] parts = s.split(":", 2);
                    String username = normalize(parts[0]);
                    String password = parts.length > 1 ? parts[1].trim() : "";
                    if (!username.isEmpty()) {
                        users.put(username, password);
                    }
                } else {
                    // old file compatibility
                    String username = normalize(s);
                    if (!username.isEmpty()) {
                        users.put(username, "");
                    }
                }
            }
        } catch (Exception ex) {
            ServerLog.error("UserStore.load", ex.toString());
        }
    }

    private static void save() {
        try {
            if (FILE.getParent() != null) {
                Files.createDirectories(FILE.getParent());
            }

            try (BufferedWriter bw = Files.newBufferedWriter(
                    FILE,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            )) {
                for (Map.Entry<String, String> e : users.entrySet()) {
                    bw.write(e.getKey() + ":" + e.getValue());
                    bw.newLine();
                }
            }
        } catch (Exception ex) {
            ServerLog.error("UserStore.save", ex.toString());
        }
    }

    public static synchronized boolean addUser(String username, String password) {
        if (username == null || username.isBlank()) {
            ServerLog.info("REGISTER_FAILED invalid_username");
            return false;
        }

        String key = username.trim().toLowerCase(Locale.ROOT);
        String value = password == null ? "" : password.trim();

        if (users.containsKey(key)) {
            ServerLog.info("REGISTER_FAILED duplicate username=" + key);
            return false;
        }

        users.put(key, value);
        save();

        ServerLog.info("REGISTER_SUCCESS username=" + key);
        return true;
    }

    public static synchronized boolean removeUser(String username) {
        String u = normalize(username);
        if (!users.containsKey(u)) return false;
        users.remove(u);
        save();
        return true;
    }

    public static synchronized boolean exists(String username) {
        return users.containsKey(normalize(username));
    }

    public static synchronized boolean checkUser(String username, String pass) {
        String u = normalize(username);
        if (!users.containsKey(u)) return false;

        String stored = users.get(u);
        String given = pass == null ? "" : pass.trim();
        return Objects.equals(stored, given);
    }

    public static synchronized boolean updatePassword(String username, String newPass) {
        String u = normalize(username);
        if (!users.containsKey(u)) return false;

        users.put(u, newPass == null ? "" : newPass.trim());
        save();
        return true;
    }

    public static synchronized String getPassword(String username) {
        return users.get(normalize(username));
    }

    public static synchronized List<String> snapshot() {
        return new ArrayList<>(users.keySet());
    }

    private static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

}