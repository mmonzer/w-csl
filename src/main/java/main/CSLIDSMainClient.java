package main;

import com.csl.alert.CSLAlertManager;
import com.csl.core.CSLContext;
import com.csl.core.Config;
import com.csl.core.NoLogging;
import com.csl.intercom.broker.MosquittoConfig;
import com.csl.intercom.dbapi.DbapiHandler;
import com.csl.intercom.dbapi.DbapiHandlerForCSLInit;
import com.csl.intercom.dbapi.DbapiHandlerForCSLScan;
import com.csl.intercom.jsoncmd.ApiGetHelp;
import com.csl.intercom.jsoncmd.JServiceLoader;
import com.csl.web.jcmdoversocket.IAlertForwarder;
import com.csl.web.websockets.CSLWebSocket;
import com.csl.web.websockets.IMessageBroadcaster;
import main.xcom.WebsocketClientEndpoint;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import com.xcsl.miniserver.ApiHttpServer;
import main.services.*;
import main.util.CSLRunningArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CSLIDSMainClient {

    private static final Logger logger = LoggerFactory.getLogger(CSLIDSMainClient.class);
    static String SERVER_IP = "127.0.0.1";
    static String SERVER_URL_PREFIX = "";
    static String API_KEY = "";
    static int SERVER_PORT = 8000;
    static boolean USE_SSL = false;

    static WebsocketClientEndpoint clientEndPoint = null;

    static HashMap<String, IApiCommands> apiMap = new HashMap<String, IApiCommands>();

    static IMessageBroadcaster messageBroadcaster =
            new IMessageBroadcaster() {

                @Override
                public void broadcastMessageString(String socketName, String s) {
                    // TODO Auto-generated method stub

                    if (clientEndPoint != null) {
                        if (!clientEndPoint.isOpen()) return;
                        clientEndPoint.sendMessage("wss:" + socketName + ":" + s);
                    }
                }

                @Override
                public void broadcastMessageJson(String socketName, Json j) {

                    if (clientEndPoint != null) {
                        if (!clientEndPoint.isOpen()) return;
                        clientEndPoint.sendMessage("wsj:" + socketName + ":" + j);
                    }

                }
            };

    static IAlertForwarder alertForwarder = new IAlertForwarder() {

        @Override
        public void sendAlert(Json alert) {
           logger.debug("********Forward alert:\n" + alert + "\n*************");
            if (clientEndPoint != null) {
                if (!clientEndPoint.isOpen()) return;
                clientEndPoint.sendMessage("alert:" + alert);
            }

        }
    };


    /***
     * Adds the < serviceName, service > to the apiMap hashmap from the JServiceLoader's commandsList (listOfAPIToRegister)
     */
    static public void iniServices() {

        for (IApiCommands api : JServiceLoader.getApiCommandsList()) {

            String path = api.getName();
            logger.info("REGISTER API:<" + path + ">");
            apiMap.put(path.toLowerCase(), api);
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
    static public void connectToServer() {
        try {
            String wsProtocol = USE_SSL ? "wss" : "ws";
            String s = null;
            if (SERVER_PORT > 0) {
                s = wsProtocol + "://" + SERVER_IP + ":" + SERVER_PORT + SERVER_URL_PREFIX + "/cmd";
            } else
                s = wsProtocol + "://" + SERVER_IP + SERVER_URL_PREFIX + "/cmd";

            logger.debug("Try to connect to WS server {} with API Key {}", s, API_KEY);

            clientEndPoint = new WebsocketClientEndpoint(new URI(s), API_KEY);
            if (!clientEndPoint.isOpen()) {
                logger.warn("Connection to server failed, retrying...");
                return;
            } else
                logger.info("Connected to server");

            // add listener
            clientEndPoint.addMessageHandler(messageString -> {
                logger.debug("MESSAGE:" + messageString);
                messageString = messageString.trim();
                if (messageString.startsWith("{") && messageString.endsWith("}")) {

                    Json messageJson = Json.read(messageString);
                    logger.debug("received:" + messageJson);

                    String apiname = JsonUtil.getStringFromJson(messageJson, "api", "");

                    Runnable r = () -> {
                        Json result = Json.object().set("error", "api not found ");

                        if (apiname.isEmpty()) {

                        } else {

                            IApiCommands api = apiMap.get(apiname);
                            Json jcmd = messageJson.get("jcmd");
                            if (jcmd == null) result.set("error", "jcmd not found");

                            if ((api != null) && (jcmd != null)) result = api.execJcmd(jcmd);
                        }


                        Json resultMessageJson = Json.object();
                        resultMessageJson.set("uuid", messageJson.get("uuid"));
                        resultMessageJson.set("result", result);
                        logger.debug("****RESULT:" + resultMessageJson);
                        clientEndPoint.sendMessage("res:" + resultMessageJson);

                    };

                    Thread t = new Thread(r);
                    t.start();

                }
            });


            for (String sx : apiMap.keySet()) {
                clientEndPoint.sendMessage("api:" + sx);

            }
            Thread.sleep(100);

        } catch (InterruptedException ex) {
            logger.error("InterruptedException exception: {}", ex.getMessage(), ex);
        } catch (URISyntaxException ex) {
            logger.error("URISyntaxException exception: {}", ex.getMessage(), ex);
        }

    }

    /***
     * At a scheduled rate, check if the connection to the server socket (cmd) is open, if not try to connect
     */
    static public void startRemoteConnectTask() {
        ScheduledExecutorService executorService;
        executorService = Executors.newSingleThreadScheduledExecutor();

        // reconnect
        executorService.scheduleAtFixedRate(
                new Runnable() {
                    public void run() {
                        boolean reconnect = false;
                        if (clientEndPoint != null) {
                            reconnect = !clientEndPoint.isOpen();
                        } else reconnect = true;

                        if (reconnect) connectToServer();
                    }
                },
                0, 1, TimeUnit.SECONDS);

        // keep alive the websocket
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                new Runnable() {
                    public void run() {
                        if (clientEndPoint != null && clientEndPoint.isOpen()) {
                            logger.debug("WS keeping alive");
                            clientEndPoint.sendMessage("keep alive");
                        }
                    }
                },
                1, 5, TimeUnit.SECONDS);

    }

    public static void main(String[] args) {
        org.eclipse.jetty.util.log.Log.setLog(new NoLogging());

//        Json configObj = CSLContext.instance.getConfig();
        Config config = Config.instance;

        CSLContext.instance.init(new CSLRunningArgs().parseArgs(args).setHasIdsRunner(true));

        CSLContext.instance.setDebug(true);

        // region -- read configuration
        // This is the client, override configuration is needed not to launch servers
        config.Server.setOn(false);
        config.UdpServerConf.setOn(true);

        // The proxy server to connect
        SERVER_IP = config.Client.getIpServerRemote();
        SERVER_URL_PREFIX = config.Client.getServerRemoteUrlPrefix();
        if (SERVER_URL_PREFIX == null) {
            SERVER_URL_PREFIX = "";
        }

        Boolean force_host_name_resolution = config.Client.getForceHostNameResolution();
        // Try to resolve host name (mainly for Docker hostnames)
        if (force_host_name_resolution) {
            try {
                SERVER_IP = InetAddress.getByName(SERVER_IP).getHostAddress();
            } catch (UnknownHostException e) {
                logger.error("Error while resolving host name: {}", e.getMessage(), e);
            }
        }

        SERVER_PORT = config.Client.getPortServerRemote();
        USE_SSL = config.Client.getUseSsl();
        API_KEY = config.Client.getApiKey();
        logger.trace("API KEY is: " + API_KEY);
        // endregion -- read configuration

        boolean USE_BROKER = false;

        JServiceLoader.setModuleName("IDS", new MosquittoConfig().setUseBroker(USE_BROKER));

        JServiceLoader.registerService(new CSLServiceIDS(), Json.object(), true);
        JServiceLoader.registerService(new AlertsService(), Json.object(), true);
        JServiceLoader.registerService(new MonitorService(), Json.object(), true);
        JServiceLoader.registerService(new TapsServices(), Json.object(), true);
        JServiceLoader.registerService(new DiscoveryServices(), Json.object(), true);
        JServiceLoader.registerService(new StatusService(), Json.object(), true);
        JServiceLoader.registerService(new AutoCryptService(), Json.object(), true);
        JServiceLoader.registerService(new NmapServices(), Json.object(), true);


        iniServices();

        startRemoteConnectTask();    // connect/reconnect
        CSLWebSocket.registerMessageBroadcaster(messageBroadcaster);

        CSLContext.instance.postInit(false, true);
        CSLContext.instance.start();

        CSLContext.instance.getIdsRunner().start();
        ((CSLAlertManager) CSLContext.instance.getCSLAlertManager()).registerAlertForwarder(alertForwarder);

        // Send API commands with specific privileges to the server
        try (DbapiHandlerForCSLInit dbapiHandler = new DbapiHandlerForCSLInit()) {
            dbapiHandler.sendCommandsList(JServiceLoader.getApiCommandsList());
        } catch (Exception e) {
            logger.error("Error while sending API commands to the server: {}", e.getMessage(), e);
        }

        if (Config.instance.Client.getLaunchWebApiServer()) {
            int port = Config.instance.Client.getWebApiServerPort();
            ApiHttpServer apiHttpServer = new ApiHttpServer().createServer(
                    new InetSocketAddress(port),
                    JServiceLoader.getApiCommandsList(),
                    new ApiGetHelp());
        }
    }
}