package main;

import com.csl.core.CSLContext;
import com.csl.core.NoLogging;
import com.csl.intercom.jsoncmd.JServiceLoader;
import com.ucsl.json.Json;
import main.services.*;
import main.util.CSLRunningArgs;


// JDK 17

public class CSLIDSMainServer {

    public static void main(String[] args) {
        initializeContext(args);
        registerServices();
        startServers();
    }

    /**
     * Initializes the CSLContext with the provided arguments and sets debug mode.
     *
     * @param args Command-line arguments passed to the application.
     */
    private static void initializeContext(String[] args) {
        // Disable Jetty logging
        org.eclipse.jetty.util.log.Log.setLog(new NoLogging());
        System.out.println("Starting CSL IDS version " + CSLContext.VERSION);
        CSLContext.instance.init(new CSLRunningArgs().parseArgs(args).setHasIdsRunner(true));
    }

    /**
     * Registers the necessary services with the JServiceLoader.
     */
    private static void registerServices() {
        boolean forwardToCSLClient = true;
        // remote services
        CSLContext.instance.registerHttpEndpoint(new CpeServices(), !forwardToCSLClient);
        CSLContext.instance.registerHttpEndpoint(new CveServices(), !forwardToCSLClient);
        CSLContext.instance.registerHttpEndpoint(new NmapServices(), !forwardToCSLClient);
        // concentrator services
        CSLContext.instance.registerHttpEndpoint(new CSLServiceIDS(), forwardToCSLClient);
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
        System.out.println(CSLContext.instance);
        CSLContext.instance.postInit(true);
        // Start the HTTP server and the Mqtt broker if enabled (Mqtt broker is not enabled by default)
        CSLContext.instance.startServers();
    }

}

