package com.csl.intercom.status;

import com.csl.core.Config;
import com.csl.logger.LoggerInterfaces;
import com.csl.util.ThreadUtils;
import com.csl.web.websockets.CSLWebSocket;
import com.ucsl.json.Json;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Handle the status notifications,
 * collects them from registered status providers and, if necessary, sends them on the server's WebSocket.
 */
public class StatusNotifier implements AutoCloseable {
    private final Map<String, IStatusProvider> providers = new HashMap<>();
    /**
     * -- SETTER --
     *  Tell whether we should periodically send notifications on the WebSocket.
     *
     * @param sendNotifications true if we should send notification, false if we shouldn't.
     */
    @Setter
    private boolean sendNotifications;
    private final ScheduledExecutorService sender;
    private static final String ticType = "status";

    /**
     * Create a {@link StatusNotifier}.
     * @param sendNotifications true if we should send notifications periodically, false if we shouldn't.
     * @param notificationsPeriod The delay between two notifications, in seconds.
     */
    public StatusNotifier(boolean sendNotifications, int notificationsPeriod) {
        this.sendNotifications = sendNotifications;
        sender = Executors.newScheduledThreadPool(1);
        ThreadUtils.uncorrelatedSingleThreadScheduledAtFixedRate(
                sender,
                () -> {
                    if (this.sendNotifications) {
                        sendNotification(buildNotification());
                    }
                },
                0,
                notificationsPeriod,
                TimeUnit.SECONDS,
                "status_notifier",
                LoggerInterfaces.CSL_CLIENT);
    }

    /**
     * Create a {@link StatusNotifier} with the delay specified in the configuration file.
     * @param sendNotifications true if we should send notifications periodically, false if we shouldn't.
     */
    public StatusNotifier(boolean sendNotifications) {
        this(sendNotifications, Config.instance.Status.getNotificationsPeriod());
    }

    /**
     * Register a status provider to send status information.
     *
     * @param name The name of the provider, as will be displayed in the notification. Should be unique, reusing a name results in overwriting older value.
     * @param provider The status information provider.
     * @return this.
     */
    public StatusNotifier registerStatusProvider(String name, IStatusProvider provider) {
        providers.put(name, provider);
        return this;
    }

    /**
     * Create a notification.
     * @return The created notification's contents, for display purposes only.
     */
    public Json getNotification() {
        return buildNotification().get("line");
    }

    /**
     * Create a notification.
     *
     * @return The newly created notification, ready to be sent on the WebSocket.
     */
    private Json buildNotification() {
        Json notification = Json.object();
        for (Map.Entry<String, IStatusProvider> provider : providers.entrySet()) {
            if (provider.getValue() != null){
                notification.set(provider.getKey(), provider.getValue().getStatus());
            }
        }
        return Json.object("line", notification, "type", ticType);
    }

    /**
     * Send a notification on the server's WebSocket.
     * @param notification The notification to send.
     */
    private void sendNotification(Json notification) {
        CSLWebSocket.broadcastMessageJson(CSLWebSocket.WEB_SOCKET_CONSOLE, notification);
    }

    public void close() {
        sender.shutdown();
    }
}
