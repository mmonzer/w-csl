package com.csl.intercom.cslscan;

import com.csl.intercom.cslscan.models.scans.ExternalScan;
import com.csl.intercom.services.ScansService;
import com.csl.intercom.dbapi.DbapiHandler;
import com.csl.intercom.dbapi.models.ScanEntity;
import com.csl.intercom.dbapi.models.ScansList;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import main.services.DiscoveryServices;
import main.services.JsonApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;


/**
 * Handle the WebSocket connections with CSL-Scan.
 */
public class ScanWebSocketHandler {
    static private final Logger logger = LoggerFactory.getLogger(ScanWebSocketHandler.class);
    private final DiscoveryServices discoveryService;
    private final String websocketNotificationsEndpoint = "/discovery/ready";
    private final String websocketStartDiscoveryEndpoint = "/discovery/start";
    private final String websocketExternalScanEndpoint = "/external_discovery/ready";
    private final Queue<List<String>> scanRequestsQueue = new ConcurrentLinkedQueue<>();
    private String scanManagerDiscoveryUrl;
    private ScheduledExecutorService webSocketsConnectionAttempts;
    private StompSession stompRequestsSession = null;
    private StompSession stompNotificationSession = null;
    private StompSession stompExternalScanSession = null;
    private static final DbapiHandler dbapiHandler = new DbapiHandler();
    private ScanApiHandler scanApiHandler = new ScanApiHandler();
    private ScansService scansService;
    private ScansList scansList = ScansList.instance;


    /**
     * Create a new manager with the correct URL.
     *
     * @param discoveryService        The parent {@link DiscoveryServices}, used to handle the necessary
     * @param scanManagerDiscoveryUrl The URL of CSL-Scan.
     */
    public ScanWebSocketHandler(DiscoveryServices discoveryService, String scanManagerDiscoveryUrl, ScansService scansService) {
        this.discoveryService = discoveryService;
        this.scanManagerDiscoveryUrl = scanManagerDiscoveryUrl;
        this.scansService = scansService;

        // Schedule reconnection to websockets every 2 seconds
        webSocketsConnectionAttempts = Executors.newScheduledThreadPool(1);
        webSocketsConnectionAttempts.scheduleAtFixedRate(
                this::connectStompSessionsIfNecessary,
                0,
                2,
                TimeUnit.SECONDS);
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
     * Start scanning all registered entities.
     * Note: the notifications will be handled by the callback {@code handleFrame()} defined in {@code subscribeToNotifications()}
     *
     * @return An id identifying the scan for further notice.
     */
    public JsonApiResponse requestScan(List<String> entities) {
        // Check if ws to csl-scan is already connected
        if (stompRequestsSession == null || !stompRequestsSession.isConnected()) {
            // not connected to csl-scan --> add this request to the queue
            scanRequestsQueue.add(entities);
            return JsonApiResponse.error("Scan service unavailable, added scan request to queue");
        } else {
            // request the startScan to the csl-scan
            startScan(entities);
            return JsonApiResponse.success();
        }
    }

    /**
     * Actually start the scan, without checking the validity of the session.
     * Note: the notifications will be handled by the callback {@code handleFrame()} defined in {@code subscribeToNotifications()}
     *
     * @param uuids A list of entities to scan. May be null, resulting in a scan of all entities.
     */
    private void startScan(List<String> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            stompRequestsSession.send(websocketStartDiscoveryEndpoint, "");
        } else {
            stompRequestsSession.send(websocketStartDiscoveryEndpoint, Json.array(uuids.toArray()));
        }
    }

    /**
     * Check if the websockets are connected, and tries to connect if not.
     * Blocking, should be called asynchronously.
     */
    private void connectStompSessionsIfNecessary() {
        boolean new_notification_connection = false;
        if (stompNotificationSession == null || !stompNotificationSession.isConnected()) {
            try {
                new_notification_connection = true;
                stompNotificationSession = subscribeToNotifications();
            } catch (InterruptedException | ExecutionException | ResourceAccessException e) {
                logger.warn("Error while connecting to notifications websocket", e);
                stompNotificationSession = null;
                new_notification_connection = false;
            } catch (Throwable e) {
                logger.error("Error while connecting to notifications websocket", e);
                stompNotificationSession = null;
                new_notification_connection = false;
            }
        }

        boolean new_request_connection = false;
        if (stompRequestsSession == null || !stompRequestsSession.isConnected()) {
            try {
                new_request_connection = true;
                logger.debug("Connecting to requests websocket");
                stompRequestsSession = connectToRequestsWebSocket();
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                stompRequestsSession = null;
                new_request_connection = false;
            }
        }

        boolean new_external_scan_connection = false;
        if (stompExternalScanSession == null || !stompExternalScanSession.isConnected()) {
            try {
                new_external_scan_connection = true;
                logger.debug("Connecting to external scans notifications websocket");
                stompExternalScanSession = connectToExternalScansNotificationsWebSocket();
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                stompExternalScanSession = null;
                new_external_scan_connection = false;
            }
        }

        if (new_notification_connection || new_request_connection || new_external_scan_connection) {
            scansService.handleConnectionEstablishedWithScanner();
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
            public void handleFrame(StompHeaders headers, Object payloadRaw) {
                super.handleFrame(headers, payloadRaw);
                Json payload = (Json) payloadRaw;

                //region Log the notification
                if (payload != null) {
                    logger.debug("[STOMP] " + payload.toString());
                } else {
                    logger.debug("[STOMP] null");
                }
                //endregion Log the notification

                String scanId = JsonUtil.getStringFromJson(payload, "uuid", null);

                //region Get or Create Scan Entity
                // Check if we already know the scan
                ScanEntity scan = scansList.getScanByScanId(scanId);

                if (scan == null) {
                    // If we did not already see the scan, create a new Scan Entity
                    OffsetDateTime startDate = ScanUtils.scanTimeToLocal(OffsetDateTime.parse(JsonUtil.getStringFromJson(payload, "createdAt", null)));
                    if (startDate == null) {
                        startDate = OffsetDateTime.now();
                    }
                    scan = ScanEntity.fromScanId(scanId, startDate);
                }
                //endregion Get or Create Scan Entity

                //region Update the scan's info (status, progress)
                String scanStatus = JsonUtil.getStringFromJson(payload, "status", "NONE");
                if (ScanConstants.finishedScanStatuses.contains(scanStatus)) {
                    // Put the end date in the scan information and notify DB-API the scan ended.
                    OffsetDateTime endDate = OffsetDateTime.now();
                    if (ScanConstants.successScanStatuses.contains(scanStatus)) {
                        scan.setFinishedSuccess(endDate);
                    } else if (scanStatus.equals("DISCARDED")) {
                        scan.setDiscarded(endDate);
                    } else {
                        scan.setFinishedFail(payload.get("entitiesInError").toString(), endDate);
                    }
                } else if ("PENDING".equals(scanStatus)) {
                    scan.setStatus(ScanEntity.Status.PENDING);
                } else {
                    double scanProgress = ScanUtils.getProgressFromScanNotification(payload);
                    scan.setProgress(scanProgress);
                }
                //endregion Update the scan's info (status, progress)

                scansList.createOrUpdate(scan);
            }

            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                logger.debug("Connected to notifications websocket");
                super.afterConnected(session, connectedHeaders);
                session.subscribe(websocketNotificationsEndpoint, this);
            }

            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                logger.warn("Transport Error", exception);
                super.handleException(session, command, headers, payload, exception);
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                logger.warn("Transport Error", exception);
                super.handleTransportError(session, exception);
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
                    logger.debug("[STOMP] " + payload.toString());
                    // handle response
                } else {
                    logger.debug("[STOMP] null");
                }
            }

            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                logger.debug("Connected to requests websocket");
                super.afterConnected(session, connectedHeaders);
                purgeScanRequestsQueue();
            }
        }).get(1, TimeUnit.SECONDS);
    }

    private StompSession connectToExternalScansNotificationsWebSocket() throws ExecutionException, InterruptedException, TimeoutException {
        WebSocketStompClient requestStompClient = createStompClient();
        // Define the callbacks and return the future when it is realized.
        return requestStompClient.connect(this.scanManagerDiscoveryUrl, new StompSessionHandlerAdapter() {
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                super.handleFrame(headers, payload);
                if (payload instanceof Json) {
                    logger.debug("[STOMP] " + payload.toString());
                    ExternalScan scan = ExternalScan.fromScannerJson((Json) payload);
                    scansService.createOrUpdateExternalScan(scan);
                } else {
                    logger.debug("[STOMP] null");
                }
            }

            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                logger.debug("Connected to external scans notifications websocket");
                super.afterConnected(session, connectedHeaders);
                session.subscribe(websocketExternalScanEndpoint, this);
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
        SockJsClient sockJsClient = new SockJsClient(List.of(new WebSocketTransport(client)));

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
    private static class JsonMessageConverter extends AbstractMessageConverter {
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
}