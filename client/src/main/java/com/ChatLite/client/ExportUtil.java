package com.ChatLite.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ExportUtil {

    public static void exportConversation(String me,
                                          String peer,
                                          List<ChatClientAPI.Retrieved> messages,
                                          File outFile) throws Exception {
        try (Writer w = new OutputStreamWriter(
                new FileOutputStream(outFile), StandardCharsets.UTF_8)) {

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            w.write("Conversation between " + me + " and " + peer + "\n\n");

            for (ChatClientAPI.Retrieved m : messages) {
                String from = extract(m.headers, "FROM:");
                String to = extract(m.headers, "TO:");
                String subj = extract(m.headers, "SUBJ:");

                boolean mineToPeer = from.equalsIgnoreCase(me) && containsUser(to, peer);
                boolean peerToMe   = from.equalsIgnoreCase(peer) && containsUser(to, me);
                if (!(mineToPeer || peerToMe)) continue;

                String ts = sdf.format(new Date()); // لو بدك توقيت الرسالة الحقيقي، خزّنه عند الـRETR
                w.write("[" + ts + "] " + from + " -> " + to + " | " + subj + "\n");
                w.write(m.body == null ? "" : m.body);
                w.write("\n\n");
            }
        }
    }

    private static boolean containsUser(String csv, String user) {
        if (csv == null) return false;
        String[] parts = csv.split(",");
        for (String p : parts) {
            if (p.trim().equalsIgnoreCase(user)) return true;
        }
        return false;
    }

    private static String extract(String headersLine, String key) {
        if (headersLine == null) return "";
        String[] toks = headersLine.split("\\s+");
        for (String t : toks) {
            if (t.startsWith(key)) return t.substring(key.length());
        }
        return "";
    }
}
