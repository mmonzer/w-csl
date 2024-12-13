package main;

import com.csl.core.CSLContext;
import com.csl.core.Config;
import com.csl.core.NoLogging;
import com.csl.exceptions.ServiceNotReadyException;
import com.csl.intercom.dbapi.DbapiHandlerForCSLInit;
import com.csl.intercom.jsoncmd.ApiCommands;
import com.csl.intercom.jsoncmd.JServiceLoader;
import com.csl.logger.*;
import com.csl.util.CorrelationUtils;
import com.csl.util.JCmd;
import com.csl.util.ThreadUtils;
import com.csl.web.CSLHttpServerJetty;
import com.csl.web.WebsocketClientEndpoint;
import com.csl.web.jcmdoversocket.CSLWebSocketForJcmd;
import com.csl.web.jcmdoversocket.IAlertForwarder;
import com.csl.web.websockets.CSLWebSocket;
import com.csl.web.websockets.IMessageBroadcaster;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import main.services.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.csl.logger.LoggerConstants.*;
import static com.csl.web.jcmdoversocket.CSLWebSocketForJcmd.COMMAND;
import static com.ucsl.json.JsonUtil.getValueStringOrNull;

public class CSLIDSMainClient {

    private static final CSLApplicativeLogger logger = CSLApplicativeLogger.getLogger(CSLIDSMainClient.class);

    // Server configuration variables
    private static String serverIp = "127.0.0.1";
    private static String serverUrlPrefix = "";
    private static String apiKey = "";
    private static int serverPort = 8000;
    private static boolean useSsl = false;

    private static final Object lock1 = new Object();
    private static final Object lock2 = new Object();

    // WebSocket client endpoint
    private static WebsocketClientEndpoint clientEndPoint = null;

    // API Command map
    private static final HashMap<String, ApiCommands> apiMap = new HashMap<>();

    // Message broadcaster for WebSocket communication
    private static final IMessageBroadcaster messageBroadcaster = new IMessageBroadcaster() {

        @Override
        public void broadcastMessageString(String socketName, String message) {
            if (clientEndPoint != null) {
                clientEndPoint.sendMessageIfOpen("wss:" + socketName + ":" + message);
            }
        }

        @Override
        public void broadcastMessageJson(String socketName, Json jsonMessage) {
            if (clientEndPoint != null) {
                clientEndPoint.sendMessageIfOpen("wsj:" + socketName + ":" + jsonMessage);
            }
        }
    };

    // Alert forwarder for handling alerts
    private static final IAlertForwarder alertForwarder = alert -> {
        logger.debug("Forwarding alert:\n{}", alert);
        if (clientEndPoint != null) {
            clientEndPoint.sendMessageIfOpen("alert:" + alert);
        }
    };

    /**
     * Initializes services and registers them to the API command map.
     */
    public static void initServices() {
        for (ApiCommands api : JServiceLoader.getApiCommandsList()) {
            String path = api.getName().toLowerCase();
            logger.info("Registering API: <{}>", path);
            apiMap.put(path, api);
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
    public static @NotNull WebsocketClientEndpoint initWebSocketClient() {
        WebsocketClientEndpoint websocketClient = new WebsocketClientEndpoint(getWebSocketURI(), apiKey);
        websocketClient.setMessageHandler(messageString -> handleServerMessage(messageString.trim()));
        return websocketClient;
    }

    /**
     * gives the websocket url
     *
     * @return the websocket url
     */
    private static @NotNull String getWebSocketUrl() {
        String wsProtocol = useSsl ? "wss" : "ws";
        return (serverPort > 0)
                ? wsProtocol + "://" + serverIp + ":" + serverPort + serverUrlPrefix + "/cmd"
                : wsProtocol + "://" + serverIp + serverUrlPrefix + "/cmd";
    }

    /**
     * gives the websocket URI or default "ws://wrongURI" if syntax error
     *
     * @return the websocket URI or default "ws://wrongURI" if syntax error
     */
    private static URI getWebSocketURI() {
        try {
            return new URI(getWebSocketUrl());
        } catch (URISyntaxException e) {
            logger.error("Wrong syntax in socket URI {} : {}", getWebSocketUrl(), e.getMessage());
            try {
                return new URI("ws://wrongURI");
            } catch (URISyntaxException e2) {
                return null; // never reached
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
    public static synchronized void connectToServer() {
        logger.debug("Attempting to connect to WebSocket server at {} with API Key {}", getWebSocketUrl(), apiKey);


        if (clientEndPoint.isOpen()) {
            return;
        }

        clientEndPoint.connect();

        if (!clientEndPoint.isOpen()) {
            return;
        }

        // register endpoints
        apiMap.keySet().forEach(apiName -> clientEndPoint.sendMessageIfOpen("api:" + apiName));
    }

    /**
     * Handles messages received from the WebSocket server.
     *
     * @param messageString the raw message string received
     */
    private static void handleServerMessage(String messageString) {
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
                    ApiCommands api = apiMap.get(apiName);
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
                clientEndPoint.sendMessageIfOpen("res:" + resultMessageJson);
                CSLNetworkLogger.infoOutboundResponse(logger, Config.instance.Client.getIpServerRemote(), Config.instance.Client.getPortServerRemote(), "", uri, "WS", 0, LoggerConstants.WS_RESPONSE_SENT);
                MDC.remove(COMMAND);
                MDC.remove(ENDPOINT);
                MDC.remove(X_CORRELATION_ID);
                MDC.remove(PROTOCOL);
            }
        }).start();
    }

    /**
     * Starts tasks for maintaining the connection to the WebSocket server and keeping the connection alive.
     */
    public static void openWsConnectionWithCSLServer() {
        clientEndPoint = initWebSocketClient();

        // Reconnect task
        ThreadUtils.uncorrelatedSingleThreadScheduledAtFixedRate(
                Executors.newSingleThreadScheduledExecutor(),
                CSLIDSMainClient::connectToServer,
                0, 5, TimeUnit.SECONDS,
                LoggerCustomEndpoints.RECONNECT_WS_CSL, LoggerInterfaces.CSL_CLIENT);

        // Keep-alive task
        ThreadUtils.uncorrelatedSingleThreadScheduledAtFixedRate(Executors.newSingleThreadScheduledExecutor(),
                () -> {
                    synchronized (lock2) {
                        if (clientEndPoint != null) {
                            clientEndPoint.sendMessageIfOpen("keep alive");
                        }
                    }
                },
                1, 5, TimeUnit.SECONDS,
                LoggerCustomEndpoints.KEEP_ALIVE_WS_CSL, LoggerInterfaces.CSL_CLIENT);
    }

    /**
     * Initializes the CSLContext with the provided arguments and sets debug mode.
     */
    private static void initializeContext() {
        CorrelationUtils.setXCorrelationId();
        CorrelationUtils.setEndpoint("mainClient");

        // Disable Jetty logging
        org.eclipse.jetty.util.log.Log.setLog(new NoLogging());

        Config config = Config.instance;
        CSLContext.getInstance().init();
        configureClientSettings(config);

        // Provide the callback method for the
        CSLWebSocket.registerMessageBroadcaster(messageBroadcaster);
        CSLContext.getInstance().getCSLAlertManager().registerAlertForwarder(alertForwarder);
    }

    public static void main(String[] args) {
        initializeContext();
        registerServices();
        initServices();

        // Connect to the server using WebSocket and keep the connection alive
        openWsConnectionWithCSLServer();
        // Start the servers and services of the csl_client
        startServers();

        // Sends the list of supported API commands to the csl-server
        sendApiCommandsToServer();

        // Launch the Web API server if required by the configuration (for testing purposes)
        launchWebApiServerIfRequired(Config.instance);
    }

    /**
     * Initializes databases, HTTP server, UDP server, and other necessary components, and starts them.
     */
    private static void startServers() {
        // Start the servers
        CSLContext.getInstance().postInit(false);
        // Start the UDP Server (To receive IDS Alerts) and the task executor
        CSLContext.getInstance().startServers();
    }

    /**
     * Configures client settings based on the provided configuration.
     *
     * @param config the configuration object
     */
    private static void configureClientSettings(Config config) {
        // Override server configuration for the client
        config.Server.setOn(false);
        config.UdpServerConf.setOn(true);

        serverIp = config.Client.getIpServerRemote();
        serverUrlPrefix = config.Client.getServerRemoteUrlPrefix();
        serverUrlPrefix = (serverUrlPrefix == null) ? "" : serverUrlPrefix;

        resolveHostNameIfRequired(config);

        serverPort = config.Client.getPortServerRemote();
        useSsl = config.Client.getUseSsl();
        apiKey = config.Client.getApiKey();
        logger.trace("API Key: {}", apiKey);
    }

    /**
     * Attempts to resolve the host name if the configuration requires it.
     *
     * @param config the configuration object
     */
    private static void resolveHostNameIfRequired(Config config) {
        if (config.Client.getForceHostNameResolution()) {
            try {
                serverIp = InetAddress.getByName(serverIp).getHostAddress();
            } catch (UnknownHostException e) {
                logger.error("Error while resolving host name: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Registers the required services with the service loader.
     */
    private static void registerServices() {
        boolean forwardToCSLClient = false;
        CSLContext.getInstance().registerHttpEndpoint(new AlertsService(), forwardToCSLClient);
        CSLContext.getInstance().registerHttpEndpoint(new MonitorService(), forwardToCSLClient);
        CSLContext.getInstance().registerHttpEndpoint(new TapsServices(), forwardToCSLClient);
        CSLContext.getInstance().registerHttpEndpoint(new DiscoveryServices(), forwardToCSLClient);
        CSLContext.getInstance().registerHttpEndpoint(new StatusService(), forwardToCSLClient);
        CSLContext.getInstance().registerHttpEndpoint(new AutoCryptService(), forwardToCSLClient);
    }

    /**
     * Sends the API commands to the server using the DbapiHandler. It retries to send the Commands every 5 seconds till
     * the service is reachable. This method creates a thread that finishes when the command list is sent to the DBapi.
     */
    private static void sendApiCommandsToServer() {
        Object monitorObj = new Object(); // in static methods we need an object that works as lock for the synchronized thread block.

        Executors.newSingleThreadExecutor().submit(() -> {
            boolean areCommandSent = false;

            // Try to send the commands
            try (DbapiHandlerForCSLInit dbapiHandler = new DbapiHandlerForCSLInit()) {
                while (!areCommandSent) {
                    areCommandSent = tryToSendCommandList(dbapiHandler, areCommandSent);

                    // Wait
                    if (wasInterruptedWhileWaiting(5)) return;
                }
            } catch (Exception e) {
                logger.error("Error while sending API commands to the server, retrying ...");
            }
        });
    }

    private static synchronized boolean wasInterruptedWhileWaiting(int seconds) {
       return wasInterruptedWhileWaiting(1F*seconds);
    }

    private static synchronized boolean wasInterruptedWhileWaiting(float seconds) {
        try {
            Thread.sleep((long) seconds * 1000L);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return true;
        }
    }

    private static boolean tryToSendCommandList(DbapiHandlerForCSLInit dbapiHandler, boolean areCommandSent) {
        try {
            dbapiHandler.sendCommandsList(JServiceLoader.getApiCommandsList());
            areCommandSent = true;
        } catch (ServiceNotReadyException e) {
            logger.error("Error while sending API commands to the server, retrying ...");
        }
        return areCommandSent;
    }

    /**
     * Launches the Web API server if required by the configuration.
     *
     * @param config the configuration object
     */
    private static void launchWebApiServerIfRequired(Config config) {
        if (config.Client.getLaunchWebApiServer()) {
            CSLHttpServerJetty server = new CSLHttpServerJetty();
            server.initServer(config.Client);
            server.start();
        }
    }
}