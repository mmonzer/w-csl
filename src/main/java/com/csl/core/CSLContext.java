package com.csl.core;

import com.csl.alert.CSLAlertManager;
import com.csl.intercom.broker.CSLMqttBrokerHandler;
import com.csl.intercom.jsoncmd.JServiceLoader;
import com.csl.intercom.status.StatusNotifier;
import com.csl.web.CSLHttpServerJetty;
import com.csl.web.CSLUDPServer;
import lombok.Getter;
import lombok.Setter;
import main.services.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSLContext {
    /**
     * Logger instance for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(CSLContext.class);

    @Getter
    private String cslConfDir = "";


    private static final CSLContext INSTANCE = new CSLContext();

    /**
     * Indicates if the context is running as a server (true), client (false), or uninitialized (null).
     */
    private Boolean isServer = null;

    /**
     * Instance of the alert manager.
     */
    private CSLAlertManager cslAlertManager = null;



    private CSLMqttBrokerHandler mqttBroker = null;

    private StatusNotifier statusNotifier = null;

    /**
     * Instance of the HTTP server.
     */
    private CSLHttpServerJetty cslHttpServer = null;

    /**
     * Instance of the UDP server.
     */
    private CSLUDPServer cslUDPServer = null;

    private long initialTime = 0;

    @Getter
    @Setter
    private boolean testMode = false;

    /**
     * Private constructor for singleton pattern.
     */
    private CSLContext() {
        // Singleton instance
    }

    public static CSLContext getInstance() { return INSTANCE;}

    /**
     * Prints an error message to the standard error stream.
     *
     * @param message The error message to print.
     */
    public void printError(String message) {
        logger.error(message);
    }

    /**
     * Gets the user directory.
     *
     * @return The user directory as a string.
     */
    public String getUserDir() {
        return JServiceLoader.getUserDir();
    }

    /**
     * Gets the alert manager instance, logging a warning if not registered.
     *
     * @return The alert manager instance.
     */
    public CSLAlertManager getCSLAlertManager() {
        if (cslAlertManager == null) {
            logger.warn("Warning, no alertManager registered");
        }
        return cslAlertManager;
    }

    /**
     * Gets the HTTP server instance, logging a warning if not registered.
     *
     * @return The HTTP server instance.
     */
    public CSLHttpServerJetty getCslHttpServer() {
        if (cslHttpServer == null) {
            logger.warn("Warning, no CSL HTTP server registered");
        }
        return cslHttpServer;
    }

    /**
     * Sets the HTTP server instance.
     *
     * @param cslHttpServer The HTTP server instance to set.
     */
    private void setCslHttpServer(CSLHttpServerJetty cslHttpServer) {
        this.cslHttpServer = cslHttpServer;
    }

    /**
     * Gets the UDP server instance, logging a warning if not registered.
     *
     * @return The UDP server instance.
     */
    public CSLUDPServer getCslUDPServer() {
        if (cslUDPServer == null) {
            logger.warn("Warning, no CSL UDP server registered");
        }
        return cslUDPServer;
    }

    /**
     * Sets the UDP server instance.
     *
     * @param cslUDPServer The UDP server instance to set.
     */
    private void setCslUDPServer(CSLUDPServer cslUDPServer) {
        this.cslUDPServer = cslUDPServer;
    }

    /**
     * Builds the full path within the user directory.
     *
     * @param dir The directory to build the path for.
     * @return The full path as a string.
     */
    public String buildFullPathInUserDir(String dir) {
        return JServiceLoader.buildFullPathInUserDir(dir);
    }

    /**
     * Gets the current system time, accounting for replay mode if enabled.
     *
     * @return The current system time in milliseconds.
     */
    public long getSystemCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Gets the configuration instance.
     *
     * @return The configuration instance.
     */
    public Config getConfig() {
        if (Config.getInstance().server == null) {
            logger.error("Invalid config file, update to new format");
            System.exit(0);
        }
        return Config.getInstance();
    }

    /**
     * Initializes the CSL context with the default running arguments.
     */
    public void init() {
        getConfig();

        this.cslConfDir = buildFullPathInUserDir("cslconf");

        org.eclipse.jetty.util.log.Log.setLog(new com.csl.core.NoLogging());

        cslAlertManager = new CSLAlertManager(getConfig().alertViewer);

        setCslHttpServer(new CSLHttpServerJetty());
        setCslUDPServer(new CSLUDPServer());
    }

    public boolean registerHttpEndpoint(Service cslService, boolean executeInCSLClient) {
        boolean initialized = JServiceLoader.registerService(cslService);

        if (getCslHttpServer() == null) {
            return false;
        }
        if (executeInCSLClient) {
            getCslHttpServer().registerHttpEndpoint(cslService.getName());
        }

        return initialized;
    }

    /**
     * Performs post-initialization tasks, setting up the server and/or client configurations.
     *
     * @param isServer Indicates if the context should operate as a server (true), client (false), or uninitialized (null).
     */
    public void postInit(Boolean isServer) {
        this.isServer = isServer;

        if (isServer == null) {
            logger.error("CSLContext not initialized as server or client");
            return;
        }

        if (isServer) {
            getCslHttpServer().initServer(Config.getInstance().server);
            getStatusNotifier().setSendNotifications(false);
        } else {
            getCslUDPServer().initUDPServer(Config.getInstance().udpServerConf);
            initTime();
        }
    }

    /**
     * Starts the HTTP and UDP servers.
     */
    public void startServers() {
        if (isServer == null) {
            logger.error("CSLContext not initialized as server or client, cannot start servers");
            return;
        }

        if (isServer) {
            getCslHttpServer().start();
        } else {
            getCslUDPServer().start();
        }

        JServiceLoader.getCSLInterModuleCommunicationManager().start();
    }

    /**
     * Initializes the timing settings for module execution.
     */
    private void initTime() {
        initialTime = getSystemCurrentTimeMillis();
    }

    /**
     * Gets the MQTT broker handler.
     *
     * @return The MQTT broker handler.
     */
    public CSLMqttBrokerHandler getMqttBroker() {
        if (mqttBroker == null) {
            mqttBroker = new CSLMqttBrokerHandler(getConfig());
        }
        return mqttBroker;
    }

    /**
     * Gets the status notifier.
     *
     * @return The status notifier.
     */
    public StatusNotifier getStatusNotifier() {
        if (statusNotifier == null) {
            statusNotifier = new StatusNotifier(false);
        }
        return statusNotifier;
    }
}
