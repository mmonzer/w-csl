package main;

import com.csl.core.CSLContext;
import com.csl.core.Config;
import com.csl.intercom.broker.MosquittoConfig;
import com.csl.intercom.jsoncmd.ApiGetHelp;
import com.csl.intercom.jsoncmd.JServiceLoader;
import com.ucsl.json.Json;
import com.xcsl.miniserver.ApiHttpServer;
import main.services.*;
import main.util.CSLRunningArgs;

import java.net.InetSocketAddress;

public class CSLIDSMainServer {

    static boolean START_MOSQUITTO = false;

    // JDK 11


    public static void main(String[] args) {

        System.out.println("Starting CSL IDS version  " + CSLContext.VERSION);
//        Json configObj = CSLContext.instance.getConfig();
        Config config = Config.instance;

        CSLContext.instance.init(new CSLRunningArgs().parseArgs(args).setHasIdsRunner(true));

        CSLContext.instance.setDebug(true);

        boolean USE_BROKER = false;
        JServiceLoader.setModuleName("IDS", new MosquittoConfig().setUseBroker(USE_BROKER));


//        JServiceLoader.registerService(new CSLServiceIDS(), configObj, true);
//        JServiceLoader.registerService(new AlertsService(), configObj, true);
//        JServiceLoader.registerService(new MonitorService(), configObj, true);
//        JServiceLoader.registerService(new TapsServices(), configObj, true);
//        JServiceLoader.registerService(new CSLServiceJsonDataBase(), configObj, true);
//        JServiceLoader.registerService(new CpeServices(), configObj, true);
//        JServiceLoader.registerService(new CveServices(), configObj, true);
//        JServiceLoader.registerService(new DiscoveryServices(false), configObj, true);
//        JServiceLoader.registerService(new StatusService(), configObj, true);
//        JServiceLoader.registerService(new AutoCryptService(true), configObj, true);

        JServiceLoader.registerService(new CSLServiceIDS(), Json.object(), true);
        JServiceLoader.registerService(new AlertsService(), Json.object(), true);
        JServiceLoader.registerService(new MonitorService(), Json.object(), true);
        JServiceLoader.registerService(new TapsServices(), Json.object(), true);
        JServiceLoader.registerService(new CpeServices(), Json.object(), true);
        JServiceLoader.registerService(new CveServices(), Json.object(), true);
        JServiceLoader.registerService(new DiscoveryServices(false), Json.object(), true);
        JServiceLoader.registerService(new StatusService(), Json.object(), true);
        JServiceLoader.registerService(new AutoCryptService(true), Json.object(), true);

        // set services as remote services (to be called through socket)
        CSLContext.instance.setApiRemote("ids");
        CSLContext.instance.setApiRemote("alerts");
        CSLContext.instance.setApiRemote("monitor");
        CSLContext.instance.setApiRemote("taps");
        CSLContext.instance.setApiRemote("discovery");
        CSLContext.instance.setApiRemote("status");
        CSLContext.instance.setApiRemote("autocrypt");

        // Init Databaseserver, httpserver, udpserver, ...

        System.out.println(CSLContext.instance);
        CSLContext.instance.postInit(true, false);
        CSLContext.instance.start();


        // FIXME: Client & Server are creating the same HTTP Server at the same port
        // Mini http server included in JAVA (for api)
        ApiHttpServer apiHttpServer = new ApiHttpServer().createServer(
                new InetSocketAddress(9000),
                JServiceLoader.getApiCommandsList(),
                new ApiGetHelp());

    }

}

