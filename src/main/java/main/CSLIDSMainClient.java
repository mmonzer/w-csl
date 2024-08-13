package main;

import com.csl.alert.CSLAlertManager;
import com.csl.core.CSLContext;
import com.csl.core.Config;
import com.csl.core.NoLogging;
import com.csl.intercom.broker.MosquittoConfig;
import com.csl.intercom.dbapi.DbapiHandlerForCSLInit;
import com.csl.intercom.jsoncmd.ApiGetHelp;
import com.csl.intercom.jsoncmd.JServiceLoader;
import com.csl.web.jcmdoversocket.IAlertForwarder;
import com.csl.web.websockets.CSLWebSocket;
import com.csl.web.websockets.IMessageBroadcaster;
import main.services.*;
import main.util.CSLRunningArgs;
import main.xcom.WebsocketClientEndpoint;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import com.xcsl.miniserver.ApiHttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CSLIDSMainClient {

    private static final Logger logger = LoggerFactory.getLogger(CSLIDSMainClient.class);

    // Server configuration variables
    private static String serverIp = "127.0.0.1";
    private static String serverUrlPrefix = "";
    private static String apiKey = "";
    private static int serverPort = 8000;
    private static boolean useSsl = false;

    // WebSocket client endpoint
    private static WebsocketClientEndpoint clientEndPoint = null;

    // API Command map
    private static final HashMap<String, IApiCommands> apiMap = new HashMap<>();

    // Message broadcaster for WebSocket communication
    private static final IMessageBroadcaster messageBroadcaster = new IMessageBroadcaster() {

                @Override
        public void broadcastMessageString(String socketName, String message) {
            if (clientEndPoint != null && clientEndPoint.isOpen()) {
                clientEndPoint.sendMessage("wss:" + socketName + ":" + message);
            }
        }

                @Override
        public void broadcastMessageJson(String socketName, Json jsonMessage) {
            if (clientEndPoint != null && clientEndPoint.isOpen()) {
                clientEndPoint.sendMessage("wsj:" + socketName + ":" + jsonMessage);
            }
        }
    };

    // Alert forwarder for handling alerts
    private static final IAlertForwarder alertForwarder = new IAlertForwarder() {

        @Override
        public void sendAlert(Json alert) {
            logger.debug("Forwarding alert:\n{}", alert);
            if (clientEndPoint != null && clientEndPoint.isOpen()) {
                clientEndPoint.sendMessage("alert:" + alert);
            }
        }
    };

    /**
     * Initializes services and registers them to the API command map.
     */
    public static void initServices() {
        for (IApiCommands api : JServiceLoader.getApiCommandsList()) {
            String path = api.getName().toLowerCase();
            logger.info("Registering API: <{}>", path);
            apiMap.put(path, api);
        }
    }

    /***
     * Connects to the server at (serverUrl/cmd) TCP Socket, and maps the received commands over socket to the specific registered service
     * The messages received from the server are expected to follow the following format:
     * {
     *     api: <the service name>,
     *     jcmd: {
     *     		cmd: <command>,
     *     		params: {
     *				...
     *            }
     *     }
     * }
     * NOTE that each message is handled by a new thread
     */
    public static void connectToServer() {
        try {
            String wsProtocol = useSsl ? "wss" : "ws";
            String webSocketUrl = (serverPort > 0)
                    ? wsProtocol + "://" + serverIp + ":" + serverPort + serverUrlPrefix + "/cmd"
                    : wsProtocol + "://" + serverIp + serverUrlPrefix + "/cmd";

            logger.debug("Attempting to connect to WebSocket server at {} with API Key {}", webSocketUrl, apiKey);

            clientEndPoint = new WebsocketClientEndpoint(new URI(webSocketUrl), apiKey);

            if (!clientEndPoint.isOpen()) {
                logger.warn("Failed to connect to the server, retrying...");
                return;
                        } else {
                logger.info("Successfully connected to the server");
                        }

            // Add message handler
            clientEndPoint.addMessageHandler(messageString -> handleServerMessage(messageString.trim()));

            // Register available APIs with the server
            apiMap.keySet().forEach(apiName -> clientEndPoint.sendMessage("api:" + apiName));

            Thread.sleep(100);

        } catch (InterruptedException ex) {
            logger.error("InterruptedException: {}", ex.getMessage(), ex);
        } catch (URISyntaxException ex) {
            logger.error("URISyntaxException: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Handles messages received from the WebSocket server.
     *
     * @param messageString the raw message string received
     */
    private static void handleServerMessage(String messageString) {
        logger.trace("Received message: {}", messageString);

        if (messageString.startsWith("{") && messageString.endsWith("}")) {
            Json messageJson = Json.read(messageString);
            logger.trace("Parsed JSON message: {}", messageJson);

            String apiName = JsonUtil.getStringFromJson(messageJson, "api", "");

            Runnable task = () -> {
                Json result = Json.object().set("error", "api not found");

                if (!apiName.isEmpty()) {
                    IApiCommands api = apiMap.get(apiName);
                    Json jcmd = messageJson.get("jcmd");

                    if (jcmd != null && api != null) {
                        result = api.execJcmd(jcmd);
                    } else if (jcmd == null) {
                        result.set("error", "jcmd not found");
                    }
                }

                Json resultMessageJson = Json.object()
                        .set("uuid", messageJson.get("uuid"))
                        .set("result", result);

                logger.trace("Sending result: {}", resultMessageJson);
                clientEndPoint.sendMessage("res:" + resultMessageJson);
            };

            new Thread(task).start();
        }
    }

    /**
     * Starts tasks for maintaining the connection to the WebSocket server and keeping the connection alive.
     */
    public static void startRemoteConnectTask() {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

        // Reconnect task
        executorService.scheduleAtFixedRate(() -> {
            boolean reconnect = clientEndPoint == null || !clientEndPoint.isOpen();
            if (reconnect) {
                connectToServer();
                    }
        }, 0, 1, TimeUnit.SECONDS);

        // Keep-alive task
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
                        if (clientEndPoint != null && clientEndPoint.isOpen()) {
                            clientEndPoint.sendMessage("keep alive");
                        }
        }, 1, 5, TimeUnit.SECONDS);
    }

    public static void main(String[] args) {
        org.eclipse.jetty.util.log.Log.setLog(new NoLogging());

        Config config = Config.instance;
        CSLContext.instance.init(new CSLRunningArgs().parseArgs(args).setHasIdsRunner(true));

        configureClientSettings(config);

        boolean useBroker = false;
        JServiceLoader.setModuleName("IDS", new MosquittoConfig().setUseBroker(useBroker));

        registerServices();

        initServices();
        startRemoteConnectTask();
        CSLWebSocket.registerMessageBroadcaster(messageBroadcaster);

        CSLContext.instance.postInit(false, true);
        CSLContext.instance.startServers();

        CSLContext.instance.getIdsRunner().start();
        ((CSLAlertManager) CSLContext.instance.getCSLAlertManager()).registerAlertForwarder(alertForwarder);

        sendApiCommandsToServer();

        launchWebApiServerIfRequired(config);
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
        JServiceLoader.registerService(new CSLServiceIDS(), Json.object());
        JServiceLoader.registerService(new AlertsService(), Json.object());
        JServiceLoader.registerService(new MonitorService(), Json.object());
        JServiceLoader.registerService(new TapsServices(), Json.object());
        JServiceLoader.registerService(new DiscoveryServices(), Json.object());
        JServiceLoader.registerService(new StatusService(), Json.object());
        JServiceLoader.registerService(new AutoCryptService(), Json.object());
        JServiceLoader.registerService(new NmapServices(), Json.object());
    }

    /**
     * Sends the API commands to the server using the DbapiHandler.
     */
    private static void sendApiCommandsToServer() {
        try (DbapiHandlerForCSLInit dbapiHandler = new DbapiHandlerForCSLInit()) {
            dbapiHandler.sendCommandsList(JServiceLoader.getApiCommandsList());
        } catch (Exception e) {
            logger.error("Error while sending API commands to the server: {}", e.getMessage(), e);
        }
    }

    /**
     * Launches the Web API server if required by the configuration.
     *
     * @param config the configuration object
     */
    private static void launchWebApiServerIfRequired(Config config) {
        if (config.Client.getLaunchWebApiServer()) {
            int port = config.Client.getWebApiServerPort();
            new ApiHttpServer().createServer(
                new InetSocketAddress(port),
                JServiceLoader.getApiCommandsList(),
                new ApiGetHelp()
            );
        }
    }
}