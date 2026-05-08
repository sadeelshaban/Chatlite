package com.ChatLite.client;

import java.awt.*;
import java.awt.event.AWTEventListener;
import java.io.IOException;
import java.util.concurrent.*;

public class IdleDetector {
    private final ChatClientAPI api;
    private final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
    private volatile long lastActivity = System.currentTimeMillis();
    private volatile String current = "ACTIVE";
    private final long idleMs;

    public IdleDetector(ChatClientAPI api, long idleMs) {
        this.api = api;
        this.idleMs = idleMs;
    }

    public void start() {
        Toolkit.getDefaultToolkit().addAWTEventListener(activityListener(),
                AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        ses.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
    }

    private AWTEventListener activityListener() {
        return e -> {
            lastActivity = System.currentTimeMillis();
            if (!"ACTIVE".equals(current)) {
                try { api.setStatus("ACTIVE"); current = "ACTIVE"; } catch (IOException ignored) {}
            }
        };
    }

    private void tick() {
        if (System.currentTimeMillis() - lastActivity >= idleMs && !"AWAY".equals(current)) {
            try { api.setStatus("AWAY"); current = "AWAY"; } catch (IOException ignored) {}
        }
    }

    public void setBusy() {
        try { api.setStatus("BUSY"); current="BUSY"; } catch (IOException ignored) {}
    }

    public void shutdown(){ ses.shutdownNow(); }
}