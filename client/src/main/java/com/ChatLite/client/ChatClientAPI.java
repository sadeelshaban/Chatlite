package com.ChatLite.client;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ChatClientAPI implements Closeable {
    private final Socket sock;
    private final BufferedReader in;
    private final BufferedWriter out;
    private final BlockingQueue<String> responses = new LinkedBlockingQueue<>();
    private final BlockingQueue<String> events = new LinkedBlockingQueue<>();
    private volatile boolean running = true;
    private final Thread readerThread;

    public ChatClientAPI(String host, int port) throws IOException {
        this.sock = new Socket();
        this.sock.connect(new InetSocketAddress(host, port), 5000);
        this.sock.setSoTimeout(0);
        this.in = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.US_ASCII));
        this.out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.US_ASCII));
        this.readerThread = new Thread(this::readerLoop, "chatlite-api-reader");
        this.readerThread.setDaemon(true);
        this.readerThread.start();
    }

    private void sendLine(String s) throws IOException {
        out.write(s);
        if (!s.endsWith("\n")) out.write("\n");
        out.flush();
    }

    private void readerLoop() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                if (line.startsWith("ROOMMSG ")
                        || line.startsWith("PMFROM ")
                        || line.startsWith("BROADCAST ")) {
                    events.offer(line);
                } else {
                    responses.offer(line);
                }
            }
        } catch (IOException ignored) {
        } finally {
            running = false;
            events.offer("__CLOSED__");
            responses.offer("__CLOSED__");
        }
    }

    private String requestSingleLine(String cmd, String expectedPrefix) throws IOException {
        synchronized (out) {
            sendLine(cmd);
            String r = takeResponse();
            if (r == null || "__CLOSED__".equals(r) || !r.startsWith(expectedPrefix)) {
                throw new IOException(cmd.split(" ")[0] + " failed: " + r);
            }
            return r;
        }
    }

    private String takeResponse() throws IOException {
        try {
            return responses.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for response");
        }
    }

    public void hello(String username) throws IOException {
        requestSingleLine("HELLO " + username, "200");
    }

    public boolean userExists(String username) throws IOException {
        synchronized (out) {
            sendLine("CHECKUSER " + username);
            String r = takeResponse();
            return r != null && r.startsWith("250");
        }
    }

    public void setUdpPort(int port) throws IOException {
        requestSingleLine("UDPPORT " + port, "250");
    }

    public boolean auth(String user, String pass) throws IOException {
        synchronized (out) {
            sendLine("AUTH " + user + " " + pass);
            String r = takeResponse();
        return r != null && (r.startsWith("235") || r.startsWith("200"));
        }
    }

    /** @return server millis recorded at join (used to ignore room traffic from before membership). */
    public long join(String room) throws IOException {
        synchronized (out) {
            sendLine("JOIN " + room);
            String r = takeResponse();
            if (r == null || "__CLOSED__".equals(r) || !r.startsWith("210")) {
                throw new IOException("JOIN failed: " + r);
            }
            String[] parts = r.trim().split("\\s+");
            if (parts.length >= 4) {
                try {
                    return Long.parseLong(parts[3]);
                } catch (NumberFormatException ignored) {
                }
            }
            return System.currentTimeMillis();
        }
    }

    public void leave(String room) throws IOException {
        requestSingleLine("LEAVE " + room, "215");
    }

    public void msg(String room, String message) throws IOException {
        requestSingleLine("MSG " + room + " " + message, "211");
    }

    public void pm(String username, String message) throws IOException {
        requestSingleLine("PM " + username + " " + message, "212");
    }

    public void setStatus(String status) throws IOException {
        requestSingleLine("STATUS " + status, "250");
    }

    public List<String> users() throws IOException {
        synchronized (out) {
            sendLine("USERS");
            List<String> rows = new ArrayList<>();
            String r;
            while (true) {
                r = takeResponse();
                if (r == null || "__CLOSED__".equals(r)) throw new IOException("Connection closed");
                if (r.equals("213 END")) break;
                if (r.startsWith("213U ")) rows.add(r.substring(5).trim());
            }
            return rows;
        }
    }

    public List<String> rooms() throws IOException {
        synchronized (out) {
            sendLine("ROOMS");
            List<String> rows = new ArrayList<>();
            String r;
            while (true) {
                r = takeResponse();
                if (r == null || "__CLOSED__".equals(r)) throw new IOException("Connection closed");
                if (r.equals("214 END")) break;
                if (r.startsWith("214 ")) rows.add(r.substring(4).trim());
            }
            return rows;
        }
    }

    public String readEvent() throws IOException {
        try {
            String e = events.take();
            if ("__CLOSED__".equals(e)) return null;
            return e;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for event");
        }
    }

    public void quit() throws IOException {
        synchronized (out) {
            sendLine("QUIT");
            takeResponse();
        }
        close();
    }

    // Compatibility helpers for existing classes.
    public void helo(String username, int udpPort) throws IOException { hello(username); }
    public List<String> who() throws IOException { return users(); }
    public List<String> list(boolean unreadOnly) { return List.of(); }
    public List<String> listSent() { return List.of(); }
    public List<String> listArchive() { return List.of(); }
    public Retrieved retr(long id) { return null; }
    public boolean dele(long id) { return false; }
    public boolean restore(long id) { return false; }
    public Stat stat() { return new Stat(); }
    public long send(String from, List<String> to, String subj, String body) throws IOException {
        if (to.isEmpty()) throw new IOException("Recipient required");
        pm(to.get(0), subj + " | " + body);
        return System.currentTimeMillis();
    }

    @Override
    public void close() throws IOException {
        running = false;
        sock.close();
    }

    public static class Retrieved {
        public final String headers;
        public final int bodyLen;
        public final String body;
        public Retrieved(String h, int l, String b){ headers=h; bodyLen=l; body=b; }
    }
    public static class Stat { public int messages; public long storage; public int unread; }
}