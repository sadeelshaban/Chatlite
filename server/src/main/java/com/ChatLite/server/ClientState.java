package com.ChatLite.server;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ClientState {

    private final Path baseDir;

    // ====== ID SYSTEM ======
    private static final Path ID_FILE = Path.of("data/last_id.txt");
    private static long NEXT_ID = loadLastId();

    private static long loadLastId() {
        try {
            if (Files.exists(ID_FILE)) {
                String t = Files.readString(ID_FILE).trim();
                return Long.parseLong(t);
            }
        } catch (Exception ignored) {}
        return 1; // first ID
    }

    private static void saveLastId(long id) {
        try {
            Files.createDirectories(ID_FILE.getParent());
            Files.writeString(ID_FILE, Long.toString(id),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ignored) {}
    }
    // ========================


    public ClientState(Path base) {
        this.baseDir = base;
    }

    private Path userDir(String user) { return baseDir.resolve(user); }

    private Path inbox(String user) { return userDir(user).resolve("inbox.json"); }
    private Path sent(String user) { return userDir(user).resolve("sent.json"); }
    private Path archive(String user) { return userDir(user).resolve("archive.json"); }

    private <T> List<T> load(Path file, Class<T> cls) throws IOException {
        if (!Files.exists(file)) return new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(file)) {
            return Json.readList(br, cls);
        }
    }

    private <T> void save(Path file, List<T> list) throws IOException {
        Files.createDirectories(file.getParent());
        try (BufferedWriter bw = Files.newBufferedWriter(file)) {
            Json.writeList(bw, list);
        }
    }
//
//    // ====== MESSAGE CREATION ======
//    public static Mail createMessage(String from, String to, String subj, String body) {
//        long id = NEXT_ID++;   // sequential ID
//        saveLastId(id);        // persist latest ID
//        return new Mail(id, from, to, subj, body, System.currentTimeMillis());
//    }
//    // ===============================
//
//    public void deliver(String user, Mail mail) throws IOException {
//        List<Mail> inbox = load(inbox(user), Mail.class);
//        inbox.add(mail);
//        save(inbox(user), inbox);
//    }
//
//    public void saveSent(String user, Mail mail) throws IOException {
//        List<Mail> s = load(sent(user), Mail.class);
//        s.add(mail);
//        save(sent(user), s);
//    }
//
//    public List<Mail> listInbox(String u) throws IOException { return load(inbox(u), Mail.class); }
//    public List<Mail> listSent(String u) throws IOException { return load(sent(u), Mail.class); }
//    public List<Mail> listArchive(String u) throws IOException { return load(archive(u), Mail.class); }
//
//    public Mail get(String user, long id) throws IOException {
//        for (Mail m : load(inbox(user), Mail.class)) if (m.id == id) return m;
//        for (Mail m : load(sent(user), Mail.class)) if (m.id == id) return m;
//        for (Mail m : load(archive(user), Mail.class)) if (m.id == id) return m;
//        return null;
//    }
//
//    public boolean archiveMsg(String u, long id) throws IOException {
//        List<Mail> inbox = load(inbox(u), Mail.class);
//        Mail target = inbox.stream().filter(m -> m.id == id).findFirst().orElse(null);
//
//        if (target == null) return false;
//
//        inbox.remove(target);
//        save(inbox(u), inbox);
//
//        List<Mail> arch = load(archive(u), Mail.class);
//        arch.add(target);
//        save(archive(u), arch);
//
//        return true;
//    }
//
//    public boolean restore(String u, long id) throws IOException {
//        List<Mail> arch = load(archive(u), Mail.class);
//        Mail target = arch.stream().filter(m -> m.id == id).findFirst().orElse(null);
//
//        if (target == null) return false;
//
//        arch.remove(target);
//        save(archive(u), arch);
//
//        List<Mail> inbox = load(inbox(u), Mail.class);
//        inbox.add(target);
//        save(inbox(u), inbox);
//
//        return true;
//    }
//
//    public void touch(String user) throws IOException {}

}
