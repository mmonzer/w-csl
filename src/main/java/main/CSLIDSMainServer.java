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
     *
     * @param args Command-line arguments passed to the application.
     */
    private static void initializeContext() {
        CorrelationUtils.setXCorrelationId();
        CorrelationUtils.setEndpoint("mainServer");

        // Disable Jetty logging
        org.eclipse.jetty.util.log.Log.setLog(new NoLogging());

        CSLContext.instance.init();
    }

    /**
     * Registers the necessary services with the JServiceLoader.
     */
    private static void registerServices() {
        boolean forwardToCSLClient = true;
        // concentrator services
        CSLContext.instance.registerHttpEndpoint(new AlertsService(), forwardToCSLClient);
        CSLContext.instance.registerHttpEndpoint(new MonitorService(), forwardToCSLClient);
        CSLContext.instance.registerHttpEndpoint(new TapsServices(), forwardToCSLClient);
        CSLContext.instance.registerHttpEndpoint(new DiscoveryServices(forwardToCSLClient), forwardToCSLClient);
        CSLContext.instance.registerHttpEndpoint(new StatusService(), forwardToCSLClient);
        CSLContext.instance.registerHttpEndpoint(new AutoCryptService(forwardToCSLClient), forwardToCSLClient);
    }

    /**
     * Initializes databases, HTTP server, UDP server, and other necessary components, and starts them.
     */
    private static void startServers() {
        CSLContext.instance.postInit(true);
        // Start the HTTP server and the Mqtt broker if enabled (Mqtt broker is not enabled by default)
        CSLContext.instance.startServers();
    }
}

