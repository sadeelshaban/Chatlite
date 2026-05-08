package com.ChatLite.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramSocket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChatLiteClientApp extends JFrame {
    private final Font defaultFont = new Font("Segoe UI", Font.PLAIN, 13);
    private final Color bg = new Color(240, 242, 245);
    private final Color panelWhite = Color.WHITE;
    private final Color border = new Color(220, 223, 230);

    private final JButton connectBtn = new JButton("Connect");
    private final DefaultListModel<String> roomsModel = new DefaultListModel<>();
    private final JList<String> roomsList = new JList<>(roomsModel);
    private final JButton joinBtn = new JButton("Join");
    private final JButton leaveBtn = new JButton("Leave");
    private final DefaultListModel<String> usersModel = new DefaultListModel<>();
    private final JList<String> usersList = new JList<>(usersModel);
    private final DefaultTableModel chatModel = new DefaultTableModel(new Object[]{"User", "Message", "Time"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable chatTable = new JTable(chatModel);
    private final JTextField searchField = new JTextField(16);
    private final JTextField messageField = new JTextField("Type message here...");
    private final JButton sendBtn = new JButton("Send");
    private final JComboBox<String> pmToCombo = new JComboBox<>();
    private final JTextArea pmField = new JTextArea();
    private final JTextArea pmLogArea = new JTextArea();
    private final JButton pmQuickSendBtn = new JButton("Send");
    private final JButton pmBtn = new JButton("Send PM");
    private final JButton pmClearBtn = new JButton("Clear");
    private final JButton exportRoomBtn = new JButton("Export Room");
    private final JButton exportPmBtn = new JButton("Export PM");
    private final JLabel statusBar = new JLabel("Disconnected");
    private final JLabel headerStatus = new JLabel("STATUS: DISCONNECTED");
    private final JLabel headerUptime = new JLabel("Uptime: 00:00");
    private final JRadioButton activeRb = new JRadioButton("Active", true);
    private final JRadioButton busyRb = new JRadioButton("Busy");
    private final JRadioButton awayRb = new JRadioButton("Away");

    private String currentHost = "127.0.0.1";
    private int currentPort = 2525;

    private static class ChatEntry {
        final String user;
        final String message;
        final String time;
        ChatEntry(String user, String message, String time) {
            this.user = user;
            this.message = message;
            this.time = time;
        }
    }

    // Per-room message history (what appears in CHAT MESSAGES)
    private final Map<String, List<ChatEntry>> roomHistory = new HashMap<>();
    private final Set<String> joinedRooms = new HashSet<>();
    /** Server time at last successful JOIN per room; inbound ROOMMSG older than this is ignored. */
    private final Map<String, Long> roomJoinEpochMs = new HashMap<>();
    private ChatClientAPI api;
    private String currentRoom = "";      // room used for sending
    private String viewRoom = "";         // room currently displayed
    private String currentUser = "";
    private long connectTs = -1;
    private javax.swing.Timer uptimeTimer;
    private javax.swing.Timer rosterTimer;
    private UdpListener udpListener;
    private Thread udpThread;
    private int udpPort = -1;

    private static class ConnectionContext {
        final String user;
        final ChatClientAPI api;
        final UdpListener udpListener;
        final Thread udpThread;

        ConnectionContext(String user, ChatClientAPI api, UdpListener udpListener, Thread udpThread) {
            this.user = user;
            this.api = api;
            this.udpListener = udpListener;
            this.udpThread = udpThread;
        }
    }

    /**
     * Keep multiple TCP sessions alive so "online users" stays correct
     * even when the UI user changes via repeated Connect clicks.
     */
    private final List<ConnectionContext> connections = new ArrayList<>();

    /** Kept while TCP session is active; never replaced by room view / leave hints. */
    private String connectionFooterLine = "";
    /** Secondary hint (room view, errors, export notice). */
    private String auxFooterLine = "";

    public ChatLiteClientApp() {
        super("ChatLite Client");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1180, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(bg);

        setUIFont(new javax.swing.plaf.FontUIResource(defaultFont));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        top.setBorder(new EmptyBorder(6, 10, 6, 10));
        top.setBackground(panelWhite);
        top.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
        styleButton(connectBtn, new Color(64, 158, 255));
        top.add(connectBtn);
        top.add(Box.createHorizontalStrut(20));
        top.add(headerStatus);
        top.add(Box.createHorizontalStrut(20));
        top.add(headerUptime);
        add(top, BorderLayout.NORTH);

        JPanel left = new JPanel(new BorderLayout(6, 6));
        left.setBorder(new EmptyBorder(10, 10, 10, 5));
        left.setPreferredSize(new Dimension(240, 0));
        left.setOpaque(false);
        JPanel roomsPanel = new JPanel(new BorderLayout(4, 4));
        roomsPanel.setBorder(BorderFactory.createTitledBorder("CHAT ROOMS"));
        roomsPanel.setBackground(panelWhite);
        roomsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1, true),
                BorderFactory.createTitledBorder("CHAT ROOMS")
        ));
        roomsPanel.add(new JScrollPane(roomsList), BorderLayout.CENTER);
        JPanel roomsBtn = new JPanel(new GridLayout(1, 3, 4, 4));
        roomsBtn.setOpaque(false);
        styleButton(joinBtn, new Color(65, 150, 65));
        styleButton(leaveBtn, new Color(220, 80, 60));
        styleButton(exportRoomBtn, new Color(108, 117, 125));
        roomsBtn.add(joinBtn);
        roomsBtn.add(leaveBtn);
        roomsBtn.add(exportRoomBtn);
        roomsPanel.add(roomsBtn, BorderLayout.SOUTH);
        left.add(roomsPanel, BorderLayout.NORTH);

        JPanel usersPanel = new JPanel(new BorderLayout(4, 4));
        usersPanel.setBorder(BorderFactory.createTitledBorder("ONLINE USERS"));
        usersPanel.setBackground(panelWhite);
        usersPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1, true),
                BorderFactory.createTitledBorder("ONLINE USERS")
        ));
        usersPanel.add(new JScrollPane(usersList), BorderLayout.CENTER);
        left.add(usersPanel, BorderLayout.CENTER);

        JPanel userStatusPanel = new JPanel(new GridLayout(0, 1));
        userStatusPanel.setBorder(BorderFactory.createTitledBorder("USER STATUS"));
        userStatusPanel.setBackground(panelWhite);
        userStatusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1, true),
                BorderFactory.createTitledBorder("USER STATUS")
        ));
        ButtonGroup statusGroup = new ButtonGroup();
        statusGroup.add(activeRb);
        statusGroup.add(busyRb);
        statusGroup.add(awayRb);
        userStatusPanel.add(activeRb);
        userStatusPanel.add(busyRb);
        userStatusPanel.add(awayRb);
        left.add(userStatusPanel, BorderLayout.SOUTH);
        add(left, BorderLayout.WEST);

        chatTable.setRowHeight(24);
        chatTable.getTableHeader().setReorderingAllowed(false);
        chatTable.getTableHeader().setFont(defaultFont.deriveFont(Font.BOLD));
        JPanel center = new JPanel(new BorderLayout(6, 6));
        center.setBorder(new EmptyBorder(10, 5, 10, 5));
        center.setOpaque(false);
        JPanel centerCard = new JPanel(new BorderLayout(6, 6));
        centerCard.setBackground(panelWhite);
        centerCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1, true),
                BorderFactory.createTitledBorder("CHAT MESSAGES")
        ));
        JPanel chatTop = new JPanel(new BorderLayout(6, 6));
        chatTop.setOpaque(false);
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchPanel.setOpaque(false);
        searchPanel.add(new JLabel("Search:"));
        styleTextField(searchField);
        searchPanel.add(searchField);
        chatTop.add(searchPanel, BorderLayout.WEST);
        centerCard.add(chatTop, BorderLayout.NORTH);
        centerCard.add(new JScrollPane(chatTable), BorderLayout.CENTER);
        JPanel inputPanel = new JPanel(new BorderLayout(6, 6));
        inputPanel.setOpaque(false);
        inputPanel.setBorder(BorderFactory.createTitledBorder("MESSAGE INPUT"));
        styleTextField(messageField);
        styleButton(sendBtn, new Color(64, 158, 255));
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);
        centerCard.add(inputPanel, BorderLayout.SOUTH);
        center.add(centerCard, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout(6, 6));
        right.setBorder(new EmptyBorder(10, 5, 10, 10));
        right.setPreferredSize(new Dimension(300, 0));
        right.setOpaque(false);
        JPanel pmPanel = new JPanel(new GridLayout(0, 1, 6, 6));
        pmPanel.setBackground(panelWhite);
        pmPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1, true),
                BorderFactory.createTitledBorder("PRIVATE MSG")
        ));
        pmPanel.add(new JLabel("To:"));
        pmToCombo.setEditable(false);
        pmToCombo.addItem("");
        pmToCombo.setFont(defaultFont);
        pmPanel.add(pmToCombo);
        pmPanel.add(new JLabel("Message:"));
        pmField.setLineWrap(true);
        pmField.setWrapStyleWord(true);
        pmPanel.add(new JScrollPane(pmField));
        styleButton(pmQuickSendBtn, new Color(65, 150, 65));
        pmPanel.add(pmQuickSendBtn);
        right.add(pmPanel, BorderLayout.CENTER);

        JPanel pmPanel2 = new JPanel(new BorderLayout(6, 6));
        pmPanel2.setBackground(panelWhite);
        pmPanel2.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1, true),
                BorderFactory.createTitledBorder("PRIVATE MSG")
        ));
        pmLogArea.setRows(5);
        pmLogArea.setEditable(false);
        pmPanel2.add(new JScrollPane(pmLogArea), BorderLayout.CENTER);
        JPanel pmBottom = new JPanel(new GridLayout(1, 3, 6, 6));
        pmBottom.setOpaque(false);
        styleButton(pmBtn, new Color(64, 158, 255));
        styleButton(pmClearBtn, new Color(108, 117, 125));
        styleButton(exportPmBtn, new Color(108, 117, 125));
        pmBottom.add(pmBtn);
        pmBottom.add(pmClearBtn);
        pmBottom.add(exportPmBtn);
        pmPanel2.add(pmBottom, BorderLayout.SOUTH);
        right.add(pmPanel2, BorderLayout.SOUTH);
        add(right, BorderLayout.EAST);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(new EmptyBorder(0, 10, 5, 10));
        footer.add(statusBar, BorderLayout.CENTER);
        footer.setBackground(panelWhite);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1, true),
                new EmptyBorder(4, 10, 6, 10)
        ));

        add(footer, BorderLayout.SOUTH);

        connectBtn.addActionListener(e -> connect());
        joinBtn.addActionListener(e -> joinRoom());
        leaveBtn.addActionListener(e -> leaveRoom());
        sendBtn.addActionListener(e -> sendRoomMessage());
        pmBtn.addActionListener(e -> sendPrivateMessage());
        pmQuickSendBtn.addActionListener(e -> sendPrivateMessage());
        pmClearBtn.addActionListener(e -> pmLogArea.setText(""));
        exportPmBtn.addActionListener(e -> exportPrivateChat());
        exportRoomBtn.addActionListener(e -> exportCurrentRoomChat());
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applySearch(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applySearch(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applySearch(); }
        });
        roomsList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String r = roomsList.getSelectedValue();
                    if (r != null && !r.isBlank()) {
                        viewRoom = r;
                        applySearch();
                        refreshRoomFooterHint();
                        setRoomInputEnabled(joinedRooms.contains(r));
                    }
                }
            }
        });
        usersList.addListSelectionListener(e -> {
            // Keep recipient selection user-controlled via dropdown.
        });
        activeRb.addActionListener(e -> setStatus("ACTIVE"));
        busyRb.addActionListener(e -> setStatus("BUSY"));
        awayRb.addActionListener(e -> setStatus("AWAY"));
    }

    private void connect() {
        LoginDialog.LoginData loginData = LoginDialog.showDialog(
                this,
                currentUser.isBlank() ? "student1" : currentUser,
                currentHost,
                currentPort
        );

        if (loginData == null) {
            return;
        }

        new Thread(() -> {
            try {
                String host = loginData.host;
                int port = loginData.port;
                String user = loginData.username;
                String password = loginData.password;

                currentHost = host;
                currentPort = port;
                currentUser = user;
                String connectionUser = currentUser;

                // Reset UI state for the newly active "currentUser".
                SwingUtilities.invokeLater(() -> {
                    joinedRooms.clear();
                    roomHistory.clear();
                    roomJoinEpochMs.clear();
                    viewRoom = "";
                    currentRoom = "";
                    chatModel.setRowCount(0);
                    messageField.setText("");
                    setRoomInputEnabled(false);
                    refreshRoomFooterHint();
                });

                ChatClientAPI newApi = new ChatClientAPI(host, port);
                api = newApi;

                if (!newApi.userExists(connectionUser)) {
                    setDisconnectedFooter("User not found: " + currentUser);
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                            this,
                            "Username not found in server records.",
                            "Login Rejected",
                            JOptionPane.WARNING_MESSAGE
                    ));
                    newApi.close();
                    api = null;
                    return;
                }



                newApi.hello(connectionUser);

                boolean ok = newApi.auth(connectionUser, password);
                if (!ok) {
                    setDisconnectedFooter("Authentication failed");
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                            this,
                            "Wrong password.",
                            "Login Failed",
                            JOptionPane.ERROR_MESSAGE
                    ));
                    newApi.close();
                    api = null;
                    return;
                }

                String st = getSelectedStatus();
                newApi.setStatus(st);

                int localUdpPort = findFreeUdpPort();
                UdpListener newUdpListener = new UdpListener(localUdpPort, msg -> SwingUtilities.invokeLater(() -> {
                    if (!connectionUser.equalsIgnoreCase(currentUser)) return;
                    if (msg != null && msg.startsWith("NOTIFY PM")) {
                        pmLogArea.append("[UDP] " + msg + "\n");
                    }
                }));
                Thread newUdpThread = new Thread(newUdpListener, "udp-listener-" + connectionUser);
                newUdpThread.setDaemon(true);
                newUdpThread.start();

                newApi.setUdpPort(localUdpPort);

                udpPort = localUdpPort;
                udpListener = newUdpListener;
                udpThread = newUdpThread;

                connections.add(new ConnectionContext(connectionUser, newApi, newUdpListener, newUdpThread));

                setConnectedFooter(host, port, connectionUser);
                SwingUtilities.invokeLater(() -> headerStatus.setText("STATUS: " + st));
                connectTs = System.currentTimeMillis();
                startUptimeTimer();
                startRosterRefresh();
                refreshUsersAndRooms();
                startEventReader(newApi, connectionUser);
            } catch (Exception ex) {
                setDisconnectedFooter("Connect failed: " + ex.getMessage());
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "Connect failed: " + ex.getMessage()));
            }
        }, "connect-thread").start();
    }

    private String getSelectedStatus() {
        if (busyRb.isSelected()) return "BUSY";
        if (awayRb.isSelected()) return "AWAY";
        return "ACTIVE";
    }

    private void refreshUsersAndRooms() {
        if (api == null) return;
        try {
            List<String> users = api.users();
            List<String> rooms = api.rooms();
            SwingUtilities.invokeLater(() -> {
                String prevRecipient = pmToCombo.getSelectedItem() == null ? "" : pmToCombo.getSelectedItem().toString();
                String prevRoom = roomsList.getSelectedValue();
                String currentLower = currentUser == null ? "" : currentUser.trim().toLowerCase();

                usersModel.clear();
                pmToCombo.removeAllItems();
                pmToCombo.addItem("");

                Map<String, String> recipientByLower = new HashMap<>();
                for (String u : users) {
                    // Server format: "<username> <status>"
                    String[] parts = u.trim().split("\\s+");
                    if (parts.length == 0) continue;
                    String name = parts[0].trim();
                    String status = parts.length > 1 ? parts[1].trim() : "";
                    // UI requirement: show only other online users (exclude self).
                    if (name.equalsIgnoreCase(currentLower)) continue;
                    usersModel.addElement(name + " - " + (status.isBlank() ? "online" : status));
                    recipientByLower.put(name.toLowerCase(), name);
                }

                for (String name : recipientByLower.values()) pmToCombo.addItem(name);

                if (prevRecipient != null && !prevRecipient.isBlank()) {
                    String restored = recipientByLower.get(prevRecipient.toLowerCase());
                    if (restored != null) pmToCombo.setSelectedItem(restored);
                }

                roomsModel.clear();
                for (String r : rooms) roomsModel.addElement(r);

                if (prevRoom != null && !prevRoom.isBlank()) roomsList.setSelectedValue(prevRoom, true);
            });
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> setAuxFooter("Refresh failed: " + ex.getMessage()));
        }
    }

    private void joinRoom() {
        if (api == null) return;
        String room = roomsList.getSelectedValue();
        if (room == null || room.isBlank()) return;
        // Already in this room on the server — keep transcript; duplicate JOIN would wipe history.
        if (joinedRooms.contains(room)) {
            currentRoom = room;
            viewRoom = room;
            applySearch();
            setRoomInputEnabled(true);
            refreshRoomFooterHint();
            return;
        }
        new Thread(() -> {
            try {
                joinedRooms.add(room);
                try {
                    long joinEpoch = api.join(room);
                    roomJoinEpochMs.put(room, joinEpoch);
                } catch (Exception joinEx) {
                    joinedRooms.remove(room);
                    roomJoinEpochMs.remove(room);
                    throw joinEx;
                }
                currentRoom = room;
                viewRoom = room;
                currentRoom = room;
                addRoomSystem(room, "Joined room: " + room);
                applySearch();
                setRoomInputEnabled(joinedRooms.contains(viewRoom));
                SwingUtilities.invokeLater(this::refreshRoomFooterHint);
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Join failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
            }
        }, "join-thread").start();
    }

    private void leaveRoom() {
        if (api == null) return;
        String room = roomsList.getSelectedValue();
        if (room == null || room.isBlank()) room = viewRoom;
        if (room == null || room.isBlank()) {
            JOptionPane.showMessageDialog(this, "Select a room in the list, then click Leave.");
            return;
        }
        final String toLeave = room;
        new Thread(() -> {
            try {
                api.leave(toLeave);
                joinedRooms.remove(toLeave);
                addRoomSystem(toLeave, "You left the " + toLeave + " chat room.");
                if (currentRoom.equals(toLeave)) currentRoom = "";
                if (viewRoom.equals(toLeave)) {
                    viewRoom = "";
                    SwingUtilities.invokeLater(() -> chatModel.setRowCount(0));
                }
                applySearch();
                setRoomInputEnabled(!viewRoom.isBlank() && joinedRooms.contains(viewRoom));
                SwingUtilities.invokeLater(this::refreshRoomFooterHint);
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Leave failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
            }
        }, "leave-thread").start();
    }

    private void sendRoomMessage() {
        if (api == null) return;
        // Send to the currently viewed room (as requested).
        if (viewRoom == null || viewRoom.isBlank()) {
            JOptionPane.showMessageDialog(this, "Double-click a room to view it, then join it to send messages.");
            return;
        }
        if (!joinedRooms.contains(viewRoom)) {
            JOptionPane.showMessageDialog(this, "You must join the selected room before sending messages.");
            return;
        }
        String msg = messageField.getText().trim();
        if (msg.isBlank()) return;
        messageField.setText("");
        new Thread(() -> {
            try {
                api.msg(viewRoom, msg);
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Send failed: " + ex.getMessage(), "Room message", JOptionPane.WARNING_MESSAGE));
            }
        }, "send-thread").start();
    }

    private void sendPrivateMessage() {
        if (api == null) return;
        Object selected = pmToCombo.getSelectedItem();
        String to = selected == null ? "" : selected.toString().trim();
        String msg = pmField.getText().trim();
        if (to.isBlank() || msg.isBlank()) return;
        pmField.setText("");
        new Thread(() -> {
            try {
                api.pm(to, msg);
                String ts = new SimpleDateFormat("HH:mm:ss").format(new Date());
                SwingUtilities.invokeLater(() -> pmLogArea.append("[" + ts + "] To " + to + ": " + msg + "\n"));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Private message failed: " + ex.getMessage(), "PM", JOptionPane.ERROR_MESSAGE));
            }
        }, "pm-thread").start();
    }

    private void startEventReader(ChatClientAPI readerApi, String connectionUser) {
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    String line = readerApi.readEvent();
                    if (line == null) {
                        handleServerDisconnect(connectionUser, "You were disconnected by the server.");
                        break;
                    }

                    if (line.startsWith("ROOMMSG ")) {
                        String payload = line.substring(8);
                        String room = parseRoomFromRoomMsgPayload(payload);
                        if (connectionUser.equalsIgnoreCase(currentUser) && joinedRooms.contains(room)) {
                            addRoomMsg(payload);
                        }
                    } else if (line.startsWith("PMFROM ")) {
                        String rest = line.substring(7).trim();
                        int sp1 = rest.indexOf(' ');
                        int sp2 = sp1 > 0 ? rest.indexOf(' ', sp1 + 1) : -1;
                        String from = sp1 > 0 ? rest.substring(0, sp1) : "?";
                        String time = (sp2 > sp1) ? rest.substring(sp1 + 1, sp2) : "";
                        String pmsg = (sp2 > sp1) ? rest.substring(sp2 + 1) : rest.substring(sp1 + 1);
                        String logLine = "[" + time + "] From " + from + ": " + pmsg;
                        if (connectionUser.equalsIgnoreCase(currentUser)) {
                            SwingUtilities.invokeLater(() -> pmLogArea.append(logLine + "\n"));
                        }
                    } else if (line.startsWith("BROADCAST ")) {
                        String rest = line.substring(10).trim();
                        int sp1 = rest.indexOf(' ');
                        int sp2 = sp1 > 0 ? rest.indexOf(' ', sp1 + 1) : -1;

                        String from = sp1 > 0 ? rest.substring(0, sp1) : "SERVER";
                        String time = (sp2 > sp1) ? rest.substring(sp1 + 1, sp2) : "";
                        String bmsg = (sp2 > sp1) ? rest.substring(sp2 + 1) : rest;

                        if (connectionUser.equalsIgnoreCase(currentUser)) {
                            String finalFrom = from;
                            String finalTime = time;
                            String finalMsg = bmsg;
                            SwingUtilities.invokeLater(() ->
                                    pmLogArea.append("[" + finalTime + "] " + finalFrom + " broadcast: " + finalMsg + "\n"));
                        }
                    }
                }
            } catch (Exception ex) {
                handleServerDisconnect(connectionUser, "Connection lost: " + ex.getMessage());
            }
        }, "event-reader");

        t.setDaemon(true);
        t.start();
    }

    private static String parseRoomFromRoomMsgPayload(String payload) {
        int sp = payload.indexOf(' ');
        return sp > 0 ? payload.substring(0, sp) : "";
    }

    private void addRoomSystem(String room, String message) {
        String r = room == null ? "" : room;
        if (r.isBlank()) r = viewRoom == null ? "" : viewRoom;
        if (r.isBlank()) r = "(no room)";
        roomHistory.computeIfAbsent(r, k -> new ArrayList<>())
                .add(new ChatEntry("INFO", message, new SimpleDateFormat("HH:mm:ss").format(new Date())));
    }

    /** Room chat only — never used for PM/UDP (those go to pmLogArea). */
    private void appendLocal(String type, String text) {
        String t = type == null ? "INFO" : type.trim().toUpperCase();
        if ("UDP".equals(t) || "PM".equals(t) || "ERROR".equals(t)) {
            setAuxFooter(text);
            return;
        }
        String msg = (text == null) ? "" : text;
        addRoomSystem(viewRoom, msg);
        applySearch();
    }

    private void applySearch() {
        String q = searchField.getText().trim().toLowerCase();
        SwingUtilities.invokeLater(() -> {
            chatModel.setRowCount(0);
            if (viewRoom == null || viewRoom.isBlank()) return;
            List<ChatEntry> entries = roomHistory.getOrDefault(viewRoom, List.of());
            for (ChatEntry e : entries) {
                if (!q.isBlank() && !(e.user + " " + e.message).toLowerCase().contains(q)) continue;
                chatModel.addRow(new Object[]{e.user, e.message, e.time});
            }
        });
    }

    private void paintFooter() {
        String text;
        if (connectionFooterLine.isEmpty()) {
            text = auxFooterLine.isEmpty() ? "Disconnected" : auxFooterLine;
        } else {
            text = connectionFooterLine + (auxFooterLine.isEmpty() ? "" : " · " + auxFooterLine);
        }
        SwingUtilities.invokeLater(() -> statusBar.setText(text));
    }

    private void setConnectedFooter(String host, int port, String user) {
        connectionFooterLine = "Connected to " + host + ":" + port + " as " + user;
        auxFooterLine = "";
        paintFooter();
    }

    private void setDisconnectedFooter(String message) {
        connectionFooterLine = "";
        auxFooterLine = message == null ? "" : message;
        paintFooter();
    }

    private void setAuxFooter(String message) {
        auxFooterLine = message == null ? "" : message;
        paintFooter();
    }

    private void refreshRoomFooterHint() {
        if (connectionFooterLine.isEmpty()) {
            paintFooter();
            return;
        }
        if (viewRoom == null || viewRoom.isBlank()) {
            auxFooterLine = "";
        } else {
            auxFooterLine = "Viewing room: " + viewRoom
                    + (joinedRooms.contains(viewRoom) ? " (joined)" : " (not joined)");
        }
        paintFooter();
    }

    private int findFreeUdpPort() throws IOException {
        try (DatagramSocket ds = new DatagramSocket(0)) {
            return ds.getLocalPort();
        }
    }

    private void setStatus(String status) {
        if (api == null) return;
        new Thread(() -> {
            try {
                api.setStatus(status);
                SwingUtilities.invokeLater(() -> headerStatus.setText("STATUS: " + status));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> setAuxFooter("Status update failed: " + ex.getMessage()));
            }
        }, "status-thread").start();
    }

    private void startUptimeTimer() {
        if (uptimeTimer != null) uptimeTimer.stop();
        uptimeTimer = new javax.swing.Timer(1000, e -> {
            if (connectTs <= 0) return;
            long sec = (System.currentTimeMillis() - connectTs) / 1000L;
            long m = sec / 60;
            long s = sec % 60;
            headerUptime.setText(String.format("Uptime: %02d:%02d", m, s));
        });
        uptimeTimer.start();
    }

    private void startRosterRefresh() {
        if (rosterTimer != null) rosterTimer.stop();
        rosterTimer = new javax.swing.Timer(3000, e -> {
            if (api != null) refreshUsersAndRooms();
        });
        rosterTimer.start();
    }

    private void exportCurrentRoomChat() {
        String room = (viewRoom == null || viewRoom.isBlank()) ? currentRoom : viewRoom;
        if (room == null || room.isBlank()) {
            JOptionPane.showMessageDialog(this, "Select a room first to export its chat.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("room-" + room + "-" + currentUser + ".txt"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File out = chooser.getSelectedFile();
        try (FileWriter fw = new FileWriter(out)) {
            for (ChatEntry e : roomHistory.getOrDefault(room, List.of())) {
                fw.write("[" + e.time + "] " + e.user + ": " + e.message + System.lineSeparator());
            }
            setAuxFooter("Room chat exported: " + out.getName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage());
        }
    }

    private void exportPrivateChat() {
        if (pmLogArea.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "No private messages to export.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("pm-" + currentUser + ".txt"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (FileWriter fw = new FileWriter(chooser.getSelectedFile())) {
            fw.write(pmLogArea.getText());
            setAuxFooter("Private chat exported: " + chooser.getSelectedFile().getName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage());
        }
    }

    private void setRoomInputEnabled(boolean on) {
        SwingUtilities.invokeLater(() -> {
            messageField.setEnabled(on);
            sendBtn.setEnabled(on);
        });
    }

    private void addRoomMsg(String payload) {
        // New: "<room> <username> <epochMs> <HH:mm:ss> <message...>"
        // Legacy: "<room> <username> <HH:mm:ss> <message...>"
        String room;
        String user;
        String time;
        String message;
        long serverEpoch = -1L;

        String[] p5 = payload.split(" ", 5);
        if (p5.length >= 5 && p5[2].matches("\\d{10,}")) {
            room = p5[0];
            user = p5[1];
            try {
                serverEpoch = Long.parseLong(p5[2]);
            } catch (NumberFormatException e) {
                return;
            }
            time = p5[3];
            message = p5[4];
        } else {
            String[] p4 = payload.split(" ", 4);
            if (p4.length < 4) return;
            room = p4[0];
            user = p4[1];
            time = p4[2];
            message = p4[3];
        }

        if (serverEpoch >= 0) {
            Long cutoff = roomJoinEpochMs.get(room);
            if (cutoff != null && serverEpoch < cutoff) {
                return;
            }
        }

        List<ChatEntry> list = roomHistory.computeIfAbsent(room, k -> new ArrayList<>());
        ChatEntry entry = new ChatEntry(user, message, time);
        if (!list.isEmpty()) {
            ChatEntry last = list.get(list.size() - 1);
            if (last.user.equals(entry.user) && last.message.equals(entry.message) && last.time.equals(entry.time)) {
                return;
            }
        }
        list.add(entry);
        if (room.equals(viewRoom)) applySearch();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatLiteClientApp().setVisible(true));
    }

    private void styleButton(JButton button, Color bgColor) {
        button.setFont(defaultFont.deriveFont(Font.BOLD, 12));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 0, 0, 0.1f)),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            final Color originalBg = bgColor;
            final Color hoverBg = new Color(
                    Math.max(0, bgColor.getRed() - 18),
                    Math.max(0, bgColor.getGreen() - 18),
                    Math.max(0, bgColor.getBlue() - 18)
            );
            @Override public void mouseEntered(java.awt.event.MouseEvent evt) { button.setBackground(hoverBg); }
            @Override public void mouseExited(java.awt.event.MouseEvent evt) { button.setBackground(originalBg); }
        });
    }

    private void styleTextField(JComponent field) {
        field.setFont(defaultFont);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
    }

    private static void setUIFont(javax.swing.plaf.FontUIResource f) {
        java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof javax.swing.plaf.FontUIResource) UIManager.put(key, f);
        }
    }
    private void handleServerDisconnect(String connectionUser, String reason) {
        if (!connectionUser.equalsIgnoreCase(currentUser)) {
            return;
        }

        try {
            if (api != null) {
                api.close();
            }
        } catch (Exception ignored) {
        }
        api = null;

        if (udpListener != null) {
            udpListener.stop();
            udpListener = null;
        }
        udpThread = null;
        udpPort = -1;

        joinedRooms.clear();
        roomHistory.clear();
        roomJoinEpochMs.clear();
        currentRoom = "";
        viewRoom = "";

        SwingUtilities.invokeLater(() -> {
            chatModel.setRowCount(0);
            usersModel.clear();
            roomsModel.clear();
            pmToCombo.removeAllItems();
            pmToCombo.addItem("");
            setRoomInputEnabled(false);
            headerStatus.setText("STATUS: DISCONNECTED");
            setDisconnectedFooter(reason == null || reason.isBlank() ? "Disconnected from server" : reason);
            JOptionPane.showMessageDialog(
                    this,
                    reason == null || reason.isBlank() ? "Connection to server was closed." : reason,
                    "Disconnected",
                    JOptionPane.WARNING_MESSAGE
            );
        });
    }
}