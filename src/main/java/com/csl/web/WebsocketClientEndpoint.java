package com.csl.web;

import com.csl.core.Config;
import com.csl.intercom.jsoncmd.ApiCommands;
import com.csl.intercom.jsoncmd.JServiceLoader;
import com.csl.logger.CSLNetworkLogger;
import com.csl.logger.LoggerConstants;
import com.csl.logger.LoggerCustomEndpoints;
import com.csl.logger.LoggerInterfaces;
import com.csl.util.JCmd;
import com.csl.util.ThreadUtils;
import com.csl.web.jcmdoversocket.CSLWebSocketForJcmd;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import jakarta.websocket.*;
import lombok.Setter;
import main.CSLIDSMainClient;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.csl.logger.LoggerConstants.*;
import static com.csl.web.jcmdoversocket.CSLWebSocketForJcmd.COMMAND;
import static com.ucsl.json.JsonUtil.getValueStringOrNull;

@ClientEndpoint(subprotocols = {"xsCrossfire"}, configurator = WebsocketClientEndpoint.Configurator.class)
public class WebsocketClientEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(WebsocketClientEndpoint.class);
    public static final String WEBSOCKET_CONNECTION = "websocket/connection";
    Session userSession = null;
    @Setter
    private MessageHandler messageHandler;
    private final URI endpointURI;
    private static final String APIKEY = Config.instance.Client.getApiKey();
    private static final WebSocketContainer container = ContainerProvider.getWebSocketContainer();
    private static final AtomicBoolean isConnecting = new AtomicBoolean(false);
    LocalDateTime lastConnectionDateTime = null;

    private static final ScheduledExecutorService reconnectWsExecutor = Executors.newSingleThreadScheduledExecutor();
    private static WebsocketClientEndpoint websocketClientInstance = null;

    public static class Configurator extends ClientEndpointConfig.Configurator {
        @Override
        public void beforeRequest(Map<String, List<String>> headers) {
            if (APIKEY != null) {
                headers.put("Authorization", List.of("Api-Key " + APIKEY));
            }
        }
    }

    public static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "";
        }

        String firstPart = apiKey.substring(0, 4); // First 4 characters
        String lastPart = apiKey.substring(apiKey.length() - 4); // Last 4 characters

        return firstPart + "****" + lastPart;
    }

    public WebsocketClientEndpoint(URI endpointURI) {
        this(endpointURI, null);
    }

    public WebsocketClientEndpoint(URI endpointURI, String apiKey) {
        this.endpointURI = endpointURI;
    }

    /**
     * Callback hook for Connection open events.
     *
     * @param userSession the userSession which is opened.
     */
    @OnOpen
    public synchronized void onOpen(Session userSession) {
        CSLNetworkLogger.info(logger, WEBSOCKET_CONNECTION, LoggerInterfaces.WS.toString(), "Opened websocket " + userSession.getRequestURI() +" : "+ userSession);
        logger.info("Connected to WCSL websocket {}", endpointURI);
        this.userSession = userSession;
        userSession.setMaxIdleTimeout(Config.instance.Server.getWebsocketTimeout());
        CSLNetworkLogger.debug(logger, WEBSOCKET_CONNECTION, LoggerInterfaces.WS.toString(), "Timeout = " + userSession.getMaxIdleTimeout() +" : "+ userSession);

        isConnecting.set(false);

        // register endpoints
        logger.info("Registering API endpoints with the server");
        JServiceLoader.apiMap.keySet().forEach(apiName -> sendMessageIfOpen("api:" + apiName));
        // TODO : clean  cyclic imports to send api names from here and not from the MainCLient.connectIfNecessary
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param userSession the userSession which is getting closed.
     * @param reason      the reason for connection close
     */
    @OnClose
    public synchronized void onClose(Session userSession, CloseReason reason) {
        CSLNetworkLogger.warn(logger, WEBSOCKET_CONNECTION, LoggerInterfaces.WS.toString(), "Closing websocket " + userSession.getRequestURI()+ " Reason:" + reason.getReasonPhrase() +" : "+ userSession);
        this.userSession = null;
        isConnecting.set(false);
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param session the userSession that got an error.
     * @param error      throwable
     */
    @OnError
    public synchronized void onError(Session session, Throwable error) {
        logger.error("Connection lost in WS with server with error : {}",(error.getMessage()!=null)?error.getMessage(): "unknown");
        this.userSession = null;
        isConnecting.set(false);
    }

    /**
     * Callback hook for Message Events. This method will be invoked when a client send a message.
     *
     * @param message The text message
     */
    @OnMessage
    public void onMessage(String message) {
        if (this.messageHandler != null) {
            this.messageHandler.handleMessage(message);
        }
    }

    /**
     * Send a message.
     *
     * @param message message to send
     */
    public void sendMessage(String message) {
        this.userSession.getAsyncRemote().sendText(message);
    }

    /**
     * Send a message if the websocket is open
     *
     * @param message to send
     */
    public void sendMessageIfOpen(String message) {
        if (isOpen()) {
            this.sendMessage(message);
        }
    }

    public void connect() {
        synchronized (WebsocketClientEndpoint.class) { // Ensures thread-safety
            logger.debug("Attempting to connect to WebSocket server at {} with API Key {}", endpointURI, maskApiKey(APIKEY));

            if (isOpen()) {
                logger.info("WS is already connected, skipping reconnect");
                return;
            }
            LocalDateTime currentDateTime =  LocalDateTime.now();
            if (lastConnectionDateTime!=null && Duration.between(lastConnectionDateTime, currentDateTime).getSeconds()<10) {
                logger.info("WebSocket connection cooling down, skipping reconnect.");
                return;
            }

            if (!isConnecting.compareAndSet(false, true)) {
                logger.info("WebSocket connection is already in progress, skipping reconnect.");
                return;
            }

            try {
                logger.info("Attempting to open websocket at {}", endpointURI);
                lastConnectionDateTime = currentDateTime;
                this.userSession = container.connectToServer(this, endpointURI);
            } catch (Exception e) {
                isConnecting.set(false); // Reset the flag regardless of success or failure
                logger.warn("Exception occurred when connecting to WebSocket {}: {}", endpointURI, e.getMessage());
            }
        }
    }

    /**
     * Connects to the server at (serverUrl/cmd) TCP Socket, and maps the received commands over socket to the specific registered service
     * The messages received from the server are expected to follow the following format:
     * {
     * api: <the service name>,
     * jcmd: {
     * cmd: <command>,
     * params: {
     * ...
     * }
     * }
     * }
     * NOTE that each message is handled by a new thread
     */
    public void connectToServerIfRequired() {
        if (this.isOpen()) {
            return;
        }

        this.connect();
    }

    public boolean isOpen() {
        if (userSession == null) {
            return false;
        }
        try {
            return userSession.isOpen();
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Handles messages received from the WebSocket server.
     *
     * @param messageString the raw message string received
     */
    public void handleServerMessage(String messageString) {
        new Thread(() -> {
            logger.trace("Received message: {}", messageString);

            if (messageString.startsWith("{") && messageString.endsWith("}")) {
                Json messageJson = Json.read(messageString);
                getValueStringOrNull(messageJson, CSLWebSocketForJcmd.ID);
                String xCorrelationId = getValueStringOrNull(messageJson, X_CORRELATION_ID);
                MDC.put(X_CORRELATION_ID, xCorrelationId);
                String uri = "";

                String apiName = JsonUtil.getStringFromJson(messageJson, "api", "");

                Json result = Json.object().set("error", "api not found");

                if (!apiName.isEmpty()) {
                    ApiCommands api = JServiceLoader.apiMap.get(apiName);
                    MDC.put(ENDPOINT, apiName);
                    Json jsonCommand = messageJson.get("jsonCommand");
                    uri = "/" + apiName + "/" + jsonCommand.get(JCmd.CMD).asString();
                    MDC.put(ENDPOINT, uri);

                    CSLNetworkLogger.infoInboundRequest(logger, Config.instance.Client.getIpServerRemote(), Config.instance.Client.getPortServerRemote(), "", uri, "WS", LoggerConstants.WS_REQUEST_RECV);

                    if (jsonCommand != null && api != null) {
                        result = api.execJcmd(jsonCommand);
                    } else if (jsonCommand == null) {
                        result.set("error", "jsonCommand not found");
                    }
                } else {
                    logger.warn("API endpoint not found");
                }


                Json resultMessageJson = Json.object()
                        .set("uuid", messageJson.get("uuid"))
                        .set(X_CORRELATION_ID, xCorrelationId)
                        .set("result", result);

                logger.trace("Sending result: {}", resultMessageJson);
                this.sendMessageIfOpen("res:" + resultMessageJson);
                CSLNetworkLogger.infoOutboundResponse(logger, Config.instance.Client.getIpServerRemote(), Config.instance.Client.getPortServerRemote(), "", uri, "WS", 0, LoggerConstants.WS_RESPONSE_SENT);
                MDC.remove(COMMAND);
                MDC.remove(ENDPOINT);
                MDC.remove(X_CORRELATION_ID);
                MDC.remove(PROTOCOL);
            }
        }).start();
    }

    // region static code : init, launch threads and send message to singleton
    /**
     * Connects to the server at (serverUrl/cmd) TCP Socket, and maps the received commands over socket to the specific registered service
     * The messages received from the server are expected to follow the following format:
     * {
     * api: <the service name>,
     * jcmd: {
     * cmd: <command>,
     * params: {
     * ...
     * }
     * }
     * }
     * NOTE that each message is handled by a new thread
     */
    public static @NotNull WebsocketClientEndpoint initWebSocketClient() {
        WebsocketClientEndpoint websocketClient = new WebsocketClientEndpoint(CSLIDSMainClient.getWebSocketURI());
        websocketClient.setMessageHandler(messageString -> websocketClient.handleServerMessage(messageString.trim()));
        return websocketClient;
    }

    /**
     * Starts tasks for maintaining the connection to the WebSocket server and keeping the connection alive.
     */
    public static void openWsConnectionWithCSLServer() {
        websocketClientInstance = WebsocketClientEndpoint.initWebSocketClient();

        // Reconnect task
        ThreadUtils.uncorrelatedSingleThreadScheduledAtFixedRate(
                reconnectWsExecutor,
//                CSLIDSMainClient::connectToServerIfRequired,
                ()->websocketClientInstance.connectToServerIfRequired(),
                0, 5, TimeUnit.SECONDS,
                LoggerCustomEndpoints.RECONNECT_WS_CSL, LoggerInterfaces.CSL_CLIENT);

        // Shutdown hook to clean up the executor
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            reconnectWsExecutor.shutdown();
            try {
                if (!reconnectWsExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    reconnectWsExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                reconnectWsExecutor.shutdownNow();
            }
        }));

        // Keep-alive task
        ThreadUtils.uncorrelatedSingleThreadScheduledAtFixedRate(Executors.newSingleThreadScheduledExecutor(),
                () -> {
                    synchronized (websocketClientInstance) {
                        if (websocketClientInstance != null) {
                            websocketClientInstance.sendMessageIfOpen("keep alive");
                        }
                    }
                },
                1, 5, TimeUnit.SECONDS,
                LoggerCustomEndpoints.KEEP_ALIVE_WS_CSL, LoggerInterfaces.CSL_CLIENT);
    }

    /**
     * Sends message to the websocket instance if exists and if opened.
     * @param message message to send
     */
    public static void sendMessageIfConnected(String message) {
        if (websocketClientInstance != null) {
            websocketClientInstance.sendMessageIfOpen(message);
        }
    }
    // endregion static code : init, launch threads and send message to singleton

    /**
     * Message handler.
     *
     * @author Jiji_Sasidharan
     */
    public interface MessageHandler {

        public void handleMessage(String message);
    }
}