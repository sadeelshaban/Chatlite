package com.ChatLite.server;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TcpProtocolHandler implements Runnable {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Set<TcpProtocolHandler> CLIENTS = ConcurrentHashMap.newKeySet();

    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    public TcpProtocolHandler(Socket socket) {
        this.socket = socket;
        CLIENTS.add(this);
    }

    private void handleLine(String line) throws IOException {
        if (line.isEmpty()) return;

        String[] parts = line.split(" ", 2);
        String cmd = parts[0].toUpperCase(Locale.ROOT);
        String rest = parts.length > 1 ? parts[1] : "";

        switch (cmd) {
            case "CHECKUSER" -> handleCheckUser(rest);
            case "UDPPORT" -> handleUdpPort(rest);
            case "HELLO", "HELO" -> handleHello(rest);
            case "AUTH" -> handleAuth(rest);
            case "STATUS", "SETSTAT" -> handleStatus(rest);
            case "JOIN" -> handleJoin(rest);
            case "MSG" -> handleMsg(rest);
            case "PM" -> handlePm(rest);
            case "USERS", "WHO" -> handleUsers();
            case "ROOMS" -> handleRooms();
            case "LEAVE" -> handleLeave(rest);
            case "QUIT" -> {
                sendLine("221 BYE\n");
                disconnect();
            }
            default -> sendLine("500 UNKNOWN\n");
        }
    }

    private void handleHello(String rest) throws IOException {
        String user = rest.trim().toLowerCase(Locale.ROOT);
        if (user.isEmpty()) {
            sendLine("400 USER REQUIRED\n");
            return;
        }
        if (!UserStore.exists(user)) {
            sendLine("404 USER NOT FOUND\n");
            ServerLog.info("LOGIN_FAIL user=" + user + " reason=not_registered");
            return;
        }
        sendLine("200 WELCOME\n");
        ServerLog.info("HELLO_OK user=" + user);
    }

    private void handleCheckUser(String rest) throws IOException {
        String user = rest.trim().toLowerCase(Locale.ROOT);
        if (user.isEmpty()) {
            sendLine("400 USER REQUIRED\n");
            return;
        }
        if (UserStore.exists(user)) sendLine("250 EXISTS\n");
        else sendLine("550 NOT FOUND\n");
    }

    private void handleUdpPort(String rest) throws IOException {
        if (!isReady()) return;

        try {
            int p = Integer.parseInt(rest.trim());
            if (p <= 0 || p > 65535) {
                sendLine("400 BAD UDP PORT\n");
                return;
            }
            RosterService.setUdpPort(username, p);
            sendLine("250 UDP OK\n");
            ServerLog.info("UDPPORT_SET user=" + username + " port=" + p);
        } catch (Exception ex) {
            sendLine("400 BAD UDP PORT\n");
        }
    }

    private void handleAuth(String rest) throws IOException {
        String[] toks = rest.trim().split("\\s+", 2);
        if (toks.length < 2) {
            sendLine("535 AUTH FAILED\n");
            return;
        }

        String user = toks[0].trim().toLowerCase(Locale.ROOT);
        String pass = toks[1].trim();

        boolean ok = UserStore.checkUser(user, pass);
        InetSocketAddress addr = (InetSocketAddress) socket.getRemoteSocketAddress();
        String ip = addr.getAddress().getHostAddress();

        ServerLog.auth(user, ip, ok);

        if (!ok) {
            sendLine("535 AUTH FAILED\n");
            return;
        }

        username = user;
        RosterService.setOnline(user, ip);
        sendLine("235 OK\n");
    }

    private void handleJoin(String rest) throws IOException {
        if (!isReady()) return;

        String room = rest.trim().toLowerCase(Locale.ROOT);
        if (room.isEmpty()) {
            sendLine("400 ROOM REQUIRED\n");
            return;
        }

        boolean ok = ChatService.join(username, room);
        if (!ok) {
            sendLine("404 ROOM NOT FOUND\n");
            return;
        }

        long joinEpochMs = System.currentTimeMillis();
        sendLine("210 JOINED " + room + " " + joinEpochMs + "\n");
        ServerLog.info("JOIN user=" + username + " room=" + room);
    }

    private void handleStatus(String rest) throws IOException {
        if (!isReady()) return;

        String status = rest.trim().toUpperCase(Locale.ROOT);
        if (!(status.equals("ACTIVE") || status.equals("BUSY") || status.equals("AWAY"))) {
            sendLine("400 BAD STATUS\n");
            return;
        }

        RosterService.setStatus(username, status);
        sendLine("250 OK\n");
    }

    private void handleMsg(String rest) throws IOException {
        if (!isReady()) return;

        String[] toks = rest.split(" ", 2);
        if (toks.length < 2) {
            sendLine("400 MSG PARAMS\n");
            return;
        }

        String room = toks[0].trim();
        String msg = toks[1].trim();

        if (msg.isEmpty()) {
            sendLine("400 MSG EMPTY\n");
            return;
        }

        if (exceedsMaxMessageSize(msg)) {
            sendLine("413 MESSAGE TOO LARGE\n");
            ServerLog.info("MSG_REJECTED user=" + username + " reason=too_large size="
                    + msg.length() + " max=" + ServerConfig.getMaxMessageSize());
            return;
        }

        if (!ChatService.membersOf(room).contains(username)) {
            sendLine("403 NOT IN ROOM\n");
            return;
        }

        long sentEpochMs = System.currentTimeMillis();
        String event = "ROOMMSG " + room + " " + username + " " + sentEpochMs + " "
                + TS.format(LocalTime.now()) + " " + msg + "\n";

        Set<String> members = ChatService.membersOf(room);

        for (TcpProtocolHandler c : snapshotClients()) {
            if (c.username != null && members.contains(c.username)) {
                c.sendLine(event);

                if (!c.username.equalsIgnoreCase(username)) {
                    ServerStats.recordReceived(c.username, msg);

                    RosterService.Entry e = RosterService.get(c.username);
                    if (e != null && e.udpPort > 0 && !"offline".equalsIgnoreCase(e.status)) {
                        UdpNotifier.notifyAddress(
                                e.ip,
                                e.udpPort,
                                "NOTIFY ROOM " + room + " FROM " + username
                        );
                    }
                }
            }
        }

        ServerStats.recordSent(username, msg);
        sendLine("211 SENT\n");
        ServerLog.info("MSG room=" + room + " from=" + username + " text=" + msg);
    }

    private void handlePm(String rest) throws IOException {
        if (!isReady()) return;

        String[] toks = rest.split(" ", 2);
        if (toks.length < 2) {
            sendLine("400 PM PARAMS\n");
            return;
        }

        String to = toks[0].trim().toLowerCase(Locale.ROOT);
        String msg = toks[1].trim();

        if (msg.isEmpty()) {
            sendLine("400 PM EMPTY\n");
            return;
        }

        if (exceedsMaxMessageSize(msg)) {
            sendLine("413 MESSAGE TOO LARGE\n");
            ServerLog.info("PM_REJECTED user=" + username + " reason=too_large size="
                    + msg.length() + " max=" + ServerConfig.getMaxMessageSize());
            return;
        }

        TcpProtocolHandler target = findByUsername(to);
        if (target == null) {
            sendLine("404 USER OFFLINE\n");
            return;
        }

        String event = "PMFROM " + username + " " + TS.format(LocalTime.now()) + " " + msg + "\n";
        target.sendLine(event);

        ServerStats.recordSent(username, msg);
        ServerStats.recordReceived(to, msg);

        RosterService.Entry targetEntry = RosterService.get(to);
        if (targetEntry != null && targetEntry.udpPort > 0 && !"offline".equalsIgnoreCase(targetEntry.status)) {
            UdpNotifier.notifyAddress(targetEntry.ip, targetEntry.udpPort, "NOTIFY PM FROM " + username);
        }

        sendLine("212 PRIVATE SENT\n");
        ServerLog.info("PM from=" + username + " to=" + to + " text=" + msg);
    }

    private void handleUsers() throws IOException {
        List<RosterService.Entry> users = RosterService.snapshot();
        int online = 0;

        for (RosterService.Entry u : users) {
            if (!"offline".equalsIgnoreCase(u.status)) online++;
        }

        sendLine("213 " + online + "\n");
        for (RosterService.Entry u : users) {
            if ("offline".equalsIgnoreCase(u.status)) continue;
            sendLine("213U " + u.username + " " + u.status + "\n");
        }
        sendLine("213 END\n");
    }

    private void handleRooms() throws IOException {
        for (String room : ChatService.listRooms()) {
            sendLine("214 " + room + "\n");
        }
        sendLine("214 END\n");
    }

    private void handleLeave(String rest) throws IOException {
        if (!isReady()) return;

        String room = rest.trim();
        if (room.isEmpty()) {
            sendLine("400 ROOM REQUIRED\n");
            return;
        }

        ChatService.leave(username, room);
        sendLine("215 LEFT\n");
        ServerLog.info("LEAVE user=" + username + " room=" + room);
    }

    private boolean isReady() throws IOException {
        if (username == null || username.isBlank()) {
            sendLine("401 LOGIN FIRST\n");
            return false;
        }
        return true;
    }

    private static TcpProtocolHandler findByUsername(String user) {
        for (TcpProtocolHandler c : CLIENTS) {
            if (user.equals(c.username)) return c;
        }
        return null;
    }

    private static List<TcpProtocolHandler> snapshotClients() {
        return new ArrayList<>(CLIENTS);
    }

    private void disconnect() throws IOException {
        CLIENTS.remove(this);

        if (username != null) {
            RosterService.setOffline(username);
            ChatService.leaveAll(username);
        }

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    public synchronized void sendLine(String s) {
        if (out != null) {
            out.print(s);
            out.flush();
        }
    }

    private boolean exceedsMaxMessageSize(String msg) {
        return msg != null && msg.length() > ServerConfig.getMaxMessageSize();
    }

    public static void broadcastFromServer(String message) {
        if (message == null || message.isBlank()) return;

        String text = message.trim();
        String event = "BROADCAST SERVER " + TS.format(LocalTime.now()) + " " + text + "\n";

        for (TcpProtocolHandler c : snapshotClients()) {
            try {
                c.sendLine(event);
                if (c.username != null && !c.username.isBlank()) {
                    ServerStats.recordReceived(c.username, text);
                }
            } catch (Exception ignored) {
            }
        }

        ServerLog.info("BROADCAST text=" + text);
    }

    public static boolean kickUser(String userToKick) {
        if (userToKick == null || userToKick.isBlank()) return false;

        String normalizedUser = userToKick.trim().toLowerCase(Locale.ROOT);
        TcpProtocolHandler target = findByUsername(normalizedUser);
        if (target == null) return false;

        try {
            target.disconnect();
            ServerLog.info("USER_KICKED username=" + normalizedUser);
            return true;
        } catch (Exception ex) {
            ServerLog.error("KICK_USER", ex.toString());
            return false;
        }
    }

    public static boolean unregisterUser(String username) {
        if (username == null || username.isBlank()) return false;

        String normalizedUser = username.trim().toLowerCase(Locale.ROOT);
        boolean existed = UserStore.removeUser(normalizedUser);
        if (!existed) {
            return false;
        }

        TcpProtocolHandler target = findByUsername(normalizedUser);
        if (target != null) {
            try {
                target.disconnect();
            } catch (Exception ex) {
                ServerLog.error("UNREGISTER_USER_DISCONNECT", ex.toString());
            }
        }

        ChatService.leaveAll(normalizedUser);
        RosterService.remove(normalizedUser);
        ServerLog.info("USER_UNREGISTERED username=" + normalizedUser);
        return true;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII), true);

            String line;
            while ((line = in.readLine()) != null) {
                handleLine(line.trim());
            }
        } catch (Exception e) {
            ServerLog.error("CLIENT_HANDLER", e.toString());
        } finally {
            try {
                disconnect();
            } catch (Exception ignored) {
            }
        }
    }
}