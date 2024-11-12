package com.csl.intercom.cslscan;

import com.csl.intercom.cslscan.models.ExportQuery;
import com.csl.intercom.cslscan.models.ImportQuery;
import com.csl.intercom.cslscan.models.scans.ExternalScan;
import com.csl.intercom.cslscan.services.ImportExportBsonService;
import com.csl.intercom.dbapi.models.ScanEntity;
import com.csl.intercom.services.CpeScanService;
import com.csl.intercom.services.ExternalScansService;
import com.csl.logger.CSLApplicativeLogger;
import com.csl.logger.CSLNetworkLogger;
import com.csl.logger.LoggerCustomEndpoints;
import com.csl.logger.LoggerInterfaces;
import com.csl.util.ThreadUtils;
import com.csl.web.websockets.CorrelatedStompSessionHandlerAdapter;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import main.services.DiscoveryServices;
import main.services.JsonApiResponse;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.simp.stomp.ConnectionLostException;
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

import static com.csl.logger.LoggerConstants.X_CORRELATION_ID;

/**
 * Handle the WebSocket connections with CSL-Scan.
 */
public class ScanWebSocketHandler {
    private static final CSLApplicativeLogger logger = CSLApplicativeLogger.getLogger(ScanWebSocketHandler.class);
    public static final String REQUEST = "request";
    public static final String IMPORT_NOTIFICATIONS = "import notifications";
    public static final String EXTERNAL_SCANS = "external scans";
    public static final String EXPORT_NOTIFICATIONS = "export notifications";
    private final ImportExportBsonService importExportBsonService;
    private final DiscoveryServices discoveryService;
    private static final String WEBSOCKET_NOTIFICATIONS_ENDPOINT = "/discovery/ready";
    private static final String WEBSOCKET_START_DISCOVERY_ENDPOINT = "/discovery/start";
    private static final String WEBSOCKET_EXTERNAL_SCAN_ENDPOINT = "/external_discovery/ready";
    private static final String WEBSOCKET_IMPORT_NOTIFICATIONS_ENDPOINT = "/import/ready";
    private static final String WEBSOCKET_EXPORT_NOTIFICATIONS_ENDPOINT = "/export/ready";
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
        stompHeaders.add(StompHeaders.DESTINATION, WEBSOCKET_START_DISCOVERY_ENDPOINT);
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
        boolean newNotificationConnection = false;
        logger.trace("connectStompSessionsIfNecessary : {}", stompNotificationSession);
        if (stompNotificationSession == null || !stompNotificationSession.isConnected()) {
            try {
                logger.trace("Connecting to notifications websocket ...");
                stompNotificationSession = subscribeToNotifications();
                logger.trace("stompNotificationSession connected : {}", stompNotificationSession);
                newNotificationConnection = true;
                reconnect(stompNotificationSession, "notifications");
            } catch (InterruptedException | ExecutionException | ResourceAccessException | ConnectionLostException e) {
                if (disconnect(e, "notifications")) {
                    logger.warn("Error while connecting to notifications websocket, retrying");
                    logger.trace("Error while connecting to notifications websocket, retrying", e);
                }
                stompNotificationSession = null;
            } catch (Throwable e) {
                disconnect(new Exception(e), REQUEST);
                logger.error("Unexpected exception at stompNotificationSession", new Exception(e));
                stompNotificationSession = null;
            }
        }

        boolean newRequestConnection = false;
        if (stompRequestsSession == null || !stompRequestsSession.isConnected()) {
            try {
                logger.trace("Connecting to requests websocket ...");
                stompRequestsSession = connectToRequestsWebSocket();
                logger.trace("stompRequestsSession connected : {}", stompRequestsSession);
                newRequestConnection = true;
                reconnect(stompRequestsSession, REQUEST);
            } catch (InterruptedException | ExecutionException | TimeoutException | ConnectionLostException e) {
                if (disconnect(e, REQUEST)) {
                    stompRequestsSession = null;
                }
            } catch (Throwable e) {
                disconnect(new Exception(e), REQUEST);
                logger.error("Unexpected exception at stompRequestsSession", new Exception(e));
                stompRequestsSession = null;
            }
        }

        if (stompImportNotificationSession == null || !stompImportNotificationSession.isConnected()) {
            try {
                logger.trace("stompImportNotificationSession connecting ...");
                stompImportNotificationSession = subscribeToImportNotifications();
                logger.trace("stompImportNotificationSession connected : {}", stompImportNotificationSession);
                reconnect(stompImportNotificationSession, IMPORT_NOTIFICATIONS);
            } catch (InterruptedException | ExecutionException | ResourceAccessException | ConnectionLostException e) {
                if (disconnect(e, IMPORT_NOTIFICATIONS)) {
                    logger.warn("Error while connecting to import notifications websocket, retrying");
                    logger.trace("Error while connecting to import notifications websocket, retrying", e);
                }
                stompImportNotificationSession = null;
            } catch (Throwable e) {
                disconnect(new Exception(e), IMPORT_NOTIFICATIONS);
                logger.error("Unexpected exception at stompImportNotificationSession", new Exception(e));
                stompImportNotificationSession = null;
            }
        }

        if (stompExportNotificationSession == null || !stompExportNotificationSession.isConnected()) {
            try {
                logger.trace("stompExportNotificationSession connecting ...");
                stompExportNotificationSession = subscribeToExportNotifications();
                logger.trace("stompExportNotificationSession connected : {}", stompExportNotificationSession);
                reconnect(stompExportNotificationSession, EXPORT_NOTIFICATIONS);
            } catch (InterruptedException | ExecutionException | ResourceAccessException | ConnectionLostException e) {
                if (disconnect(e, EXPORT_NOTIFICATIONS)) {
                    logger.warn("Error while connecting to export notifications websocket, retrying");
                    logger.trace("Error while connecting to export notifications websocket, retrying", e);
                }
                stompExportNotificationSession = null;
            } catch (Throwable e) {
                disconnect(new Exception(e), EXPORT_NOTIFICATIONS);
                logger.error("Unexpected exception at stompExportNotificationSession", new Exception(e));
                stompExportNotificationSession = null;
            }
        }

        boolean newExternalScanConnection = false;
        if (stompExternalScanSession == null || !stompExternalScanSession.isConnected()) {
            try {
                logger.trace("Connecting to external scans notifications websocket");
                stompExternalScanSession = connectToExternalScansNotificationsWebSocket();
                newExternalScanConnection = true;
                logger.trace("stompExternalScanSession connected : {}", stompExternalScanSession);
                reconnect(stompExternalScanSession, EXTERNAL_SCANS);
            } catch (InterruptedException | ExecutionException | TimeoutException | ConnectionLostException e) {
                disconnect(e, EXTERNAL_SCANS);
                stompExternalScanSession = null;
            } catch (Throwable e) {
                disconnect(new Exception(e), EXTERNAL_SCANS);
                logger.error("Unexpected exception at stompExternalScanSession", new Exception(e));
                stompExternalScanSession = null;
            }
        }

        if (newNotificationConnection || newRequestConnection || newExternalScanConnection) {
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
        return stompClient.connect(this.scanManagerDiscoveryUrl, new NotificationsStompSessionHandler()).get(1000, TimeUnit.MILLISECONDS);
    }

    private StompSession subscribeToImportNotifications() throws ExecutionException, InterruptedException, TimeoutException {
        WebSocketStompClient stompClient = createStompClient();

        return stompClient.connect(this.scanManagerDiscoveryUrl, new ImportStompSessionHandler()).get(1000, TimeUnit.MILLISECONDS);
    }

    private StompSession subscribeToExportNotifications() throws ExecutionException, InterruptedException, TimeoutException {
        WebSocketStompClient stompClient = createStompClient();

        return stompClient.connect(this.scanManagerDiscoveryUrl, new ExportStompSessionHandler()).get(1000, TimeUnit.MILLISECONDS);
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
        return requestStompClient.connect(this.scanManagerDiscoveryUrl, new RequestStompSessionHandler()).get(1, TimeUnit.SECONDS);
    }

    private StompSession connectToExternalScansNotificationsWebSocket() throws ExecutionException, InterruptedException, TimeoutException {
        WebSocketStompClient requestStompClient = createStompClient();
        // Define the callbacks and return the future when it is realized.
        return requestStompClient.connect(this.scanManagerDiscoveryUrl, new ExternalScanStompSessionHandler()).get(1, TimeUnit.SECONDS);
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
                return payload.toString().getBytes();
            } else if (payload instanceof String stringPayload) {
                return stringPayload.getBytes();
            } else if (payload instanceof byte[]) {
                return payload;
            } else if (payload instanceof String[] arrayPayload) {
                Json result = Json.array();
                for (String x : arrayPayload) {
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
            if (payload instanceof String stringPayload) {
                return Json.read(stringPayload);
            } else if (payload instanceof byte[] bytesPayload) {
                return Json.read(new String(bytesPayload));
            } else {
                throw new RuntimeException("Bad conversion");
            }
        }
    }

    /**
     * Class handler for the request websocket
     */
    private class RequestStompSessionHandler extends CorrelatedStompSessionHandlerAdapter {
        @Override
        public void onFrame(StompHeaders headers, Object payload) {
            if (payload != null) {
                logger.debug("[STOMP] " + payload);
                // handle response
            } else {
                logger.debug("[STOMP] null");
            }
        }

        @Override
        public void onConnect(StompSession session, StompHeaders connectedHeaders) {
            purgeScanRequestsQueue();
            CSLNetworkLogger.info(LoggerFactory.getLogger(ScanWebSocketHandler.class), "scanWebsocket/", "WS", "Connected to requests websocket at " + scanManagerDiscoveryUrl);
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
    }

    /**
     * Class handler for the export bson websocket
     */
    private class ExportStompSessionHandler extends CorrelatedStompSessionHandlerAdapter {
        @Override
        public void onFrame(StompHeaders headers, Object payloadRaw) {
            Json payload = (Json) payloadRaw;

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
            session.subscribe(WEBSOCKET_EXPORT_NOTIFICATIONS_ENDPOINT, this);
            CSLNetworkLogger.info(LoggerFactory.getLogger(ScanWebSocketHandler.class), "scanWebsocket/export", "WS", "Connected to export notifications websocket at " + scanManagerDiscoveryUrl + "/" + WEBSOCKET_EXPORT_NOTIFICATIONS_ENDPOINT);
        }
    }

    /**
     * Class handler for the import bson websocket
     */
    private class ImportStompSessionHandler extends CorrelatedStompSessionHandlerAdapter {
        @Override
        public void onFrame(StompHeaders headers, Object payloadRaw) {
            Json payload = (Json) payloadRaw;

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
            session.subscribe(WEBSOCKET_IMPORT_NOTIFICATIONS_ENDPOINT, this);
            CSLNetworkLogger.info(LoggerFactory.getLogger(ScanWebSocketHandler.class), "scanWebsocket/import", "WS", "Connected to import notifications websocket at " + scanManagerDiscoveryUrl + "/" + WEBSOCKET_IMPORT_NOTIFICATIONS_ENDPOINT);
        }
    }

    /**
     * Class handler for the external scan websocket
     */
    private class ExternalScanStompSessionHandler extends CorrelatedStompSessionHandlerAdapter {
        @Override
        public void onFrame(StompHeaders headers, Object payload) {
            if (payload instanceof Json jsonPayload) {
                logger.debug("[STOMP] " + payload);
                ExternalScan scan = ExternalScan.fromScannerJson(jsonPayload);
//                    externalScansService.createOrUpdateExternalScan(scan);
                externalScansService.handleScanNotification(scan);
            } else {
                logger.debug("[STOMP] null");
            }
        }

        @Override
        public void onConnect(StompSession session, StompHeaders connectedHeaders) {
            session.subscribe(WEBSOCKET_EXTERNAL_SCAN_ENDPOINT, this);
            CSLNetworkLogger.info(LoggerFactory.getLogger(ScanWebSocketHandler.class), "scanWebsocket/external_discovery", "WS", "Connected to external scans notifications websocket at " + scanManagerDiscoveryUrl + "/" + WEBSOCKET_EXTERNAL_SCAN_ENDPOINT);
        }
    }

    /**
     * Class handler for the notifications scan websocket
     */
    private class NotificationsStompSessionHandler extends CorrelatedStompSessionHandlerAdapter {
        @Override
        public void onFrame(StompHeaders headers, Object payloadRaw) {
            Json payload = (Json) payloadRaw;

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
                logger.info("Discovery scan finished : status {} ({} devices scanned, but {} failed)", payload.get("status").asString(),
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
            session.subscribe(WEBSOCKET_NOTIFICATIONS_ENDPOINT, this);
            CSLNetworkLogger.info(LoggerFactory.getLogger(ScanWebSocketHandler.class), "scanWebsocket/discovery", "WS", "Connected to notifications websocket at " + scanManagerDiscoveryUrl + "/" + WEBSOCKET_NOTIFICATIONS_ENDPOINT);
        }
    }
}