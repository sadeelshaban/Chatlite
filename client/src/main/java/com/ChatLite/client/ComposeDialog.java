package com.ChatLite.client;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;

public class ComposeDialog extends JDialog {
    private final JTextField toField = new JTextField();
    private final JTextField subjField = new JTextField();
    private final JTextArea bodyArea = new JTextArea();
    private final JButton sendBtn = new JButton("Send");
    private final JButton cancelBtn = new JButton("Cancel");

    public ComposeDialog(Frame parent, ChatClientAPI api, String from, Runnable onSent) {
        super(parent, "Compose Mail", true);
        setSize(450, 350);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new GridLayout(2, 2, 5, 5));
        top.add(new JLabel("To:"));
        top.add(toField);
        top.add(new JLabel("Subject:"));
        top.add(subjField);
        add(top, BorderLayout.NORTH);

        bodyArea.setLineWrap(true);
        bodyArea.setWrapStyleWord(true);
        add(new JScrollPane(bodyArea), BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.add(sendBtn);
        bottom.add(cancelBtn);
        add(bottom, BorderLayout.SOUTH);

        sendBtn.addActionListener(e -> {
            String to = toField.getText().trim();
            String subj = subjField.getText().trim();
            String body = bodyArea.getText();
            if (to.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Recipient required!");
                return;
            }
            try {
                long id = api.send(from, List.of(to), subj, body);
                JOptionPane.showMessageDialog(this, "Message sent (ID " + id + ")");
                if (onSent != null) onSent.run();
                dispose();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Send failed: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelBtn.addActionListener(e -> dispose());
    }
}
