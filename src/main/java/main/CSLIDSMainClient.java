package main;

import com.csl.core.CSLContext;
import com.csl.core.Config;
import com.csl.core.NoLogging;
import com.csl.exceptions.ServiceNotReadyException;
import com.csl.intercom.dbapi.DbapiHandlerForCSLInit;
import com.csl.intercom.jsoncmd.ApiCommands;
import com.csl.intercom.jsoncmd.JServiceLoader;
import com.csl.logger.CSLApplicativeLogger;
import com.csl.util.CorrelationUtils;
import com.csl.web.CSLHttpServerJetty;
import com.csl.web.WebsocketClientEndpoint;
import com.csl.web.jcmdoversocket.IAlertForwarder;
import com.csl.web.websockets.CSLWebSocket;
import com.csl.web.websockets.IMessageBroadcaster;
import com.ucsl.json.Json;
import main.services.*;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

public class CSLIDSMainClient {

    private static final CSLApplicativeLogger logger = CSLApplicativeLogger.getLogger(CSLIDSMainClient.class);

    // Message broadcaster for WebSocket communication
    private static final IMessageBroadcaster messageBroadcaster = new IMessageBroadcaster() {

        @Override
        public void broadcastMessageString(String socketName, String message) {
            WebsocketClientEndpoint.sendMessageIfConnected("wss:" + socketName + ":" + message);
        }

        @Override
        public void broadcastMessageJson(String socketName, Json jsonMessage) {
            WebsocketClientEndpoint.sendMessageIfConnected("wsj:" + socketName + ":" + jsonMessage);
        }
    };

    // Alert forwarder for handling alerts
    private static final IAlertForwarder alertForwarder = alert ->         WebsocketClientEndpoint.sendMessageIfConnected("alert:" + alert);


    /**
     * Initializes services and registers them to the API command map.
     */
    public static void initServices() {
        for (ApiCommands api : JServiceLoader.getApiCommandsList()) {
            JServiceLoader.registerAPICommands(api);
        }
    }

    /**
     * gives the websocket url
     *
     * @return the websocket url
     */
    public static @NotNull String getWebSocketUrl() {
        Boolean useSsl = Config.instance.Client.getUseSsl();
        useSsl = (useSsl != null) && useSsl;
        String serverIp = Config.instance.Client.getIpServerRemote();
        serverIp = resolveHostNameIfRequired(serverIp, Config.instance.Client.getForceHostNameResolution());
        int serverPort = Config.instance.Client.getPortServerRemote();
        String serverUrlPrefix = Config.instance.Client.getServerRemoteUrlPrefix();
        serverUrlPrefix = (serverUrlPrefix != null) ? serverUrlPrefix : "";


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
    public static URI getWebSocketURI() {
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


        // Sends the list of supported API commands to the csl-server
        sendApiCommandsToDbapi();


        // Connect to the server using WebSocket and keep the connection alive
        WebsocketClientEndpoint.openWsConnectionWithCSLServer();
        // Start the servers and services of the csl_client
        startServers();

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
    }

    /**
     * Attempts to resolve the host name if the configuration requires it.
     */
    private static String resolveHostNameIfRequired(String ipAddress, boolean shouldForceHostNameResolution) {
        if (shouldForceHostNameResolution) {
            try {
                ipAddress = InetAddress.getByName(ipAddress).getHostAddress();
            } catch (UnknownHostException e) {
                logger.error("Error while resolving host name: {}", e.getMessage(), e);
            }
        }
        return ipAddress;
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
    private static void sendApiCommandsToDbapi() {
        boolean areCommandSent = false;

        // Try to send the commands
        try (DbapiHandlerForCSLInit dbapiHandler = new DbapiHandlerForCSLInit()) {
            while (!areCommandSent) {
                areCommandSent = tryToSendCommandList(dbapiHandler);

                // Wait
                if (wasInterruptedWhileWaiting(5)) return;
            }
            logger.info("Successfully sent API commands to the server.");
        } catch (Exception e) {
            logger.error("Error while sending API commands to the server, retrying ...");
        }
    }

    private static synchronized boolean wasInterruptedWhileWaiting(int seconds) {
        return wasInterruptedWhileWaiting(1F * seconds);
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

    private static boolean tryToSendCommandList(DbapiHandlerForCSLInit dbapiHandler) {
        boolean areCommandSent = false;
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