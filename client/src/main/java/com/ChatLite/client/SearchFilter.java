package com.ChatLite.client;

import java.util.ArrayList;
import java.util.List;

public class SearchFilter {
    public static class Row {
        public final long id; public final String from; public final int size; public final long ts; public final String subj;
        public Row(long id, String from, int size, long ts, String subj){ this.id=id; this.from=from; this.size=size; this.ts=ts; this.subj=subj; }
    }

    public static List<Row> parse(List<String> listLines){
        List<Row> out = new ArrayList<>();
        for (String s : listLines) {
            String[] t = s.split(" ");
            if (t.length >= 4) {
                long id = Long.parseLong(t[0]);
                String from = t[1];
                int size = Integer.parseInt(t[2]);
                long ts = Long.parseLong(t[3]);
                out.add(new Row(id, from, size, ts, ""));
            }
        }
        return out;
    }

    public static List<Row> filter(List<Row> rows, String q) {
        if (q == null || q.isBlank()) return rows;
        q = q.toLowerCase();
        List<Row> out = new ArrayList<>();
        for (Row r : rows) {
            if ((r.from != null && r.from.toLowerCase().contains(q)) ||
                    (r.subj != null && r.subj.toLowerCase().contains(q))) {
                out.add(r);
            }
        }
        return out;
    }
}
