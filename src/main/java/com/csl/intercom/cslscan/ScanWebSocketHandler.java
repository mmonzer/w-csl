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
import lombok.Getter;
import lombok.Setter;
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
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.scheduling.concurrent.DefaultManagedTaskScheduler;
import org.springframework.util.MimeType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.net.ConnectException;
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
    private StompChannel stompNotificationChannel = null;
    private StompChannel stompRequestChannel = null;
    private StompChannel stompExternalScanChannel = null;
    private StompChannel stompImportNotificationChannel = null;
    private StompChannel stompExportNotificationChannel = null;
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

        StompChannel.disconnectIfHasSession(stompRequestChannel);
        StompChannel.disconnectIfHasSession(stompNotificationChannel);
        StompChannel.disconnectIfHasSession(stompImportNotificationChannel);
        StompChannel.disconnectIfHasSession(stompExportNotificationChannel);

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

        status.set("is_requests_websocket_connected", StompChannel.isConnected(stompRequestChannel));
        status.set("is_notifications_websocket_connected", StompChannel.isConnected(stompNotificationChannel));
        status.set("is_import_notifications_websocket_connected", StompChannel.isConnected(stompImportNotificationChannel));
        status.set("is_export_notifications_websocket_connected", StompChannel.isConnected(stompExportNotificationChannel));
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
        if (!StompChannel.isConnected(stompRequestChannel)) {
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
        if (StompChannel.isConnected(stompRequestChannel)) {
            if (uuids == null || uuids.isEmpty()) {
                stompRequestChannel.getSession().send(stompHeaders, "");
            } else {
                stompRequestChannel.getSession().send(stompHeaders, Json.array(uuids.toArray()));
            }
        }
    }

    /**
     * Check if the websockets are connected, and tries to connect if not.
     * Blocking, should be called asynchronously.
     */
    private void connectStompSessionsIfNecessary() {
        boolean newNotificationConnection = false;
        logger.trace("connectStompSessionsIfNecessary : {}", stompNotificationChannel);
        if (!StompChannel.isConnected(stompNotificationChannel)) {
            newNotificationConnection = reconnectToNotificationsChannel(newNotificationConnection);
        }

        boolean newRequestConnection = false;
        if (!StompChannel.isConnected(stompRequestChannel)) {
            newRequestConnection = reconnectToRequestChannel(newRequestConnection);
        }

        if (!StompChannel.isConnected(stompImportNotificationChannel)) {
            reconnectToImportNotificationChannel();
        }

        if (!StompChannel.isConnected(stompExportNotificationChannel)) {
            reconnectToExportNotificationChannel();
        }

        boolean newExternalScanConnection = false;
        if (!StompChannel.isConnected(stompExternalScanChannel)) {
            newExternalScanConnection = reconnectToExternalScanChannel(newExternalScanConnection);
        }

        if (newNotificationConnection || newRequestConnection || newExternalScanConnection) {
            externalScansService.handleConnectionEstablishedWithScanner();
        }
    }

    /**
     * Try to reconnect to the external scan channel for stomp notifications
     * @param newExternalScanConnection whether it's a new connection
     * @return whether the connection was successful
     */
    private boolean reconnectToExternalScanChannel(boolean newExternalScanConnection) {
        try {
            logger.trace("Connecting to external scans notifications websocket");
            stompExternalScanChannel = connectToExternalScansNotificationsWebSocket();
            newExternalScanConnection = true;
            logger.trace("stompExternalScanSession connected : {}", stompExternalScanChannel);
            reconnect(stompExternalScanChannel.getSession(), EXTERNAL_SCANS);
        } catch (InterruptedException | ExecutionException | TimeoutException | ConnectionLostException e) {
            disconnect(e, EXTERNAL_SCANS);
            StompChannel.setSessionNull(stompExternalScanChannel);
        } catch (Throwable e) {
            disconnect(new Exception(e), EXTERNAL_SCANS);
            logger.error("Unexpected exception at stompExternalScanSession", new Exception(e));
            StompChannel.setSessionNull(stompExternalScanChannel);
        }
        return newExternalScanConnection;
    }

    /**
     * Try to reconnect to the export notification channel for stomp notifications
     */
    private void reconnectToExportNotificationChannel() {
        try {
            logger.trace("stompExportNotificationSession connecting ...");
            stompExportNotificationChannel = subscribeToExportNotifications();
            logger.trace("stompExportNotificationSession connected : {}", stompExportNotificationChannel);
            reconnect(stompExportNotificationChannel.getSession(), EXPORT_NOTIFICATIONS);
        } catch (InterruptedException | ExecutionException | ResourceAccessException | ConnectionLostException e) {
            if (disconnect(e, EXPORT_NOTIFICATIONS)) {
                logger.warn("Error while connecting to export notifications websocket, retrying");
                logger.trace("Error while connecting to export notifications websocket, retrying", e);
            }
            StompChannel.setSessionNull(stompExportNotificationChannel);
        } catch (Throwable e) {
            disconnect(new Exception(e), EXPORT_NOTIFICATIONS);
            logger.error("Unexpected exception at stompExportNotificationSession", new Exception(e));
            StompChannel.setSessionNull(stompExportNotificationChannel);
        }
    }

    /**
     * Try to reconnect to the import notification channel for stomp notifications
     */
    private void reconnectToImportNotificationChannel() {
        try {
            logger.trace("stompImportNotificationSession connecting ...");
            stompImportNotificationChannel = subscribeToImportNotifications();
            logger.trace("stompImportNotificationSession connected : {}", stompImportNotificationChannel);
            reconnect(stompImportNotificationChannel.getSession(), IMPORT_NOTIFICATIONS);
        } catch (InterruptedException | ExecutionException | ResourceAccessException | ConnectionLostException e) {
            if (disconnect(e, IMPORT_NOTIFICATIONS)) {
                logger.warn("Error while connecting to import notifications websocket, retrying");
                logger.trace("Error while connecting to import notifications websocket, retrying", e);
            }
            StompChannel.setSessionNull(stompImportNotificationChannel);
        } catch (Throwable e) {
            disconnect(new Exception(e), IMPORT_NOTIFICATIONS);
            logger.error("Unexpected exception at stompImportNotificationSession", new Exception(e));
            StompChannel.setSessionNull(stompImportNotificationChannel);
        }
    }

    /**
     * Try to reconnect to the request channel for stomp notifications
     * @param newRequestConnection whether it's a new connection
     * @return whether the connection was successful
     */
    private boolean reconnectToRequestChannel(boolean newRequestConnection) {
        try {
            logger.trace("Connecting to requests websocket ...");
            stompRequestChannel = connectToRequestsWebSocket();
            logger.trace("stompRequestsSession connected : {}", stompRequestChannel);
            newRequestConnection = true;
            reconnect(stompRequestChannel.getSession(), REQUEST);
        } catch (InterruptedException | ExecutionException | TimeoutException | ConnectionLostException e) {
            if (disconnect(e, REQUEST)) {
                StompChannel.setSessionNull(stompRequestChannel);
            }
            StompChannel.setSessionNull(stompRequestChannel);
        } catch (Throwable e) {
            disconnect(new Exception(e), REQUEST);
            logger.error("Unexpected exception at stompRequestsSession", new Exception(e));
            StompChannel.setSessionNull(stompRequestChannel);
        }
        return newRequestConnection;
    }

    /**
     * Manages the reconnection of notification channel
     * @param newNotificationConnection whether it's a new connection or not
     * @return whether the connection was successful
     */
    private boolean reconnectToNotificationsChannel(boolean newNotificationConnection) {
        try {
            logger.trace("Connecting to notifications websocket ...");
            stompNotificationChannel = subscribeToNotifications();
            logger.trace("stompNotificationSession connected : {}", stompNotificationChannel.getSession());
            newNotificationConnection = true;
            reconnect(stompNotificationChannel.getSession(), "notifications");
        } catch (InterruptedException | ExecutionException | ResourceAccessException | ConnectionLostException e) {
            if (disconnect(e, "notifications")) {
                logger.warn("Error while connecting to notifications websocket, retrying");
                logger.trace("Error while connecting to notifications websocket, retrying", e);
            }
            if (stompNotificationChannel != null) {
                stompNotificationChannel.setSession(null);
            }
        } catch (Throwable e) {
            disconnect(new Exception(e), REQUEST);
            logger.error("Unexpected exception at stompNotificationSession", new Exception(e));
            if (stompNotificationChannel != null) {
                stompNotificationChannel.setSession(null);
            }
        }
        return newNotificationConnection;
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
     * Generic method to connect a channel in the web socket to CSL-Scan, and define the necessary callbacks (after connection and on message reception).
     * Blocks so should be called asynchronously.
     *
     * @param channel channel of the websocket uri
     * @param uri uri of the channel
     * @param handler handler for the channel connection
     * @return The session we just created.
     * @throws ExecutionException   if connection failed.
     * @throws InterruptedException if connection was interrupted.
     * @throws TimeoutException if connection timed out.
     */
    private StompChannel subscribeToChannel(StompChannel channel, String uri, StompSessionHandler handler) throws ExecutionException, InterruptedException, TimeoutException {
        if (channel == null) {
            channel = new StompChannel(createStompClient());
        }

        try {
            channel.setSession(channel.getClient().connectAsync(uri, handler).get(1000, TimeUnit.MILLISECONDS));
            return channel;
        } catch (Exception e) {
            if (e.getCause() instanceof ConnectException) {
                throw new ConnectionLostException("");
            }
            throw e;
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
    private StompChannel subscribeToNotifications() throws ExecutionException, InterruptedException, TimeoutException {
        return subscribeToChannel(stompNotificationChannel, this.scanManagerDiscoveryUrl, new NotificationsStompSessionHandler());
    }

    private StompChannel subscribeToImportNotifications() throws ExecutionException, InterruptedException, TimeoutException {
        return subscribeToChannel(stompImportNotificationChannel, this.scanManagerDiscoveryUrl, new ImportStompSessionHandler());
    }

    private StompChannel subscribeToExportNotifications() throws ExecutionException, InterruptedException, TimeoutException {
        return subscribeToChannel(stompExportNotificationChannel, this.scanManagerDiscoveryUrl, new ExportStompSessionHandler());
    }

    /**
     * Connect the requests socket to CSL-Scan, and define the necessary callbacks (after connection and on message reception).
     * Blocks so should be called asynchronously.
     *
     * @return The session we just created.
     * @throws ExecutionException   if connection failed.
     * @throws InterruptedException if connection was interrupted.
     */
    private StompChannel connectToRequestsWebSocket() throws ExecutionException, InterruptedException, TimeoutException {
        return subscribeToChannel(stompRequestChannel, this.scanManagerDiscoveryUrl, new RequestStompSessionHandler());
    }

    private StompChannel connectToExternalScansNotificationsWebSocket() throws ExecutionException, InterruptedException, TimeoutException {
        return subscribeToChannel(stompExternalScanChannel, this.scanManagerDiscoveryUrl, new ExternalScanStompSessionHandler());
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

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            session.disconnect();
        }

        /**
         * Try to execute all the scan requests in the queue.
         */
        private void purgeScanRequestsQueue() {
            List<String> scanRequest;
            while (moduleConnected && (scanRequest = scanRequestsQueue.poll()) != null) {
                if (!StompChannel.isConnected(stompRequestChannel)) {
                    scanRequestsQueue.add(scanRequest);
                    break;
                }
                startScan(scanRequest);
                if (!StompChannel.isConnected(stompRequestChannel)) {
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

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            session.disconnect();
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
                logger.trace("Received import notification from CSL-Scan: {} [{}]", importQuery.getId(), importQuery.getImportQueryStatus());
                importExportBsonService.updateImportQueryStatus(importQuery.getId(), importQuery.getImportQueryStatus());
            } else {
                logger.trace("Received import notification from CSL-Scan that could not be parsed: {}", payload.toString());
            }
        }

        @Override
        public void onConnect(StompSession session, StompHeaders connectedHeaders) {
            session.subscribe(WEBSOCKET_IMPORT_NOTIFICATIONS_ENDPOINT, this);
            CSLNetworkLogger.info(LoggerFactory.getLogger(ScanWebSocketHandler.class), "scanWebsocket/import", "WS", "Connected to import notifications websocket at " + scanManagerDiscoveryUrl + "/" + WEBSOCKET_IMPORT_NOTIFICATIONS_ENDPOINT);
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            session.disconnect();
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

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            session.disconnect();
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
            if (ScanConstants.getFinishedScanStatuses().contains(scanStatus)) {
                logger.info("Discovery scan finished : status {} ({} devices scanned, but {} failed)", payload.get("status").asString(),
                        payload.get("entitiesUuid").asJsonList().size(), payload.get("entitiesInError").asJsonList().size());
                // Put the end date in the scan information and notify DB-API the scan ended.
                OffsetDateTime endDate = OffsetDateTime.now();
                if (ScanConstants.getSuccessScanStatuses().contains(scanStatus)) {
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

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            session.disconnect();
        }
    }

    @Getter
    private static class StompChannel {
        private final WebSocketStompClient client;
        @Setter
        private StompSession session;

        public StompChannel(WebSocketStompClient client) {
            this.client = client;
        }

        public static boolean isConnected(StompChannel channel) {
            return channel != null && channel.getSession() != null && channel.getSession().isConnected();
        }

        public static void disconnectIfHasSession(StompChannel channel) {
            if (channel != null && channel.getSession() != null) {
                channel.getSession().disconnect();
            }
        }

        public static void setSessionNull(StompChannel channel) {
            if (channel != null) {
                channel.setSession(null);
            }
        }
    }
}