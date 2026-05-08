package com.ChatLite.server;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServerLog {

    private static final Path FILE = Path.of("logs/server.log");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static synchronized void write(String type, String msg) {
        try {
            if (FILE.getParent() != null) {
                Files.createDirectories(FILE.getParent());
            }

            String line = "[" + TS.format(LocalDateTime.now()) + "] [" + type + "] " + msg + "\n";

            Files.writeString(FILE, line,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);

        } catch (Exception ignored) {
        }
    }

    public static void info(String msg) {
        write("INFO", msg);
    }

    public static void error(String msg) {
        write("ERROR", msg);
    }

    public static void error(String tag, String msg) {
        write("ERROR", tag + ": " + msg);
    }

    public static void error(String tag, Exception ex) {
        write("ERROR", tag + ": " + ex);
    }

    public static void auth(String user, boolean ok) {
        write("AUTH", user + " => " + (ok ? "OK" : "FAIL"));
    }

    public static void auth(String user, String host, boolean ok) {
        write("AUTH", user + "@" + host + " => " + (ok ? "OK" : "FAIL"));
    }

    public static void listCount(String user, int count) {
        write("LIST", "user=" + user + " count=" + count);
    }

    public static void listCount(String user, int count, boolean unreadOnly) {
        write("LIST", "user=" + user +
                " count=" + count +
                " unreadOnly=" + unreadOnly);
    }
}