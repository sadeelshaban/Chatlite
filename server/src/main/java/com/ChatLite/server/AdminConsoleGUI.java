

package com.ChatLite.server;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class AdminConsoleGUI extends JFrame {

    private JTable usersTable;
    private JTable statsTable;
    private JTextArea logsArea;

    private JList<String> registeredUsersList;
    private DefaultListModel<String> registeredUsersModel;

    private JList<String> activeRoomsList;
    private DefaultListModel<String> activeRoomsModel;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField broadcastField;
    private JTextField maxMessageSizeField;

    private JComboBox<String> logFilterCombo;

    private JButton addUserBtn;
    private JButton removeUserBtn;
    private JButton saveSettingsBtn;
    private JButton clearLogsBtn;

    private JLabel serverStatusLabel;
    private JLabel uptimeLabel;

    private JButton createRoomBtn;
    private JButton deleteRoomBtn;

    private static final Path LOG_FILE = Path.of("logs/server.log");
    private final Font defaultFont = new Font("Segoe UI", Font.PLAIN, 13);
    private final long guiStartTime = System.currentTimeMillis();


    public AdminConsoleGUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        setTitle("ChatLite Server Console");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1350, 820));
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(240, 242, 245));

        setUIFont(new javax.swing.plaf.FontUIResource(defaultFont));

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(240, 242, 245));
        setContentPane(mainPanel);

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        JLabel topTitle = new JLabel("Server Status: ONLINE");
        topTitle.setFont(defaultFont.deriveFont(Font.BOLD, 14f));

        JLabel tcpPortLabel = new JLabel("PORT (TCP): " + MainServer.TCP_PORT);
        tcpPortLabel.setFont(defaultFont.deriveFont(Font.BOLD));

        JLabel udpPortLabel = new JLabel("PORT (UDP): " + UdpNotifier.getBoundPort());
        udpPortLabel.setFont(defaultFont.deriveFont(Font.BOLD));

        uptimeLabel = new JLabel("Uptime: 00:00:00");
        uptimeLabel.setFont(defaultFont.deriveFont(Font.BOLD));

        JLabel maxSizeLabel = new JLabel("Max Message Size:");
        maxSizeLabel.setFont(defaultFont.deriveFont(Font.BOLD));

        maxMessageSizeField = new JTextField(String.valueOf(ServerConfig.getMaxMessageSize()), 6);
        styleTextField(maxMessageSizeField);

        saveSettingsBtn = new JButton("Apply Settings");
        styleButton(saveSettingsBtn, new Color(64, 158, 255));

        topBar.add(topTitle);
        topBar.add(Box.createHorizontalStrut(12));
        topBar.add(tcpPortLabel);
        topBar.add(Box.createHorizontalStrut(12));
        topBar.add(udpPortLabel);
        topBar.add(Box.createHorizontalStrut(12));
        topBar.add(uptimeLabel);
        topBar.add(Box.createHorizontalStrut(20));
        topBar.add(maxSizeLabel);
        topBar.add(maxMessageSizeField);
        topBar.add(saveSettingsBtn);

        mainPanel.add(topBar, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setOpaque(false);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        JPanel leftColumn = new JPanel(new BorderLayout(8, 8));
        leftColumn.setOpaque(false);
        leftColumn.setPreferredSize(new Dimension(320, 0));

        JPanel addPanel = new JPanel(new GridBagLayout());
        addPanel.setBackground(Color.WHITE);
        addPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210)),
                BorderFactory.createTitledBorder("User Management")
        ));
        addPanel.setPreferredSize(new Dimension(320, 185));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 6, 3, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(defaultFont.deriveFont(Font.BOLD));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        addPanel.add(userLabel, gbc);

        usernameField = new JTextField();
        styleTextField(usernameField);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        addPanel.add(usernameField, gbc);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(defaultFont.deriveFont(Font.BOLD));
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0.3;
        addPanel.add(passLabel, gbc);

        passwordField = new JPasswordField();
        styleTextField(passwordField);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        addPanel.add(passwordField, gbc);

        addUserBtn = new JButton("Create User");
        styleButton(addUserBtn, new Color(65, 150, 65));
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        addPanel.add(addUserBtn, gbc);

        JPanel registeredUsersPanel = new JPanel(new BorderLayout(6, 6));
        registeredUsersPanel.setBackground(Color.WHITE);
        registeredUsersPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210)),
                BorderFactory.createTitledBorder("Registered Users")
        ));

        registeredUsersModel = new DefaultListModel<>();
        registeredUsersList = new JList<>(registeredUsersModel);
        registeredUsersList.setFont(defaultFont);
        registeredUsersPanel.add(new JScrollPane(registeredUsersList), BorderLayout.CENTER);

        JPanel regBtnPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        regBtnPanel.setOpaque(false);

        removeUserBtn = new JButton("Delete Selected");
        styleButton(removeUserBtn, new Color(220, 80, 60));

        JButton resetPassBtn = new JButton("Reset Password");
        styleButton(resetPassBtn, new Color(255, 193, 7));

        regBtnPanel.add(removeUserBtn);
        regBtnPanel.add(resetPassBtn);
        registeredUsersPanel.add(regBtnPanel, BorderLayout.SOUTH);

        leftColumn.add(addPanel, BorderLayout.NORTH);
        leftColumn.add(registeredUsersPanel, BorderLayout.CENTER);
        contentPanel.add(leftColumn, BorderLayout.WEST);

        JPanel centerColumn = new JPanel(new BorderLayout(8, 8));
        centerColumn.setOpaque(false);

        JPanel sessionsPanel = new JPanel(new BorderLayout(6, 6));
        sessionsPanel.setBackground(Color.WHITE);
        sessionsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210)),
                BorderFactory.createTitledBorder("Active Sessions")
        ));

        String[] cols = {"Username", "Status", "IP Address", "Last Seen"};
        usersTable = new JTable(new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        usersTable.setRowHeight(28);
        usersTable.setFont(defaultFont);
        usersTable.getTableHeader().setFont(defaultFont.deriveFont(Font.BOLD));
        usersTable.getTableHeader().setReorderingAllowed(false);
        usersTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        usersTable.setShowGrid(false);
        usersTable.setIntercellSpacing(new Dimension(0, 0));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < usersTable.getColumnCount(); i++) {
            usersTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        usersTable.getColumnModel().getColumn(0).setPreferredWidth(180);
        usersTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        usersTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        usersTable.getColumnModel().getColumn(3).setPreferredWidth(200);

        sessionsPanel.add(new JScrollPane(usersTable), BorderLayout.CENTER);

        JButton kickUserBtn = new JButton("Kick Selected Session");
        styleButton(kickUserBtn, new Color(255, 153, 51));
        sessionsPanel.add(kickUserBtn, BorderLayout.SOUTH);

        centerColumn.add(sessionsPanel, BorderLayout.CENTER);
        contentPanel.add(centerColumn, BorderLayout.CENTER);

        JPanel rightColumn = new JPanel(new GridLayout(2, 1, 8, 8));
        rightColumn.setOpaque(false);
        rightColumn.setPreferredSize(new Dimension(320, 0));

        JPanel activeRoomsPanel = new JPanel(new BorderLayout(6, 6));
        activeRoomsPanel.setBackground(Color.WHITE);
        activeRoomsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210)),
                BorderFactory.createTitledBorder("Active Rooms")
        ));
        activeRoomsModel = new DefaultListModel<>();
        activeRoomsList = new JList<>(activeRoomsModel);
        activeRoomsList.setFont(defaultFont);
        activeRoomsPanel.add(new JScrollPane(activeRoomsList), BorderLayout.CENTER);

        JPanel roomBtnPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        roomBtnPanel.setOpaque(false);

        createRoomBtn = new JButton("Create Room");
        styleButton(createRoomBtn, new Color(65, 150, 65));

        deleteRoomBtn = new JButton("Delete Room");
        styleButton(deleteRoomBtn, new Color(220, 80, 60));

        roomBtnPanel.add(createRoomBtn);
        roomBtnPanel.add(deleteRoomBtn);

        activeRoomsPanel.add(roomBtnPanel, BorderLayout.SOUTH);

        JPanel statsPanel = new JPanel(new BorderLayout(6, 6));
        statsPanel.setBackground(Color.WHITE);
        statsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210)),
                BorderFactory.createTitledBorder("Mailbox Statistics")
        ));

        statsTable = new JTable(new DefaultTableModel(
                new String[]{"Username", "Sent", "Inbox", "Total", "Bytes"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        statsTable.setRowHeight(24);
        statsTable.setFont(defaultFont.deriveFont(12f));
        statsTable.getTableHeader().setFont(defaultFont.deriveFont(Font.BOLD, 12f));
        statsTable.setFillsViewportHeight(true);
        statsPanel.add(new JScrollPane(statsTable), BorderLayout.CENTER);

        JButton forceCleanupBtn = new JButton("Force Cleanup");
        styleButton(forceCleanupBtn, new Color(220, 80, 60));
        statsPanel.add(forceCleanupBtn, BorderLayout.SOUTH);

        rightColumn.add(activeRoomsPanel);
        rightColumn.add(statsPanel);
        contentPanel.add(rightColumn, BorderLayout.EAST);

        JPanel bottomPanel = new JPanel(new BorderLayout(8, 8));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210)),
                BorderFactory.createTitledBorder("System Logs & Broadcast")
        ));
        bottomPanel.setPreferredSize(new Dimension(0, 250));

        JPanel bottomHeader = new JPanel(new GridLayout(2, 1, 6, 6));
        bottomHeader.setOpaque(false);

        JPanel broadcastRow = new JPanel(new BorderLayout(8, 0));
        broadcastRow.setOpaque(false);

        JLabel broadcastLabel = new JLabel("Broadcast Message:");
        broadcastLabel.setFont(defaultFont.deriveFont(Font.BOLD));

        broadcastField = new JTextField();
        styleTextField(broadcastField);

        JButton broadcastBtn = new JButton("Send Broadcast");
        styleButton(broadcastBtn, new Color(138, 92, 246));

        broadcastRow.add(broadcastLabel, BorderLayout.WEST);
        broadcastRow.add(broadcastField, BorderLayout.CENTER);
        broadcastRow.add(broadcastBtn, BorderLayout.EAST);

        JPanel logsActionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        logsActionRow.setOpaque(false);

        JLabel filterLabel = new JLabel("Filter:");
        filterLabel.setFont(defaultFont.deriveFont(Font.BOLD));

        logFilterCombo = new JComboBox<>(new String[]{"ALL", "INFO", "ERROR", "AUTH", "LIST"});
        logFilterCombo.setFont(defaultFont);

        JButton applyFilterBtn = new JButton("Apply Filter");
        styleButton(applyFilterBtn, new Color(64, 158, 255));

        clearLogsBtn = new JButton("Clear Logs");
        styleButton(clearLogsBtn, new Color(108, 117, 125));

        JButton exportLogsBtn = new JButton("Save Logs to TXT");
        styleButton(exportLogsBtn, new Color(32, 201, 151));

        logsActionRow.add(filterLabel);
        logsActionRow.add(logFilterCombo);
        logsActionRow.add(applyFilterBtn);
        logsActionRow.add(clearLogsBtn);
        logsActionRow.add(exportLogsBtn);

        bottomHeader.add(broadcastRow);
        bottomHeader.add(logsActionRow);

        logsArea = new JTextArea();
        logsArea.setEditable(false);
        logsArea.setBackground(new Color(248, 248, 248));
        logsArea.setForeground(new Color(40, 40, 40));
        logsArea.setCaretColor(Color.BLACK);
        logsArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        logsArea.setMargin(new Insets(8, 10, 8, 10));

        bottomPanel.add(bottomHeader, BorderLayout.NORTH);
        bottomPanel.add(new JScrollPane(logsArea), BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        addUserBtn.addActionListener(e -> {
            String u = usernameField.getText().trim();
            String p = new String(passwordField.getPassword()).trim();

            if (u.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter a username.");
                return;
            }

            if (p.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter a password.");
                return;
            }

            boolean ok = UserStore.addUser(u, p);
            if (ok) {
                ServerLog.info("USER_ADDED username=" + u);
                usernameField.setText("");
                passwordField.setText("");

                refreshRegisteredUsersOnly();
                refreshUsers();

                JOptionPane.showMessageDialog(this, "User added successfully.");
            } else {
                ServerLog.info("USER_ADD_REJECTED username=" + u + " reason=duplicate_or_invalid");
                JOptionPane.showMessageDialog(this, "Username already exists or invalid.");
            }
        });

        removeUserBtn.addActionListener(e -> {
            String u = registeredUsersList.getSelectedValue();
            if (u == null || u.isBlank()) {
                JOptionPane.showMessageDialog(this, "Select a username from Registered Users.");
                return;
            }

            boolean ok = TcpProtocolHandler.unregisterUser(u);
            if (ok) {
                ServerLog.info("USER_UNREGISTERED username=" + u);
                usernameField.setText("");
                passwordField.setText("");

                refreshRegisteredUsersOnly();
                refreshUsers();

                JOptionPane.showMessageDialog(this, "User removed successfully.");
            } else {
               // ServerLog.info("USER_UNREGISTER_REJECTED username=" + u + " reason=not_found");
                JOptionPane.showMessageDialog(this, "User not found.");
            }
        });

        resetPassBtn.addActionListener(e -> {
            String u = registeredUsersList.getSelectedValue();
            if (u == null || u.isBlank()) {
                JOptionPane.showMessageDialog(this, "Select a user from Registered Users.");
                return;
            }

            JPasswordField newPassField = new JPasswordField();
            styleTextField(newPassField);

            int result = JOptionPane.showConfirmDialog(
                    this,
                    newPassField,
                    "Enter new password for " + u,
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (result != JOptionPane.OK_OPTION) {
                return;
            }

            String p = new String(newPassField.getPassword()).trim();
            if (p.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Password cannot be empty.");
                return;
            }

            boolean ok = UserStore.updatePassword(u, p);
            if (ok) {
                ServerLog.info("PASSWORD_RESET username=" + u);
                JOptionPane.showMessageDialog(this, "Password updated successfully.");
            } else {
                ServerLog.info("PASSWORD_RESET_REJECTED username=" + u + " reason=user_not_found");
                JOptionPane.showMessageDialog(this, "User not found.");
            }
        });

        kickUserBtn.addActionListener(e -> {
            int row = usersTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Select a session from Active Sessions.");
                return;
            }

            String user = String.valueOf(usersTable.getValueAt(row, 0));
            if (user == null || user.isBlank()) {
                JOptionPane.showMessageDialog(this, "Invalid selected session.");
                return;
            }

            boolean ok = TcpProtocolHandler.kickUser(user);
            if (ok) {
                JOptionPane.showMessageDialog(this, "User kicked successfully.");
                refreshUsers();
            } else {
                JOptionPane.showMessageDialog(this, "User is not currently online.");
            }
        });

        broadcastBtn.addActionListener(e -> {
            String msg = broadcastField.getText().trim();
            if (msg.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter a broadcast message.");
                return;
            }

            TcpProtocolHandler.broadcastFromServer(msg);
            broadcastField.setText("");
            JOptionPane.showMessageDialog(this, "Broadcast sent.");
        });

        exportLogsBtn.addActionListener(e -> {
            try {
                String filteredLogs = getFilteredLogsText();
                if (filteredLogs.isBlank() || filteredLogs.startsWith("(No ")) {
                    JOptionPane.showMessageDialog(this, "No logs to export for the current filter.");
                    return;
                }

                JFileChooser chooser = new JFileChooser();
                chooser.setSelectedFile(new java.io.File(
                        "server-log-" + String.valueOf(logFilterCombo.getSelectedItem()).toLowerCase(Locale.ROOT)
                                + "-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date()) + ".txt"
                ));

                int result = chooser.showSaveDialog(this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    Files.writeString(
                            chooser.getSelectedFile().toPath(),
                            filteredLogs,
                            StandardCharsets.UTF_8
                    );
                    JOptionPane.showMessageDialog(this, "Filtered logs exported successfully.");
                }
            } catch (Exception ex) {
                ServerLog.error("EXPORT_LOGS", ex.toString());
                JOptionPane.showMessageDialog(this, "Failed to export logs.");
            }
        });

        clearLogsBtn.addActionListener(e -> {
            try {
                if (!Files.exists(LOG_FILE)) {
                    logsArea.setText("(No logs yet)");
                    JOptionPane.showMessageDialog(this, "No logs to clear.");
                    return;
                }

                String selected = String.valueOf(logFilterCombo.getSelectedItem());
                List<String> lines = Files.readAllLines(LOG_FILE);
                List<String> keptLines = new java.util.ArrayList<>();

                for (String line : lines) {
                    if (!matchesLogFilter(line, selected)) {
                        keptLines.add(line);
                    }
                }

                Files.write(LOG_FILE, keptLines, StandardCharsets.UTF_8);
                refreshLogs();
                JOptionPane.showMessageDialog(this, "Filtered logs cleared.");
            } catch (Exception ex) {
                ServerLog.error("CLEAR_LOGS", ex.toString());
                JOptionPane.showMessageDialog(this, "Error clearing filtered logs.");
            }
        });

        applyFilterBtn.addActionListener(e -> refreshLogs());

        forceCleanupBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "This will clear all mailbox statistics. Continue?",
                    "Force Cleanup",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            ServerStats.clearAll();
            refreshMailboxStats();
            ServerLog.info("MAILBOX_STATS_CLEARED");
            JOptionPane.showMessageDialog(this, "Mailbox statistics cleaned successfully.");
        });

        createRoomBtn.addActionListener(e -> {
            JTextField roomField = new JTextField();
            styleTextField(roomField);

            int result = JOptionPane.showConfirmDialog(
                    this,
                    roomField,
                    "Enter room name",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (result != JOptionPane.OK_OPTION) return;

            String room = roomField.getText().trim().toLowerCase(Locale.ROOT);
            if (room.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Room name cannot be empty.");
                return;
            }

            boolean ok = ChatService.createRoom(room);
            if (ok) {
                ServerLog.info("ROOM_CREATED room=" + room);
                refreshActiveRooms();
                JOptionPane.showMessageDialog(this, "Room created successfully.");
            } else {
                JOptionPane.showMessageDialog(this, "Room already exists or invalid.");
            }
        });
        deleteRoomBtn.addActionListener(e -> {
            String room = activeRoomsList.getSelectedValue();
            if (room == null || room.isBlank()) {
                JOptionPane.showMessageDialog(this, "Select a room first.");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Delete room: " + room + "?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm != JOptionPane.YES_OPTION) return;

            boolean ok = ChatService.deleteRoom(room);
            if (ok) {
                ServerLog.info("ROOM_DELETED room=" + room);
                refreshActiveRooms();
                JOptionPane.showMessageDialog(this, "Room deleted successfully.");
            } else {
                JOptionPane.showMessageDialog(this, "Could not delete room.");
            }
        });

        saveSettingsBtn.addActionListener(e -> {
            try {
                int maxMessageSize = Integer.parseInt(maxMessageSizeField.getText().trim());
                int udpPort = ServerConfig.getUdpPort();
                int cleanupDays = ServerConfig.getCleanupDays();

                ServerConfig.update(udpPort, maxMessageSize, cleanupDays);

                ServerLog.info("SETTINGS_UPDATED cleanupDays=" + cleanupDays
                        + " maxMessageSize=" + maxMessageSize
                        + " udpPort=" + udpPort);

                JOptionPane.showMessageDialog(this, "Settings applied successfully.");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid max message size.");
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage());
            } catch (Exception ex) {
                ServerLog.error("SETTINGS_SAVE", ex.toString());
                JOptionPane.showMessageDialog(this, "Failed to save settings.");
            }
        });

        autoRefresh();
    }

    private void autoRefresh() {
        Timer t = new Timer(true);
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                refreshUsers();
                refreshActiveRooms();
                refreshLogs();
                refreshMailboxStats();
                refreshUptime();
            }
        }, 0, 2000);
    }

    private void refreshRegisteredUsersOnly() {
        SwingUtilities.invokeLater(() -> {
            registeredUsersModel.clear();
            for (String user : UserStore.snapshot()) {
                registeredUsersModel.addElement(user);
            }
        });
    }
    private void refreshUsers() {
        SwingUtilities.invokeLater(() -> {
            DefaultTableModel model = (DefaultTableModel) usersTable.getModel();
            model.setRowCount(0);

            for (RosterService.Entry e : RosterService.snapshot()) {
                if ("offline".equalsIgnoreCase(e.status)) {
                    continue;
                }

                model.addRow(new Object[]{
                        e.username,
                        e.status,
                        (e.ip == null || e.ip.isBlank()) ? "-" : e.ip,
                        formatTimestamp(e.lastSeen)
                });
            }

            refreshRegisteredUsersOnly();

            usersTable.clearSelection();
            registeredUsersList.clearSelection();
        });
    }

    private void refreshActiveRooms() {
        SwingUtilities.invokeLater(() -> {
            activeRoomsModel.clear();
            try {
                for (String room : ChatService.listRooms()) {
                    activeRoomsModel.addElement(room);
                }
            } catch (Exception ignored) {
            }
        });
    }

    private String getFilteredLogsText() throws java.io.IOException {
        if (!Files.exists(LOG_FILE)) {
            return "(No logs yet)";
        }

        String selected = String.valueOf(logFilterCombo.getSelectedItem());
        List<String> lines = Files.readAllLines(LOG_FILE);
        StringBuilder sb = new StringBuilder();

        for (String line : lines) {
            if (matchesLogFilter(line, selected)) {
                sb.append(line).append("\n");
            }
        }

        return sb.length() == 0 ? "(No matching logs)" : sb.toString();
    }

    private boolean matchesLogFilter(String line, String selected) {
        return "ALL".equalsIgnoreCase(selected) || line.contains("[" + selected + "]");
    }

    private void refreshLogs() {
        SwingUtilities.invokeLater(() -> {
            try {
                logsArea.setText(getFilteredLogsText());
                logsArea.setCaretPosition(logsArea.getDocument().getLength());
            } catch (Exception e) {
                logsArea.setText("Error reading log file");
                e.printStackTrace();
            }
        });
    }

    private void refreshMailboxStats() {
        SwingUtilities.invokeLater(() -> {
            DefaultTableModel model = (DefaultTableModel) statsTable.getModel();
            model.setRowCount(0);

            for (ServerStats.UserStats s : ServerStats.snapshot()) {
                model.addRow(new Object[]{
                        s.username,
                        s.sentCount,
                        s.receivedCount,
                        s.getTotalMessages(),
                        s.trafficBytes
                });
            }
        });
    }

    private void refreshUptime() {
        SwingUtilities.invokeLater(() -> {
            long elapsed = (System.currentTimeMillis() - guiStartTime) / 1000L;
            long h = elapsed / 3600;
            long m = (elapsed % 3600) / 60;
            long s = elapsed % 60;
            uptimeLabel.setText(String.format("Uptime: %02d:%02d:%02d", h, m, s));
        });
    }

    private String formatTimestamp(long ts) {
        if (ts <= 0) return "-";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(ts));
    }

    private void styleButton(JButton button, Color bgColor) {
        button.setFont(defaultFont.deriveFont(Font.BOLD, 13));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 0, 0, 25)),
                BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            final Color originalBg = bgColor;
            final Color hoverBg = new Color(
                    Math.max(0, bgColor.getRed() - 20),
                    Math.max(0, bgColor.getGreen() - 20),
                    Math.max(0, bgColor.getBlue() - 20)
            );

            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(hoverBg);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(originalBg);
            }
        });
    }

    private void styleTextField(JTextField field) {
        field.setFont(defaultFont);
        field.setPreferredSize(new Dimension(0, 30));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(0, 8, 0, 8)
        ));
    }

    private static void setUIFont(javax.swing.plaf.FontUIResource f) {
        java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof javax.swing.plaf.FontUIResource) {
                UIManager.put(key, f);
            }
        }
    }
}