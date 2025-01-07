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
import org.eclipse.jetty.io.EofException;
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
import java.nio.channels.ClosedChannelException;
import java.time.OffsetDateTime;
import java.util.Collections;
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
    private StompChannel stompNotificationChannel = new StompChannel(createStompClient());
    private StompChannel stompRequestChannel = new StompChannel(createStompClient());
    private StompChannel stompExternalScanChannel = new StompChannel(createStompClient());
    private StompChannel stompImportNotificationChannel = new StompChannel(createStompClient());
    private StompChannel stompExportNotificationChannel = new StompChannel(createStompClient());
    private boolean isScanConnected = false;

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
        logger.trace("connectStompSessionsIfNecessary : {}", stompNotificationChannel);
        boolean successfulNotificationConnection = false;
        if (!StompChannel.isConnected(stompNotificationChannel)) {
            successfulNotificationConnection = reconnectToNotificationsChannel();
        }

        boolean sucessfulRequestConnection = false;
        if (!StompChannel.isConnected(stompRequestChannel)) {
            sucessfulRequestConnection = reconnectToRequestChannel();
        }

        if (!StompChannel.isConnected(stompImportNotificationChannel)) {
            reconnectToImportNotificationChannel();
        }

        if (!StompChannel.isConnected(stompExportNotificationChannel)) {
            reconnectToExportNotificationChannel();
        }

        boolean successfulExternalScanConnection = false;
        if (!StompChannel.isConnected(stompExternalScanChannel)) {
            successfulExternalScanConnection = reconnectToExternalScanChannel();
        }

        if (successfulNotificationConnection || sucessfulRequestConnection || successfulExternalScanConnection) {
            externalScansService.handleConnectionEstablishedWithScanner();
        }
    }

    // region reconnect channels

    /**
     * Try to logIfScanWasNotConnected to the external scan channel for stomp notifications
     *
     * @return whether the connection was successful
     */
    private boolean reconnectToExternalScanChannel() {
        stompExternalScanChannel = reconnectToScanWebSocket(stompExternalScanChannel, "External Scan", this.scanManagerDiscoveryUrl, new ExternalScanStompSessionHandler());
        return StompChannel.isConnected(stompExternalScanChannel);
    }

    /**
     * Try to logIfScanWasNotConnected to the export notification channel for stomp notifications
     */
    private void reconnectToExportNotificationChannel() {
        stompExportNotificationChannel = reconnectToScanWebSocket(stompExportNotificationChannel, "Export Notifications", this.scanManagerDiscoveryUrl, new ExportStompSessionHandler());
    }

    /**
     * Try to logIfScanWasNotConnected to the import notification channel for stomp notifications
     */
    private void reconnectToImportNotificationChannel() {
        stompImportNotificationChannel = reconnectToScanWebSocket(stompImportNotificationChannel, "Import Notifications", this.scanManagerDiscoveryUrl, new ImportStompSessionHandler());
    }

    /**
     * Try to logIfScanWasNotConnected to the request channel for stomp notifications
     *
     * @return whether the connection was successful
     */
    private boolean reconnectToRequestChannel() {
        stompRequestChannel = reconnectToScanWebSocket(stompRequestChannel, "Request", this.scanManagerDiscoveryUrl, new RequestStompSessionHandler());
        return StompChannel.isConnected(stompRequestChannel);
    }

    /**
     * Manages the reconnection of notification channel
     *
     * @return whether the connection was successful
     */
    private boolean reconnectToNotificationsChannel() {
        stompNotificationChannel = reconnectToScanWebSocket(stompNotificationChannel, "Notifications", this.scanManagerDiscoveryUrl, new NotificationsStompSessionHandler());
        return StompChannel.isConnected(stompNotificationChannel);
    }

    // endregion reconnect channels

    /**
     * Manages the reconnection of a stomp channel channel
     *
     * @return the stomp channel modified
     */
    private StompChannel reconnectToScanWebSocket(StompChannel stompChannel, String channelLabel, String connectionUrl, CorrelatedStompSessionHandlerAdapter handler) {
        try {
            logger.trace("Connecting to {} websocket ...", channelLabel);
            stompChannel.setSession(subscribeToChannel(stompChannel.getClient(), connectionUrl, handler));
            logger.trace("Connected to {} websocket : ", channelLabel, stompChannel.getSession());
            logIfScanWasNotConnected(stompChannel.getSession());
            return stompChannel;
        } catch (InterruptedException | ExecutionException | ResourceAccessException | ConnectionLostException e) {
            if (logDisconnectedIfScanWasConnected()) {
                logger.warn("Error while connecting to Stomp Websocket, retrying ...");
                logger.trace("Error while connecting to Stomp Websocket, retrying ...", e);
            }
        } catch (Throwable e) {
            logDisconnectedIfScanWasConnected();
            logger.error("Unexpected exception at {} channel : {}", channelLabel, e.getMessage());
        }
        StompChannel.setSessionNull(stompChannel);
        return stompChannel;
    }

    /**
     * Log reconnection of CSL-Scan.
     *
     * @param session stomp session, verify that is not null
     */
    private void logIfScanWasNotConnected(StompSession session) {
        if (!isScanConnected && session != null && session.isConnected()) {
            logger.info("Connection recovered with CSLScan for STOMP WebSocket at {}", this.scanManagerDiscoveryUrl);
            isScanConnected = true;
        }
    }

    /**
     * Log disconnection of CSL-Scan.
     */
    private boolean logDisconnectedIfScanWasConnected() {
        if (isScanConnected) {
            logger.warn("Connection lost with CSLScan for STOMP WebSocket at {}", this.scanManagerDiscoveryUrl);
            isScanConnected = false;
        }
        return isScanConnected;
    }

    /**
     * Generic method to connect a channel in the web socket to CSL-Scan, and define the necessary callbacks (after connection and on message reception).
     * Blocks so should be called asynchronously.
     *
     * @param client  stomp websocket client
     * @param uri     uri of the channel
     * @param handler handler for the channel connection
     * @return The session we just created.
     * @throws ExecutionException   if connection failed.
     * @throws InterruptedException if connection was interrupted.
     * @throws TimeoutException     if connection timed out.
     */
    private StompSession subscribeToChannel(WebSocketStompClient client, String uri, StompSessionHandler handler) throws ExecutionException, InterruptedException, TimeoutException {
        // check if API is available
        if (!discoveryService.getScanApiHandler().getStatus().isSuccess()) {
            throw new ConnectionLostException("CSL-Scan disconnected");
        }

        try {
            return client.connectAsync(uri, handler).exceptionally(e -> null).get(1000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            if ((e.getCause() instanceof ConnectException) || (e.getCause() instanceof EofException) || (e.getCause() instanceof ClosedChannelException) || (e.getCause() instanceof TimeoutException)) {
                throw new ConnectionLostException("");
            }
            throw e;
        }
    }

    /**
     * Create the STOMP client with our custom message interpreter.
     *
     * @return a {@link WebSocketStompClient} suitable for our needs.
     */
    private WebSocketStompClient createStompClient() {

        WebSocketClient client = new StandardWebSocketClient();
        SockJsClient sockJsClient = new SockJsClient(Collections.singletonList(new WebSocketTransport(client)));
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

    // region Stomp Session Handlers

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
            ScanWebSocketHandler.handleTransportError(session, exception);
        }

        /**
         * Try to execute all the scan requests in the queue.
         */
        private void purgeScanRequestsQueue() {
            List<String> scanRequest;
            while (isScanConnected && (scanRequest = scanRequestsQueue.poll()) != null) {
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
            ScanWebSocketHandler.handleTransportError(session, exception);
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
            ScanWebSocketHandler.handleTransportError(session, exception);
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
            ScanWebSocketHandler.handleTransportError(session, exception);
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
            ScanWebSocketHandler.handleTransportError(session, exception);
        }
    }

    private static void handleTransportError(StompSession session, Throwable exception) {
        if (session.isConnected()) {
            session.disconnect();
        }
        if (exception.getCause() != null) {
            System.out.println(" >>> transport error cause " + exception.getCause());
        }
    }

    // endregion Stomp Session Handlers

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