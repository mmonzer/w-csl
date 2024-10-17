package com.csl.intercom.cslscan;

import com.csl.intercom.cslscan.models.ExportQuery;
import com.csl.intercom.cslscan.models.ImportQuery;
import com.csl.intercom.cslscan.models.scans.ExternalScan;
import com.csl.intercom.cslscan.services.ImportExportBsonService;
import com.csl.intercom.dbapi.models.ScanEntity;
import com.csl.intercom.services.CpeScanService;
import com.csl.intercom.services.ExternalScansService;
import com.csl.logger.LoggerCustomEndpoints;
import com.csl.logger.LoggerInterfaces;
import com.csl.util.ThreadUtils;
import com.csl.web.websockets.CorrelatedStompSessionHandlerAdapter;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import main.services.DiscoveryServices;
import main.services.JsonApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.simp.stomp.ConnectionLostException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
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

import static com.csl.web.jcmdoversocket.CSLWebSocketForJcmd.X_CORRELATION_ID;

/**
 * Handle the WebSocket connections with CSL-Scan.
 */
public class ScanWebSocketHandler {
    static private final Logger logger = LoggerFactory.getLogger(ScanWebSocketHandler.class);
    private final ImportExportBsonService importExportBsonService;
    private final DiscoveryServices discoveryService;
    private final String websocketNotificationsEndpoint = "/discovery/ready";
    private final String websocketStartDiscoveryEndpoint = "/discovery/start";
    private final String websocketExternalScanEndpoint = "/external_discovery/ready";
    private final String websocketImportNotificationsEndpoint = "/import/ready";
    private final String websocketExportNotificationsEndpoint = "/export/ready";
    private final Queue<List<String>> scanRequestsQueue = new ConcurrentLinkedQueue<>();
    private String scanManagerDiscoveryUrl;
    private ScheduledExecutorService webSocketsConnectionAttempts;
    private ExternalScansService externalScansService;
    private CpeScanService cpeScanService;
    private StompSession stompRequestsSession = null;
    private StompSession stompNotificationSession = null;
    private StompSession stompExternalScanSession = null;
    private StompSession stompImportNotificationSession = null;
    private StompSession stompExportNotificationSession = null;
    private boolean moduleConnected = false;

    /**
     * Create a new manager with the correct URL.
     *
     * @param discoveryService        The parent {@link DiscoveryServices}, used to handle the necessary
     * @param scanManagerDiscoveryUrl The URL of CSL-Scan.
     */
    public ScanWebSocketHandler(DiscoveryServices discoveryService, String scanManagerDiscoveryUrl, CpeScanService cpeScanService, ImportExportBsonService importExportBsonService, ExternalScansService externalScansService) {
        this.discoveryService = discoveryService;
        this.scanManagerDiscoveryUrl = scanManagerDiscoveryUrl;
        this.importExportBsonService = importExportBsonService;
        this.cpeScanService = cpeScanService;
        this.externalScansService = externalScansService;

        // Schedule reconnection to websockets every 2 seconds
        webSocketsConnectionAttempts = Executors.newScheduledThreadPool(1);
        ThreadUtils.uncorrelatedSingleThreadScheduledAtFixedRate(
                webSocketsConnectionAttempts,
                this::connectStompSessionsIfNecessary,
                0, 2, TimeUnit.SECONDS,
                LoggerCustomEndpoints.RECONNECT_WS_SCAN, LoggerInterfaces.CSL_CLIENT);
    }

    /**
     * Stop the connections with CSL-Scan
     */
    public void stop() {
        webSocketsConnectionAttempts.shutdown();
        if (stompRequestsSession != null) {
            stompRequestsSession.disconnect();
        }
        if (stompNotificationSession != null) {
            stompNotificationSession.disconnect();
        }
        if (stompImportNotificationSession != null) {
            stompImportNotificationSession.disconnect();
        }
        if (stompExportNotificationSession != null) {
            stompExportNotificationSession.disconnect();
        }

        logger.info("CSL-Scan websocket disconnected at {}", scanManagerDiscoveryUrl);
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
        status.set("is_import_notifications_websocket_connected", stompImportNotificationSession != null && stompImportNotificationSession.isConnected());
        status.set("is_export_notifications_websocket_connected", stompExportNotificationSession != null && stompExportNotificationSession.isConnected());
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
        connectStompSessionsIfNecessary();
        if ((stompRequestsSession == null || !stompRequestsSession.isConnected())) {
            // not connected to csl-scan --> add this request to the queue
            scanRequestsQueue.add(entities);
            logger.debug("Scan service unavailable, added scan request to queue");
            return JsonApiResponse.error("Scan service unavailable, added scan request to queue");
        } else {
            // request the startScan to the csl-scan
            startScan(entities);
            logger.debug("CPE scan started");
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
        StompHeaders stompHeaders = new StompHeaders();
        stompHeaders.add(StompHeaders.DESTINATION, websocketStartDiscoveryEndpoint);
        stompHeaders.add(X_CORRELATION_ID, MDC.get(X_CORRELATION_ID));
        if (uuids == null || uuids.isEmpty()) {
//            stompRequestsSession.send(websocketStartDiscoveryEndpoint, "");
            stompRequestsSession.send(stompHeaders, "");
        } else {
//            stompRequestsSession.send(websocketStartDiscoveryEndpoint, Json.array(uuids.toArray()));
            stompRequestsSession.send(stompHeaders, Json.array(uuids.toArray()));
        }
    }

    /**
     * Check if the websockets are connected, and tries to connect if not.
     * Blocking, should be called asynchronously.
     */
    private void connectStompSessionsIfNecessary() {
        boolean new_notification_connection = false;
        logger.trace("connectStompSessionsIfNecessary : {}", stompNotificationSession);
        if (stompNotificationSession == null || !stompNotificationSession.isConnected()) {
            try {
                logger.trace("Connecting to notifications websocket ...");
                stompNotificationSession = subscribeToNotifications();
                logger.trace("stompNotificationSession connected : {}", stompNotificationSession);
                new_notification_connection = true;
                reconnect(stompNotificationSession, "notifications");
            } catch (InterruptedException | ExecutionException | ResourceAccessException | ConnectionLostException e) {
                if (disconnect(e, "notifications")) {
                    logger.warn("Error while connecting to notifications websocket, retrying");
                    logger.trace("Error while connecting to notifications websocket, retrying", e);
                }
                stompNotificationSession = null;
            } catch (Throwable e) {
                disconnect(new Exception(e), "request");
                logger.error("Unexpected exception at stompNotificationSession", new Exception(e));
                stompNotificationSession = null;
            }
        }

        boolean new_request_connection = false;
        if (stompRequestsSession == null || !stompRequestsSession.isConnected()) {
            try {
                logger.trace("Connecting to requests websocket ...");
                stompRequestsSession = connectToRequestsWebSocket();
                logger.trace("stompRequestsSession connected : {}", stompRequestsSession);
                new_request_connection = true;
                reconnect(stompRequestsSession, "request");
            } catch (InterruptedException | ExecutionException | TimeoutException | ConnectionLostException e) {
                if (disconnect(e, "request")) {
                    stompRequestsSession = null;
                }
            } catch (Throwable e) {
                disconnect(new Exception(e), "request");
                logger.error("Unexpected exception at stompRequestsSession", new Exception(e));
                stompRequestsSession = null;
            }
        }

        if (stompImportNotificationSession == null || !stompImportNotificationSession.isConnected()) {
            try {
                logger.trace("stompImportNotificationSession connecting ...");
                stompImportNotificationSession = subscribeToImportNotifications();
                logger.trace("stompImportNotificationSession connected : {}", stompImportNotificationSession);
                reconnect(stompImportNotificationSession, "import notifications");
            } catch (InterruptedException | ExecutionException | ResourceAccessException | ConnectionLostException e) {
                if (disconnect(e, "import notifications")) {
                    logger.warn("Error while connecting to import notifications websocket, retrying");
                    logger.trace("Error while connecting to import notifications websocket, retrying", e);
                }
                stompImportNotificationSession = null;
            } catch (Throwable e) {
                disconnect(new Exception(e), "import notifications");
                logger.error("Unexpected exception at stompImportNotificationSession", new Exception(e));
                stompImportNotificationSession = null;
            }
        }

        if (stompExportNotificationSession == null || !stompExportNotificationSession.isConnected()) {
            try {
                logger.trace("stompExportNotificationSession connecting ...");
                stompExportNotificationSession = subscribeToExportNotifications();
                logger.trace("stompExportNotificationSession connected : {}", stompExportNotificationSession);
                reconnect(stompExportNotificationSession, "export notifications");
            } catch (InterruptedException | ExecutionException | ResourceAccessException | ConnectionLostException e) {
                if (disconnect(e, "export notifications")) {
                    logger.warn("Error while connecting to export notifications websocket, retrying");
                    logger.trace("Error while connecting to export notifications websocket, retrying", e);
                }
                stompExportNotificationSession = null;
            } catch (Throwable e) {
                disconnect(new Exception(e), "export notifications");
                logger.error("Unexpected exception at stompExportNotificationSession", new Exception(e));
                stompExportNotificationSession = null;
            }
        }

        boolean new_external_scan_connection = false;
        if (stompExternalScanSession == null || !stompExternalScanSession.isConnected()) {
            try {
                logger.trace("Connecting to external scans notifications websocket");
                stompExternalScanSession = connectToExternalScansNotificationsWebSocket();
                new_external_scan_connection = true;
                logger.trace("stompExternalScanSession connected : {}", stompExternalScanSession);
                reconnect(stompExternalScanSession, "external scans");
            } catch (InterruptedException | ExecutionException | TimeoutException | ConnectionLostException e) {
                disconnect(e, "external scans");
                stompExternalScanSession = null;
            } catch (Throwable e) {
                disconnect(new Exception(e), "external scans");
                logger.error("Unexpected exception at stompExternalScanSession", new Exception(e));
                stompExternalScanSession = null;
            }
        }

        if (new_notification_connection || new_request_connection || new_external_scan_connection) {
            externalScansService.handleConnectionEstablishedWithScanner();
        }
    }

    /**
     * Verify if the client is reconnected, logs if true and modifies the global variable
     *
     * @param session stomp session, verify that is not null
     * @param msg     the channel to customize the logs
     */
    private void reconnect(StompSession session, String msg) {
        if (!moduleConnected && session != null && session.isConnected()) {
            logger.info("Connection recovered with CSLScan for STOMP - {}", msg);
            moduleConnected = true;
        } else {
            moduleConnected = false;
        }
    }

    /**
     * Verify from the exception if the client is disconnected, logs if true and modifies the global variable
     *
     * @param exception exception from the initialisation
     * @param msg       the channel to customize the logs
     */
    private boolean disconnect(Exception exception, String msg) {
        if (moduleConnected) {
            logger.warn("Connection lost with CSLScan for STOMP - {}", msg);
            moduleConnected = false;
        }
        return moduleConnected;
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
        return stompClient.connect(this.scanManagerDiscoveryUrl, new CorrelatedStompSessionHandlerAdapter() {
            @Override
            public void onFrame(StompHeaders headers, Object payloadRaw) {
                Json payload = (Json) payloadRaw;

                //region Log the notification
                if (payload != null) {
                    logger.trace("[STOMP] " + payload.toString());
                } else {
                    logger.trace("[STOMP] null");
                }
                //endregion Log the notification

                String scanId = JsonUtil.getStringFromJson(payload, "uuid", null);

                //region Get or Create Scan Entity
                // Check if we already know the scan
                ScanEntity scan = cpeScanService.getScanByScanId(scanId);

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
                    logger.debug("Discovery scan finished : status {} ({} devices scanned, but {} failed)", payload.get("status").asString(),
                            payload.get("entitiesUuid").asJsonList().size(), payload.get("entitiesInError").asJsonList().size());
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

                cpeScanService.createOrUpdate(scan);
            }

            @Override
            public void onConnect(StompSession session, StompHeaders headers) {
                session.subscribe(websocketNotificationsEndpoint, this);
                logger.info("Connected to notifications websocket at {}/{}", scanManagerDiscoveryUrl, websocketNotificationsEndpoint);
            }

            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                //logger.warn("Transport Error", exception);
//                super.handleException(session, command, headers, payload, exception);
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                //logger.warn("Transport Error", exception);
//                super.handleTransportError(session, exception);
            }
        }).get(1000, TimeUnit.MILLISECONDS);
    }

    private StompSession subscribeToImportNotifications() throws ExecutionException, InterruptedException, TimeoutException {
        WebSocketStompClient stompClient = createStompClient();

        return stompClient.connect(this.scanManagerDiscoveryUrl, new CorrelatedStompSessionHandlerAdapter() {
            @Override
            public void onFrame(StompHeaders headers, Object payloadRaw) {
                Json payload = (Json) payloadRaw;

                if (payload != null) {
                    logger.trace("[STOMP] " + payload.toString());
                } else {
                    logger.trace("[STOMP] null");
                    return;
                }

                ImportQuery importQuery = ImportQuery.fromScannerJson(payload);
                if (importQuery != null) {
                    logger.trace("Received import notification from CSL-Scan: {} [{}]", importQuery.getId(), importQuery.getStatus());
                    importExportBsonService.updateImportQueryStatus(importQuery.getId(), importQuery.getStatus());
                } else {
                    logger.trace("Received import notification from CSL-Scan that could not be parsed: {}", payload.toString());
                }
            }

            @Override
            public void onConnect(StompSession session, StompHeaders connectedHeaders) {
                session.subscribe(websocketImportNotificationsEndpoint, this);
                logger.info("Connected to notifications websocket at {}/{}", scanManagerDiscoveryUrl, websocketImportNotificationsEndpoint);
            }
        }).get(1000, TimeUnit.MILLISECONDS);
    }

    private StompSession subscribeToExportNotifications() throws ExecutionException, InterruptedException, TimeoutException {
        WebSocketStompClient stompClient = createStompClient();

        return stompClient.connect(this.scanManagerDiscoveryUrl, new CorrelatedStompSessionHandlerAdapter() {
            @Override
            public void onFrame(StompHeaders headers, Object payloadRaw) {
                Json payload = (Json) payloadRaw;

                if (payload != null) {
                    logger.trace("[STOMP] " + payload.toString());
                } else {
                    logger.trace("[STOMP] null");
                    return;
                }

                ExportQuery exportQuery = ExportQuery.fromScannerJson(payload);
                if (exportQuery != null) {
                    logger.trace("Received export notification from CSL-Scan: {} [{}]", exportQuery.getId(), exportQuery.getStatus());
                    importExportBsonService.updateExportQueryStatus(exportQuery.getId(), exportQuery.getStatus());
                } else {
                    logger.trace("Received export notification from CSL-Scan that could not be parsed: {}", payload.toString());
                }
            }

            @Override
            public void onConnect(StompSession session, StompHeaders connectedHeaders) {
                session.subscribe(websocketExportNotificationsEndpoint, this);
                logger.info("Connected to import notifications websocket at {}/{}", scanManagerDiscoveryUrl, websocketExportNotificationsEndpoint);
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
        return requestStompClient.connect(this.scanManagerDiscoveryUrl, new CorrelatedStompSessionHandlerAdapter() {
            @Override
            public void onFrame(StompHeaders headers, Object payload) {
                if (payload != null) {
                    logger.debug("[STOMP] " + payload.toString());
                    // handle response
                } else {
                    logger.debug("[STOMP] null");
                }
            }

            @Override
            public void onConnect(StompSession session, StompHeaders connectedHeaders) {
                purgeScanRequestsQueue();
                logger.info("Connected to requests websocket at {}", scanManagerDiscoveryUrl);
            }
        }).get(1, TimeUnit.SECONDS);
    }

    private StompSession connectToExternalScansNotificationsWebSocket() throws ExecutionException, InterruptedException, TimeoutException {
        WebSocketStompClient requestStompClient = createStompClient();
        // Define the callbacks and return the future when it is realized.
        return requestStompClient.connect(this.scanManagerDiscoveryUrl, new CorrelatedStompSessionHandlerAdapter() {
            @Override
            public void onFrame(StompHeaders headers, Object payload) {
                if (payload instanceof Json) {
                    logger.debug("[STOMP] " + payload.toString());
                    ExternalScan scan = ExternalScan.fromScannerJson((Json) payload);
//                    externalScansService.createOrUpdateExternalScan(scan);
                    externalScansService.handleScanNotification(scan);
                } else {
                    logger.debug("[STOMP] null");
                }
            }

            @Override
            public void onConnect(StompSession session, StompHeaders connectedHeaders) {
                session.subscribe(websocketExternalScanEndpoint, this);
                logger.info("Connected to external scans notifications websocket at {}/{}", scanManagerDiscoveryUrl, websocketExternalScanEndpoint);
            }
        }).get(1, TimeUnit.SECONDS);
    }

    /**
     * Create the STOMP client with our custom message interpreter.
     *
     * @return a {@link WebSocketStompClient} suitable for our needs.
     */
    private WebSocketStompClient createStompClient() {
        if (!discoveryService.getScanApiHandler().getStatus().isSuccess()) {
            throw new ConnectionLostException("CSL-Scan disconnected");
        }

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
        while (moduleConnected && (scanRequest = scanRequestsQueue.poll()) != null) {
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