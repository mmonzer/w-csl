package com.csl.web;

import com.csl.intercom.jsoncmd.JServiceLoader;
import com.csl.core.Config;
import com.csl.web.auth.ServerConfig;
import com.csl.web.jcmdoversocket.CSLWebSocketForJcmd;
import com.csl.web.jcmdoversocket.CSLWebSocketForJcmdHandler;
import com.csl.web.jettyutils.JettyFilterServlet;
import com.csl.web.jettyutils.JettyServerErrorHandler;
import com.csl.web.jettyutils.JettyWebSocketServlet;
import com.csl.web.websockets.CSLWebSocket;
import com.csl.web.websockets.CSLWebSocketHandler;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.json.Json;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.api.WebSocketBehavior;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.csl.web.jcmdoversocket.CSLWebSocketForJcmd.X_CORRELATION_ID;
import static java.lang.System.exit;

/*
    HttpServer using Jetty replacing the spark version
 */
public class CSLHttpServerJetty {
    Server jettyServer = null;
    ServletContextHandler context = null;
    ServerConfig serverConfig = null;

    private static final Logger logger = LoggerFactory.getLogger(CSLHttpServerJetty.class);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    static public int REFRESH_SOCKET_PERIOD = 280;

    static boolean ADD_GET_ROUTE = false;

    private final List<String> listOfRemoteApi = new ArrayList<String>();
    private final List<String> listOfWebsocketPath = new ArrayList<String>();

    /**
     * Initialize the server
     */
    public void initServer(Config.Server config) {
        boolean on = config.getOn();
        if (!on) return;
        ServerConfig sc = new ServerConfig(config);
        initServer(sc);
    }

    /**
     * Initialize the server
     *
     * @param sc : ServerConfig object with the configuration
     */
    public void initServer(ServerConfig sc) {
        serverConfig = sc;
        jettyServer = new Server(serverConfig.getPort());
        context = new ServletContextHandler();

        if (!sc.isRunning()) return;

        jettyServer.setErrorHandler(new JettyServerErrorHandler());


        //TODO : add location for static files

        //Context initialization
        context.setContextPath("/");
        context.addFilter(JettyFilterServlet.class, "/*", EnumSet.of(DispatcherType.REQUEST)); //Filter for the console log

        //Policy for websockets
        WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.SERVER);
        policy.setMaxTextMessageSize(1024 * 1024);

        // Add websockets
        registerWebSockets(sc);

        // add the servlet for the api help page
        context.addServlet(createApiHelpServletHolder(), "/apihelp");

        //TODO : add header for static files

        // Options for CORS
        context.addServlet(createCORSOptionsServletHolder(), "/*");

        // Add the servlets for the commands for the server API
        registerApiCmd();

        jettyServer.setHandler(context);
    }

    /**
     * WebSockets initialization;
     * - alerts : DEPRECATED : receive the alerts from CSLAlertManager.sendToViewer, but also to dbapi directly. On message do nothing.
     * - console : used widely in IDS modules, and StatusNotifier and ActivityMonitor. On message do nothing.
     * - cmd : socket for jcmd ({cmd:"...", params:{...}}.
     */
    private void registerWebSockets(ServerConfig sc) {
        CSLWebSocket.registerAll();
        if (sc.isSend_alerts()) {
            context.addServlet(new ServletHolder(addWebSocket(CSLWebSocket.WEB_SOCKET_ALERT, CSLWebSocketHandler.class)),
                    CSLWebSocket.WEB_SOCKET_ALERT);
        }
        if (sc.isSend_console_output()) {
            context.addServlet(new ServletHolder(addWebSocket(CSLWebSocket.WEB_SOCKET_CONSOLE, CSLWebSocketHandler.class)),
                    CSLWebSocket.WEB_SOCKET_CONSOLE);
        }
        if(sc.isVars_commands()) {
            context.addServlet(new ServletHolder(addWebSocket(CSLWebSocket.WEB_SOCKET_VARIABLES, CSLWebSocketHandler.class)),
                    CSLWebSocket.WEB_SOCKET_VARIABLES);
        }
        if(sc.isDatabase_commands()) {
            context.addServlet(new ServletHolder(addWebSocket(CSLWebSocket.WEB_SOCKET_DATABASE, CSLWebSocketHandler.class)),
                    CSLWebSocket.WEB_SOCKET_DATABASE);
        }
        if (sc.isJcmd_commands()) {
            context.addServlet(new ServletHolder(addWebSocket(CSLWebSocketForJcmd.WEB_SOCKET_CMD, CSLWebSocketForJcmdHandler.class)),
                    CSLWebSocketForJcmd.WEB_SOCKET_CMD);
        }
    }

    /**
     * Register the Api endpoints to manage the requetes
     */
    private void registerApiCmd() {
        for (IApiCommands api : JServiceLoader.getApiCommandsList()) {
            String path = api.getName();
            logger.info("REGISTER API  : <" + path + ">");
            if (ADD_GET_ROUTE)
                context.addServlet(new ServletHolder(createGetServlet(api)), "/" + api.getName() + "/*");
            context.addServlet(new ServletHolder(createPostServlet(api)), "/" + api.getPathFilter());
        }
    }

    /**
     * Start the server
     */
    public void start() {
        try {
            logger.debug("current user dir = " + serverConfig.getUserDir());
            jettyServer.start();
            jettyServer.join();
            startRefreshWebSocketTask(REFRESH_SOCKET_PERIOD);

            if (serverConfig.isVerbose()) {
                logger.info("Starting");
                logger.info("Web server started on {} ", serverConfig.getPort());
            }
        } catch (Exception e) {
            logger.error("Error starting server", e);
            exit(0);
        }
    }

    /**
     * Stop the server
     */
    public void stop() {
        try {
            jettyServer.stop();
            scheduler.shutdownNow();
        } catch (Exception e) {
            logger.error("Error stopping server", e);
            exit(0);
        }
    }

    /**
     * Create a servlet handling get requests
     *
     * @param api : api containing commands that needs to be handled
     * @return HttpServlet handling get requests to the api
     */
    public HttpServlet createGetServlet(IApiCommands api) {
        HttpServlet httpServlet = new HttpServlet() {
            @Override
            public void init() {
                System.err.println(" adding get path " + api.getName());
            }

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                // convert http get to websocket get
                if ("Websocket".equalsIgnoreCase(req.getHeader("upgrade"))) {
                    context.addServlet(new ServletHolder(addWebSocket(api.getName(), CSLWebSocketHandler.class)), "/" + api.getName());
                }

                Set<String> paramKeys = req.getParameterMap().keySet();

                String apiURI = req.getRequestURI();
                if (apiURI.length() > 1)
                    apiURI = apiURI.substring(1);
                String cmd = "???";

                List<String> paramNames = new ArrayList<String>(paramKeys);

                Json params = Json.object();
                for (String paramName : paramNames) {
                    String value = req.getParameter(paramName);
                    if (paramName.equals("cmd") || paramName.equals("exec_jsoncmd")) {
                        cmd = value;
                    } else {
                        params.set(paramName, value);
                    }
                    ;
                }

                logger.debug("Exec " + cmd + " " + params);

                resp.getWriter().write(api.exec(cmd, params).toString());
            }
        };
        return httpServlet;
    }

    /**
     * Create a servlet handling post requests
     *
     * @param api : api containing commands that needs to be handled
     * @return HttpServlet handling post requests to the api
     */
    public HttpServlet createPostServlet(IApiCommands api) {
        HttpServlet httpServlet = new HttpServlet() {
            @Override
            public void init() {
                System.err.println(" adding post path " + api.getName());
            }

            @Override
            protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                if ("Websocket".equalsIgnoreCase(req.getHeader("upgrade"))) {
                    context.addServlet(new ServletHolder(addWebSocket(api.getName(), CSLWebSocketHandler.class)), "/" + api.getName());
                }
                StringBuilder bodyReq = new StringBuilder();
                BufferedReader reader = req.getReader();
                String line;
                while ((line = reader.readLine()) != null) {
                    bodyReq.append(line);
                }
                logger.debug("\n<" + bodyReq + ">");

                // X-Correlation-ID
                String xCorrelationId = req.getHeader(X_CORRELATION_ID);

                Json data = Json.read(bodyReq.toString());
                Json cmd = data.get("cmd");
                Json params = data.get("params");

                if (cmd == null) logger.warn("Invalid jcmd" + cmd);
                if (params == null) params = Json.object();

                String bodyResp = "";

                if (listOfRemoteApi.contains(api.getName())) {
                    bodyResp = CSLWebSocketForJcmd.execJCmd(api.getName(), data, xCorrelationId).toString();
                    logger.debug("REMOTE SERVER RESPONSE:" + bodyResp);
                } else {
                    bodyResp = api.exec(cmd.asString(), params.set(X_CORRELATION_ID, xCorrelationId)).toString();
                    logger.debug("SERVER RESPONSE:" + bodyResp);
                }
                resp.getWriter().write(bodyResp);
            }
        };
        return httpServlet;
    }

    /**
     * Create a websocket servlet
     *
     * @param path    : path to the websocket
     * @param handler : handler for the websocket
     * @return ServletHolder handling websocket request at the given path
     */
    public JettyWebSocketServlet addWebSocket(String path, Class<?> handler) {
        if (!path.startsWith("/")) path = "/" + path;

        if (listOfWebsocketPath.indexOf(path) < 0)
            listOfWebsocketPath.add(path);
        else {
            System.err.println("websocket in use:" + path);
        }

        return new JettyWebSocketServlet(handler);
    }

    /**
     * Start a task to refresh the websockets. keep alive sockets??
     *
     * @param n : interval to refresh the websockets
     */
    public void startRefreshWebSocketTask(int n) {
        //TODO : implementation
        if (n <= 0) return;

        Runnable r = () -> {
            if (serverConfig.isSend_alerts())
                CSLWebSocket.refresh(CSLWebSocket.WEB_SOCKET_ALERT); //CSLWebSocketForAlert.refresh();
            if (serverConfig.isSend_console_output())
                CSLWebSocket.refresh(CSLWebSocket.WEB_SOCKET_CONSOLE); //CSLWebSocketForConsole.refresh();
            if (serverConfig.isVars_commands())
                CSLWebSocket.refresh(CSLWebSocket.WEB_SOCKET_VARIABLES); //CSLWebSocketForVariables.refresh();
            if (serverConfig.isDatabase_commands())
                CSLWebSocket.refresh(CSLWebSocket.WEB_SOCKET_DATABASE); //CSLWebSocketForDatabase.refresh();
            if (serverConfig.isJcmd_commands())
                CSLWebSocket.refresh(CSLWebSocket.WEB_SOCKET_CMD); //CSLWebSocketForDatabase.refresh();
        };
//        scheduler.scheduleAtFixedRate(r, n, n, TimeUnit.SECONDS);
    }

    /**
     * Add a remote api
     *
     * @param apiname : name of the remote api
     */
    public void addRemoteApi(String apiname) {
        listOfRemoteApi.add(apiname.toLowerCase());
    }

    /**
     * Create API help servlet holder
     * @return the corresponding servlet Holder
     */
    private ServletHolder createApiHelpServletHolder() {
        return new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                Json cmd = Json.object();
                Set<String> paramKeys = req.getParameterMap().keySet();
                List<String> varNames = new ArrayList<String>(paramKeys);

                for (String name : varNames) {
                    String[] values = req.getParameterValues(name);
                    if (values != null) cmd.set(name, values[0]);
                }

                cmd.set("url", req.getRequestURL().toString());
                String fullUrl = req.getRequestURL().toString();
                if (req.getQueryString() != null) fullUrl += "?" + req.getQueryString();
                cmd.set("fullUrl", fullUrl);

                String cmdBody = JServiceLoader.getApiHelpPage(cmd);
                resp.setContentType("text/html");
                resp.getWriter().write(cmdBody);
            }
        });
    }

    /**
     * Create CORS options servlet holder
     * @return the corresponding servlet Holder
     */
    private ServletHolder createCORSOptionsServletHolder() {
        return new ServletHolder(new HttpServlet() {
            @Override
            protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                String accessControlRequestHeaders = req.getHeader("Access-Control-Request-Headers");
                if (accessControlRequestHeaders != null) {
                    resp.setHeader("Access-Control-Allow-Headers", accessControlRequestHeaders);
                }

                String accessControlRequestMethod = req.getHeader("Access-Control-Request-Method");
                if (accessControlRequestMethod != null) {
                    resp.setHeader("Access-Control-Allow-Methods", accessControlRequestMethod);
                }

                resp.setStatus(HttpServletResponse.SC_OK);
            }
        });
    }
}
