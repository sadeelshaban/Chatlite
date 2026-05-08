package com.ChatLite.server;

import java.util.*;

public class Protocol {

    public static Map<String,String> parseKeyVals(String line) {
        Map<String,String> map = new HashMap<>();
        String[] parts = line.split(" ");
        for (String p : parts) {
            if (p.contains(":")) {
                String[] kv = p.split(":", 2);
                map.put(kv[0].trim().toUpperCase(), kv[1].trim());
            }
        }
        return map;
    }
}
