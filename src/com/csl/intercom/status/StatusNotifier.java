package com.csl.intercom.status;

import com.csl.web.websockets.CSLWebSocket;
import com.ucsl.json.Json;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StatusNotifier implements AutoCloseable{
    private Map<String, IStatusProvider> providers = new HashMap<>();
    private boolean sendNotifications;
    private ScheduledExecutorService sender;
    private static final String ticType = "status";

    public StatusNotifier(boolean sendNotifications) {
        this.sendNotifications = sendNotifications;
        sender = Executors.newScheduledThreadPool(1);
        sender.scheduleAtFixedRate(() -> {
                    if (this.sendNotifications) {
                        sendNotification(buildNotification());
                    }
                },
                0,
                1,
                TimeUnit.SECONDS);
    }

    public StatusNotifier registerStatusProvider(String name, IStatusProvider provider) {
        providers.put(name, provider);
        return this;
    }

    public Json getNotification() {
        return buildNotification().get("line");
    }

    private Json buildNotification() {
        Json notification = Json.object();
        for (Map.Entry<String, IStatusProvider> provider : providers.entrySet()) {
            if (provider.getValue() != null){
                notification.set(provider.getKey(), provider.getValue().getStatus());
            }
        }
        return Json.object("line", notification, "type", ticType);
//        return Json.object("line", notification, "type", "tick_ids");
    }

    private void sendNotification(Json notification) {
        CSLWebSocket.broadcastMessageJson(CSLWebSocket.WEB_SOCKET_CONSOLE, notification);
    }

    public void setSendNotifications(boolean sendNotifications) {
        this.sendNotifications = sendNotifications;
    }

    public void close() {
        sender.shutdown();
    }
}
