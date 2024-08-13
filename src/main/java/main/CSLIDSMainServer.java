package main;

import com.csl.core.CSLContext;
import com.csl.intercom.broker.MosquittoConfig;
import com.csl.intercom.jsoncmd.ApiGetHelp;
import com.csl.intercom.jsoncmd.JServiceLoader;
import com.ucsl.json.Json;
import com.xcsl.miniserver.ApiHttpServer;
import main.services.*;
import main.util.CSLRunningArgs;

import java.net.InetSocketAddress;

// JDK 17

public class CSLIDSMainServer {

    public static void main(String[] args) {
        initializeContext(args);
        registerServices();
        setRemoteServices();
        startServers();
    }

    /**
     * Initializes the CSLContext with the provided arguments and sets debug mode.
     *
     * @param args Command-line arguments passed to the application.
     */
    private static void initializeContext(String[] args) {
        System.out.println("Starting CSL IDS version " + CSLContext.VERSION);
        CSLContext.instance.init(new CSLRunningArgs().parseArgs(args).setHasIdsRunner(true));

        boolean useBroker = false;
        JServiceLoader.setModuleName("IDS", new MosquittoConfig().setUseBroker(useBroker));
    }

    /**
     * Registers the necessary services with the JServiceLoader.
     */
    private static void registerServices() {
        JServiceLoader.registerService(new CpeServices(), Json.object());
        JServiceLoader.registerService(new CveServices(), Json.object());
        JServiceLoader.registerService(new CSLServiceIDS(), Json.object());
        JServiceLoader.registerService(new AlertsService(), Json.object());
        JServiceLoader.registerService(new MonitorService(), Json.object());
        JServiceLoader.registerService(new TapsServices(), Json.object());
        JServiceLoader.registerService(new DiscoveryServices(false), Json.object());
        JServiceLoader.registerService(new StatusService(), Json.object());
        JServiceLoader.registerService(new AutoCryptService(true), Json.object());
    }

    /**
     * Sets the registered services as remote services, which will be forwarded to a socket.
     */
    private static void setRemoteServices() {
        CSLContext.instance.setApiRemote("ids");
        CSLContext.instance.setApiRemote("alerts");
        CSLContext.instance.setApiRemote("monitor");
        CSLContext.instance.setApiRemote("taps");
        CSLContext.instance.setApiRemote("discovery");
        CSLContext.instance.setApiRemote("status");
        CSLContext.instance.setApiRemote("autocrypt");
    }

    /**
     * Initializes databases, HTTP server, UDP server, and other necessary components, and starts them.
     */
    private static void startServers() {
        System.out.println(CSLContext.instance);
        CSLContext.instance.postInit(true, false);
        CSLContext.instance.startServers();
        startApiHttpServer();
    }

    /**
     * Starts the API HTTP server on port 9000.
     */
    private static void startApiHttpServer() {
        new ApiHttpServer().createServer(
                new InetSocketAddress(9000),
                JServiceLoader.getApiCommandsList(),
                new ApiGetHelp());
    }
}

