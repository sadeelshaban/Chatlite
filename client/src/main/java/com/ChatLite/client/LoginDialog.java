package com.ChatLite.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginDialog extends JDialog {

    public static class LoginData {
        public final String username;
        public final String password;
        public final String host;
        public final int port;

        public LoginData(String username, String password, String host, int port) {
            this.username = username;
            this.password = password;
            this.host = host;
            this.port = port;
        }
    }

    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JTextField hostField = new JTextField(18);
    private final JTextField portField = new JTextField(18);

    private LoginData result = null;

    public LoginDialog(Frame owner, String defaultUsername, String defaultHost, int defaultPort) {
        super(owner, "ChatLite Login", true);

        setLayout(new BorderLayout(10, 10));
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        usernameField.setText(defaultUsername == null ? "" : defaultUsername);
        hostField.setText(defaultHost == null ? "127.0.0.1" : defaultHost);
        portField.setText(String.valueOf(defaultPort));

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        panel.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        panel.add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Server IP:"), gbc);
        gbc.gridx = 1;
        panel.add(hostField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 1;
        panel.add(portField, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton loginBtn = new JButton("Login");
        JButton cancelBtn = new JButton("Cancel");
        buttons.add(loginBtn);
        buttons.add(cancelBtn);

        add(panel, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        loginBtn.addActionListener(e -> onLogin());
        cancelBtn.addActionListener(e -> {
            result = null;
            dispose();
        });

        getRootPane().setDefaultButton(loginBtn);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void onLogin() {
        String username = usernameField.getText().trim().toLowerCase();
        String password = new String(passwordField.getPassword()).trim();
        String host = hostField.getText().trim();

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username is required.");
            return;
        }

        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Password is required.");
            return;
        }

        if (host.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Server IP is required.");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
            if (port <= 0 || port > 65535) {
                JOptionPane.showMessageDialog(this, "Port must be between 1 and 65535.");
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Port must be a valid number.");
            return;
        }

        result = new LoginData(username, password, host, port);
        dispose();
    }

    public static LoginData showDialog(Frame owner, String defaultUsername, String defaultHost, int defaultPort) {
        LoginDialog dialog = new LoginDialog(owner, defaultUsername, defaultHost, defaultPort);
        dialog.setVisible(true);
        return dialog.result;
    }
}