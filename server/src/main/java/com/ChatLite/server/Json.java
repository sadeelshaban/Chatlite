package com.ChatLite.server;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.lang.reflect.Type;
import java.util.List;

public class Json {

    private static final Gson gson = new Gson();

    public static <T> List<T> readList(BufferedReader br, Class<T> cls) {
        Type type = TypeToken.getParameterized(List.class, cls).getType();
        return gson.fromJson(br, type);
    }

    public static <T> void writeList(BufferedWriter bw, List<T> list) {
        gson.toJson(list, bw);
    }
}
