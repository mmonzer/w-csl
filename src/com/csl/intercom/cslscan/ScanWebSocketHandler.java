package com.csl.intercom.cslscan;

import com.csl.core.CSLContext;
import com.csl.intercom.dbapi.DbapiHandler;
import com.csl.intercom.dbapi.ScanEntity;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import main.services.DiscoveryServices;
import main.services.JsonApiResponse;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.DefaultManagedTaskScheduler;
import org.springframework.util.MimeType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;


/**
 * Handle the WebSocket connections with CSL-Scan.
 */
public class ScanWebSocketHandler {
    private final DiscoveryServices discoveryService;
    private final String websocketNotificationsEndpoint = "/discovery/ready";
    private final String websocketStartDiscoveryEndpoint = "/discovery/start";
    private ZoneId zoneId;
    private final Queue<List<String>> scanRequestsQueue = new ConcurrentLinkedQueue<>();
    private String scanManagerDiscoveryUrl;
    private ScheduledExecutorService webSocketsConnectionAttempts;
    private StompSession stompRequestsSession = null;
    private StompSession stompNotificationSession = null;
    private static final DbapiHandler dbapiHandler = new DbapiHandler();
    private List<ScanEntity> scans = new ArrayList<>();
    private static final List<String> finishedScanStatus = new ArrayList<>(5) {{
        add("READY_CHANGES");
        add("READY_NO_CHANGES");
        add("DISCARDED");
        add("PARTIAL_ERROR");
        add("ERROR");
    }};
    private static final List<String> successScanStatus = new ArrayList<>(2) {{
        add("READY_CHANGES");
        add("READY_NO_CHANGES");
    }};

    /**
     * Create a new manager with the correct URL.
     *
     * @param discoveryService        The parent {@link DiscoveryServices}, used to handle the necessary
     * @param scanManagerDiscoveryUrl The URL of CSL-Scan.
     */
    public ScanWebSocketHandler(DiscoveryServices discoveryService, String scanManagerDiscoveryUrl) {
        this.discoveryService = discoveryService;
        this.scanManagerDiscoveryUrl = scanManagerDiscoveryUrl;

        // Schedule reconnection to websockets every 2 seconds
        webSocketsConnectionAttempts = Executors.newScheduledThreadPool(1);
        webSocketsConnectionAttempts.scheduleAtFixedRate(
                this::connectStompSessionsIfNecessary,
                0,
                2,
                TimeUnit.SECONDS);
        Json config = CSLContext.instance.getConfig();
        zoneId = ZoneId.of(JsonUtil.getStringFromJson(config.get("global"), "timezone", "Europe/Paris"));
    }

    /**
     * Stop the connections with CSL-Scan
     */
    public void stop() {
        webSocketsConnectionAttempts.shutdown();
        stompRequestsSession.disconnect();
        stompNotificationSession.disconnect();
    }

    /**
     * Get the status of the websocket handling :
     * - the request websocket (connected of not)
     * - the notifications websocket
     * - the number of scans in the queue
     *
     * @return a {@link Json} with the status information.
     */
    public Json getStatus() {
        Json status = Json.object();

        status.set("is_requests_websocket_connected", stompRequestsSession != null && stompRequestsSession.isConnected());
        status.set("is_notifications_websocket_connected", stompNotificationSession != null && stompNotificationSession.isConnected());
        status.set("scan_requests_queue", scanRequestsQueue.size());

        return status;
    }

    /**
     * Start scanning all registered entities
     *
     * @return An id identifying the scan for further notice.
     */
    public JsonApiResponse requestScan(List<String> entities) {
        if (stompRequestsSession == null || !stompRequestsSession.isConnected()) {
            scanRequestsQueue.add(entities);
            return JsonApiResponse.error("Scan service unavailable, added scan request to queue");
        } else {
            startScan(entities);
            return JsonApiResponse.success();
        }
    }

    /**
     * Actually start the scan, without checking the validity of the session.
     *
     * @param uuids A list of entities to scan. May be null, resulting in a scan of all entities.
     */
    private void startScan(List<String> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            stompRequestsSession.send(websocketStartDiscoveryEndpoint, "");
        } else {
            stompRequestsSession.send(websocketStartDiscoveryEndpoint, Json.array(uuids.toArray()));
        }
        LocalDateTime startDate = LocalDateTime.now();
        int dbapiId = dbapiHandler.notifyScanStarted(startDate);
        scans.add(ScanEntity.fromDbapiId(dbapiId, startDate));
    }

    /**
     * Check if the websockets are connected, and tries to connect if not.
     * Blocking, should be called asynchronously.
     */
    private void connectStompSessionsIfNecessary() {
        if (stompNotificationSession == null || !stompNotificationSession.isConnected()) {
            try {
                stompNotificationSession = subscribeToNotifications();
            } catch (InterruptedException | ExecutionException | ResourceAccessException e) {
                stompNotificationSession = null;
            } catch (Throwable e) {
                System.out.println("Caught");
                stompNotificationSession = null;
            }
        }

        if (stompRequestsSession == null || !stompRequestsSession.isConnected()) {
            try {
                stompRequestsSession = connectToRequestsWebSocket();
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                stompRequestsSession = null;
            }
        }
    }

    /**
     * Connect the notification socket to CSL-Scan, and define the necessary callbacks (after connection and on message reception).
     * Blocks so should be called asynchronously.
     *
     * @return The session we just created.
     * @throws ExecutionException   if connection failed.
     * @throws InterruptedException if connection was interrupted.
     */
    private StompSession subscribeToNotifications() throws ExecutionException, InterruptedException, TimeoutException {
        WebSocketStompClient stompClient = createStompClient();

        // Define the callbacks and return the future when it is realized.
        return stompClient.connect(this.scanManagerDiscoveryUrl, new StompSessionHandlerAdapter() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return super.getPayloadType(headers);
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payloadRaw) {
                super.handleFrame(headers, payloadRaw);
                Json payload = (Json) payloadRaw;

                String scanId = JsonUtil.getStringFromJson(payload, "uuid", null);
                ScanEntity scan = getScanByScanId(scanId);
                if (scan == null) {
                    try {
                        scan = getFirstScanWithoutScanId();
                        scan.setScanId(scanId);
                    } catch (NullPointerException e) {
                        LocalDateTime startDate = scanToLocalTime(LocalDateTime.parse(JsonUtil.getStringFromJson(payload, "createdAt", null)));
                        if (startDate == null) {
                            startDate = LocalDateTime.now();
                        }
                        int dbapiId = dbapiHandler.notifyScanStarted(startDate);
                        scan = ScanEntity.fromDbapiId(dbapiId, startDate);
                        scan.setScanId(scanId);
                        scans.add(scan);
                    }
                }

                if (payload != null) {
                    System.out.println("[STOMP " + LocalDateTime.now() + "] " + payload.toString());
                } else {
                    System.out.println("[STOMP] null");
                }
                String scanStatus = JsonUtil.getStringFromJson(payload, "status", "NONE");
                if (finishedScanStatus.contains(scanStatus)) {
                    try {
                        // Put the end date in the scan information and notify DB-API the scan ended.
                        LocalDateTime endDate = LocalDateTime.now();
                        if (successScanStatus.contains(scanStatus)) {
                            scan.setFinished(true, endDate);
                        } else {
                            scan.setFinishedFail(payload.get("entitiesInError").toString(), endDate);
                        }
                        dbapiHandler.notifyScanFinished(scan);
                        discoveryService.handleCpeItemChanges();

                        // Check if we discovered new CPE items in the scan and if not, notify DB-API there was no new item.
                        List<CpeItem> newCpeItems = discoveryService.getCpeItemChangesSince(scan.getStartDate());
                        if (newCpeItems != null && newCpeItems.isEmpty()) {
                            dbapiHandler.notifyNoNewCpe();
                        }
                        dbapiHandler.notifySynchronisationEnded(scan);
                        scans.remove(scan);
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                    }
                } else {
                    dbapiHandler.setLastScanProgress(JsonUtil.getDoubleFromJson(payload, "completion", 0.0));
                    discoveryService.handleCpeItemChanges();
                }
            }

            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                super.afterConnected(session, connectedHeaders);
                session.subscribe(websocketNotificationsEndpoint, this);
            }
        }).get(1000, TimeUnit.MILLISECONDS);
    }

    /**
     * Connect the requests socket to CSL-Scan, and define the necessary callbacks (after connection and on message reception).
     * Blocks so should be called asynchronously.
     *
     * @return The session we just created.
     * @throws ExecutionException   if connection failed.
     * @throws InterruptedException if connection was interrupted.
     */
    private StompSession connectToRequestsWebSocket() throws ExecutionException, InterruptedException, TimeoutException {

        WebSocketStompClient requestStompClient = createStompClient();
        // Define the callbacks and return the future when it is realized.
        return requestStompClient.connect(this.scanManagerDiscoveryUrl, new StompSessionHandlerAdapter() {
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                super.handleFrame(headers, payload);
                if (payload != null) {
                    System.out.println("[STOMP " + LocalDateTime.now() + "] " + payload.toString());
                    // handle response
                } else {
                    System.out.println("[STOMP] null");
                }
            }

            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                super.afterConnected(session, connectedHeaders);
                purgeScanRequestsQueue();
            }
        }).get(1, TimeUnit.SECONDS);
    }

    /**
     * Create the STOMP client with our custom message interpreter.
     *
     * @return a {@link WebSocketStompClient} suitable for our needs.
     */
    private WebSocketStompClient createStompClient() {
        WebSocketClient client = new StandardWebSocketClient();
        SockJsClient sockJsClient = new SockJsClient(new ArrayList<>(Arrays.asList(new WebSocketTransport(client))));

        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new JsonMessageConverter());
        stompClient.setTaskScheduler(new DefaultManagedTaskScheduler());

        return stompClient;
    }

    /**
     * Try to execute all the scan requests in the queue.
     */
    private void purgeScanRequestsQueue() {
        List<String> scanRequest;
        while ((scanRequest = scanRequestsQueue.poll()) != null) {
            if (stompRequestsSession == null || !stompRequestsSession.isConnected()) {
                scanRequestsQueue.add(scanRequest);
                break;
            }
            startScan(scanRequest);
            if (stompRequestsSession == null || !stompRequestsSession.isConnected()) {
                scanRequestsQueue.add(scanRequest);
                break;
            }
        }
    }

    /**
     * Custom message converter to serialize and deserialize {@link Json} objects.
     */
    private class JsonMessageConverter extends AbstractMessageConverter {
        public JsonMessageConverter() {
            super(new MimeType("application", "json"));
        }

        @Override
        protected boolean supports(Class<?> clazz) {
            return String.class.isAssignableFrom(clazz)
                    || clazz.isArray() && String.class.isAssignableFrom(clazz.getComponentType())
                    || Json.class.isAssignableFrom(clazz)
                    ;
        }

        @Override
        protected Object convertToInternal(Object payload, MessageHeaders headers, Object conversionHint) {
            if (payload instanceof Json) {
                return ((Json) payload).toString().getBytes();
            } else if (payload instanceof String) {
                return ((String) payload).getBytes();
            } else if (payload instanceof byte[]) {
                return payload;
            } else if (payload instanceof String[]) {
                Json result = Json.array();
                for (String x : (String[]) payload) {
                    result.add(x);
                }
                return result.toString().getBytes();
            } else {
                throw new RuntimeException("Bad type");
            }
        }

        @Override
        protected Json convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
            Object payload = message.getPayload();
            if (payload instanceof String) {
                return Json.read((String) payload);
            } else if (payload instanceof byte[]) {
                return Json.read(new String((byte[]) payload));
            } else {
                throw new RuntimeException("Bad conversion");
            }
        }
    }

    /**
     * Get the first scan matching a predicate in the <code>scans</code> list.
     * @param predicate The predicate that a scan has to match.
     * @return The first scan that matched the condition, or null.
     */
    private ScanEntity searchScan(Json.Function<ScanEntity, Boolean> predicate) {
        for (ScanEntity scan : scans) {
            if (predicate.apply(scan)) {
                return scan;
            }
        }
        return null;
    }

    /**
     * Get a scan from it DB-API id.
     * @param dbapiId The id to seek.
     * @return The scan with this id, or null if none is found.
     */
    private ScanEntity getScanByDbapiId(int dbapiId) {
        return searchScan(scanEntity -> scanEntity.getDbapiId() == dbapiId);
    }

    /**
     * Get a scan from it CSL-Scan id.
     * @param scanId The id to seek.
     * @return The scan with this id, or null if none is found.
     */
    private ScanEntity getScanByScanId(String scanId) {
        return searchScan(scanEntity -> scanId.equals(scanEntity.getScanId()));
    }

    /**
     * Get the first scan in the <code>scans</code> list that has no CSL-Scan id.
     * @return
     */
    private ScanEntity getFirstScanWithoutScanId() {
        return searchScan(scanEntity -> scanEntity.getScanId() == null);
    }

    /**
     * Translate UTC time, as used bu CSL-Scan, to local time.
     *
     * @param scanTime The {@link LocalDateTime} to convert.
     * @return The {@link LocalDateTime} corresponding to utcTime.
     */
    private LocalDateTime scanToLocalTime(LocalDateTime scanTime) {
        if (scanTime == null) return null;
        return scanTime.atOffset(ZoneOffset.UTC).atZoneSameInstant(zoneId).toLocalDateTime();
    }
}
