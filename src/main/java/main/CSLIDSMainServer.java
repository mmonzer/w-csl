package main;

import com.csl.core.CSLContext;
import com.csl.core.NoLogging;
import com.csl.util.CorrelationUtils;
import main.services.*;


// JDK 17

public class CSLIDSMainServer {

    public static void main(String[] args) {
        initializeContext();
        registerServices();
        startServers();
    }

    /**
     * Initializes the CSLContext with the provided arguments and sets debug mode.
     */
    private static void initializeContext() {
        CorrelationUtils.setXCorrelationId();
        CorrelationUtils.setEndpoint("mainServer");

        // Disable Jetty logging
        org.eclipse.jetty.util.log.Log.setLog(new NoLogging());

        CSLContext.getInstance().init();
    }

    /**
     * Registers the necessary services with the JServiceLoader.
     */
    private static void registerServices() {
        boolean forwardToCSLClient = true;
        // concentrator services
        CSLContext.getInstance().registerHttpEndpoint(new AlertsService(), forwardToCSLClient);
        CSLContext.getInstance().registerHttpEndpoint(new MonitorService(), forwardToCSLClient);
        CSLContext.getInstance().registerHttpEndpoint(new TapsServices(), forwardToCSLClient);
        CSLContext.getInstance().registerHttpEndpoint(new DiscoveryServices(forwardToCSLClient), forwardToCSLClient);
        CSLContext.getInstance().registerHttpEndpoint(new StatusService(), forwardToCSLClient);
        CSLContext.getInstance().registerHttpEndpoint(new AutoCryptService(forwardToCSLClient), forwardToCSLClient);
    }

    /**
     * Initializes databases, HTTP server, UDP server, and other necessary components, and starts them.
     */
    private static void startServers() {
        CSLContext.getInstance().postInit(true);
        // Start the HTTP server and the Mqtt broker if enabled (Mqtt broker is not enabled by default)
        CSLContext.getInstance().startServers();
    }
}

