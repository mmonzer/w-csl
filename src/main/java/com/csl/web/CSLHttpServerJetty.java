package com.csl.web;

import com.csl.intercom.jsoncmd.ApiCommands;
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
import com.ucsl.json.Json;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.api.WebSocketBehavior;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.System.exit;

/**
 * CSLHttpServerJetty is an HTTP server implementation using Jetty.
 * This class handles the initialization, configuration, and management of the Jetty server,
 * including WebSocket support and API command handling.
 */
public class CSLHttpServerJetty {

    private Server jettyServer = null;
    private ServletContextHandler context = null;
    private ServerConfig serverConfig = null;

    private boolean initialized = false;
    private boolean isRemote = true;

    private static final Logger logger = LoggerFactory.getLogger(CSLHttpServerJetty.class);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // Global configuration constants
    public static int REFRESH_SOCKET_PERIOD = 280;
    static boolean ADD_GET_ROUTE = false;

    private final List<String> httpEndpointList = new ArrayList<>();
    private final List<String> listOfWebsocketPath = new ArrayList<>();

    /**
     * Initializes the server with the provided configuration.
     *
     * @param config The server configuration object.
     */
    public void initServer(Config.Server config) {
        boolean isEnabled = config.getOn();
        if (!isEnabled) return;

        isRemote = true;
        serverConfig = new ServerConfig(config);
        initServer(new InetSocketAddress(serverConfig.getPort()));
    }

    /**
     * Initializes the server with the provided configuration.
     *
     * @param config The server configuration object.
     */
    public void initServer(Config.Client config) {
        isRemote = false;
        initServer(new InetSocketAddress(config.getIpServerRemote(), config.getWebApiServerPort()));
    }

    /**
     * Initializes the server with the provided server configuration.
     *
     * @param inetSocketAddress socket address for the server configuration.
     */
    public void initServer(InetSocketAddress inetSocketAddress) {
        if (initialized) {
            logger.error("Server already initialized");
            exit(0);
        }

        jettyServer = new Server(inetSocketAddress);
        context = new ServletContextHandler();
        initialized = true;

        jettyServer.setErrorHandler(new JettyServerErrorHandler());

        setupContext();
        if (isRemote) setupWebSocketPolicy();
        if (isRemote) registerWebSockets();
        addApiHelpPageServlet();
        if (isRemote) addCorsOptionsServlet();
        registerApiCommands();

        jettyServer.setHandler(context);
    }

    /**
     * Starts the server and initializes the WebSocket refresh task.
     */
    public void start() {
        try {
            if (!initialized) {
                logger.error("API server not initialized, cannot start");
                exit(0);
            }
            jettyServer.start();
            logger.debug("API server started : {} ", jettyServer.getConnectors()[0]);
            jettyServer.join();

            // keep the web sockets alive
            if (isRemote) startRefreshWebSocketTask(REFRESH_SOCKET_PERIOD);
        } catch (Exception e) {
            logger.error("Error starting server", e);
            exit(0);
        }
    }

    /**
     * Stops the server and shuts down the scheduler.
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
     * Creates a servlet to handle GET requests for the given API.
     *
     * @param api The API commands that need to be handled.
     * @return HttpServlet that handles GET requests to the API.
     */
    public HttpServlet createGetServlet(ApiCommands api) {
        return new HttpServlet() {
            @Override
            public void init() {
                logger.debug("Adding GET path for API: {}", api.getName());
            }

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                handleWebSocketUpgrade(req, api);

                Set<String> paramKeys = req.getParameterMap().keySet();
                String cmd = extractCommandFromRequest(req);
                Json params = extractParamsFromRequest(req);

                logger.debug("Executing command: {} with parameters: {}", cmd, params);
                resp.getWriter().write(api.exec(cmd, params, null).toString());
            }
        };
    }

    /**
     * Creates a servlet to handle json and multi-part POST requests for the API.
     *
     * @param api The API commands that need to be handled.
     * @return HttpServlet that handles POST requests to the API.
     */
    public HttpServlet createPostServlet(ApiCommands api) {
        return new HttpServlet() {

            @Override
            public void init() {
                logger.debug("Adding Multi-part POST path for API: {}", api.getName());
            }

            @Override
            protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
                handleWebSocketUpgrade(req, api);

                //String bodyReq = readRequestBody(req);
                //logger.debug("Request Body: {}", bodyReq);

                Json data = Json.object();
                if (req.getContentType().contains("json")) {
                    data = handlerJsonRequest(req);
                } else
                if (req.getContentType().contains("multipart")) {
                    req.setAttribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(System.getProperty("java.io.tmpdir"),
                            1024*1024*100, 1024*1024*100,1024*1024*100));
                    req.getParts();
                    data = handlerMultipartRequest(req);
                }

                Json cmd = data.get("cmd");
                Json params = data.get("params");

                if (cmd == null) {
                    logger.warn("Invalid command: {}", cmd);
                }

                String bodyResp = executeApiCommand(api, data, cmd, params);
                resp.getWriter().write(bodyResp);
            }
        };
    }

    protected Json handlerJsonRequest(HttpServletRequest request) throws IOException {
        return Json.read(readRequestBody(request));
    }

    protected Json handlerMultipartRequest(HttpServletRequest req) throws IOException, ServletException {
        // Enable multi-part configuration
        req.setAttribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(System.getProperty("java.io.tmpdir"),
                1024*1024*100, 1024*1024*100,1024*1024*100));

        // Process the uploaded parts
        Json body = Json.object();
        Json files = Json.array();
        for (Part part : req.getParts()) {
            // Read the content of the file and print it to the console
            if (part.getContentType() != null) {  // It's a file part
                String fileName = Paths.get(part.getSubmittedFileName()).getFileName().toString(); // MSIE fix.
                try (InputStream inputStream = part.getInputStream()) {
                    byte[] fileContent = inputStream.readAllBytes();
                    files.add(Json.object("filename", fileName, "content", fileContent));
                }
            } else {  // It's a form field part
                body.set(part.getName(), new String(part.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return body.set("files", files);
    }

    /**
     * Creates a WebSocket servlet for the given path and handler.
     *
     * @param path    The WebSocket path.
     * @param handler The handler for the WebSocket.
     * @return ServletHolder that handles WebSocket requests at the given path.
     */
    public JettyWebSocketServlet addWebSocket(String path, Class<?> handler) {
        if (!path.startsWith("/")) path = "/" + path;

        if (!listOfWebsocketPath.contains(path)) {
            listOfWebsocketPath.add(path);
        } else {
            logger.warn("WebSocket path already in use: {}", path);
        }

        return new JettyWebSocketServlet(handler);
    }

    /**
     * Starts a task to refresh WebSocket connections at a regular interval.
     *
     * @param interval The interval (in seconds) to refresh the WebSockets.
     */
    public void startRefreshWebSocketTask(int interval) {
        if (interval <= 0) return;

        Runnable refreshTask = () -> {
            if (serverConfig.isSend_alerts())
                // Refresh the CSLWebSocketForAlert
                CSLWebSocket.refresh(CSLWebSocket.WEB_SOCKET_ALERT);
            if (serverConfig.isSend_console_output())
                // Refresh the CSLWebSocketForConsole
                CSLWebSocket.refresh(CSLWebSocket.WEB_SOCKET_CONSOLE);
            if (serverConfig.isVars_commands())
                // Refresh the CSLWebSocketForVariables
                CSLWebSocket.refresh(CSLWebSocket.WEB_SOCKET_VARIABLES);
            if (serverConfig.isDatabase_commands())
                // Refresh the CSLWebSocketForDatabase
                CSLWebSocket.refresh(CSLWebSocket.WEB_SOCKET_DATABASE);
            if (serverConfig.isJcmd_commands())
                // Refresh the CSLWebSocketForJcmd
                CSLWebSocket.refresh(CSLWebSocket.WEB_SOCKET_CMD);
        };
        scheduler.scheduleAtFixedRate(refreshTask, interval, interval, TimeUnit.SECONDS);
    }

    /**
     * Adds a remote API to the list of APIs that will be handled as remote.
     *
     * @param endpointPath The name of the endpoint API.
     */
    public void registerHttpEndpoint(String endpointPath) {
        httpEndpointList.add(endpointPath.toLowerCase());
    }

    // Private Helper Methods

    /**
     * Configures the server context and sets up basic filters.
     */
    private void setupContext() {
        context.setContextPath("/");
        // Add a common header to all the requests on all the paths
        context.addFilter(JettyFilterServlet.class, "/*", EnumSet.of(DispatcherType.REQUEST));
    }

    /**
     * Sets up the WebSocket policy.
     */
    private void setupWebSocketPolicy() {
        WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.SERVER);
        policy.setMaxTextMessageSize(1024 * 1024);
    }

    /**
     * Registers the WebSocket servlets based on the server configuration.
     */
    private void registerWebSockets() {
        CSLWebSocket.registerAll();

        // CSLWebSocketHandler is the WS to the HMI (alerts deprecated?, console used, variables deprecated?, database deprecated?)
        // CSLWebSocketForJcmd is the WS with CSL-Client
        if (serverConfig.isSend_alerts()) {
            context.addServlet(new ServletHolder(addWebSocket(CSLWebSocket.WEB_SOCKET_ALERT, CSLWebSocketHandler.class)),
                    CSLWebSocket.WEB_SOCKET_ALERT);
        }
        if (serverConfig.isSend_console_output()) {
            context.addServlet(new ServletHolder(addWebSocket(CSLWebSocket.WEB_SOCKET_CONSOLE, CSLWebSocketHandler.class)),
                    CSLWebSocket.WEB_SOCKET_CONSOLE);
        }
        if (serverConfig.isVars_commands()){
            context.addServlet(new ServletHolder(addWebSocket(CSLWebSocket.WEB_SOCKET_VARIABLES, CSLWebSocketHandler.class)),
                    CSLWebSocket.WEB_SOCKET_VARIABLES);
        }
        if (serverConfig.isDatabase_commands()){
            context.addServlet(new ServletHolder(addWebSocket(CSLWebSocket.WEB_SOCKET_DATABASE, CSLWebSocketHandler.class)),
                    CSLWebSocket.WEB_SOCKET_DATABASE);
        }
        if (serverConfig.isJcmd_commands()) {
            context.addServlet(new ServletHolder(addWebSocket(CSLWebSocketForJcmd.WEB_SOCKET_CMD, CSLWebSocketForJcmdHandler.class)),
                    CSLWebSocketForJcmd.WEB_SOCKET_CMD);
        }
    }

    /**
     * Adds a servlet for the API help page.
     */
    private void addApiHelpPageServlet() {
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                Json cmd = extractParamsFromRequest(req);
                cmd.set("url", req.getRequestURL().toString());
                String fullUrl = req.getRequestURL().toString();
                if (req.getQueryString() != null) fullUrl += "?" + req.getQueryString();
                cmd.set("fullUrl", fullUrl);

                String cmdBody = JServiceLoader.getApiHelpPage(cmd);
                resp.setContentType("text/html");
                resp.getWriter().write(cmdBody);
            }
        }), "/apihelp");
    }

    /**
     * Adds a servlet to handle CORS options requests.
     */
    private void addCorsOptionsServlet() {
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                handleCorsOptions(req, resp);
            }
        }), "/*");
    }

    /**
     * Registers API commands by adding their respective servlets.
     */
    private void registerApiCommands() {
        for (ApiCommands api : JServiceLoader.getApiCommandsList()) {
            String path = api.getName();
            logger.info("Registering API: <{}>", path);
            if (ADD_GET_ROUTE)
                context.addServlet(new ServletHolder(createGetServlet(api)), "/" + api.getName() + "/*");
            context.addServlet(new ServletHolder(createPostServlet(api)), "/" + api.getPathFilter());
        }
    }

    /**
     * Handles WebSocket upgrades for the given request and API.
     *
     * @param req The HTTP request.
     * @param api The API commands associated with the request.
     */
    private void handleWebSocketUpgrade(HttpServletRequest req, ApiCommands api) {
        if ("Websocket".equalsIgnoreCase(req.getHeader("upgrade"))) {
            context.addServlet(new ServletHolder(addWebSocket(api.getName(), CSLWebSocketHandler.class)), "/" + api.getName());
        }
    }

    /**
     * Extracts the command from the request URI.
     *
     * @param req The HTTP request.
     * @return The extracted command.
     */
    private String extractCommandFromRequest(HttpServletRequest req) {
        String apiURI = req.getRequestURI();
        return (apiURI.length() > 1) ? apiURI.substring(1) : "???";
    }

    /**
     * Extracts parameters from the request as a JSON object.
     *
     * @param req The HTTP request.
     * @return The parameters as a JSON object.
     */
    private Json extractParamsFromRequest(HttpServletRequest req) {
        Set<String> paramKeys = req.getParameterMap().keySet();
        Json params = Json.object();

        for (String paramName : paramKeys) {
            String value = req.getParameter(paramName);
            params.set(paramName, value);
        }
        return params;
    }

    /**
     * Reads the request body and returns it as a string.
     *
     * @param req The HTTP request.
     * @return The request body as a string.
     * @throws IOException If an input or output exception occurred.
     */
    private String readRequestBody(HttpServletRequest req) throws IOException {
        StringBuilder bodyReq = new StringBuilder();
        BufferedReader reader = req.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            bodyReq.append(line);
        }
        return bodyReq.toString();
    }

    /**
     * Executes the API command and returns the response as a string.
     *
     * @param api    The API commands to execute.
     * @param data   The JSON data containing the command and parameters.
     * @param cmd    The command to execute.
     * @param params The parameters for the command.
     * @return The response from executing the command.
     */
    private String executeApiCommand(ApiCommands api, Json data, Json cmd, Json params) {
        String bodyResp;
        if (httpEndpointList.contains(api.getName().toLowerCase())) {
            bodyResp = CSLWebSocketForJcmd.execJCmd(api.getName(), data).toString();
            logger.debug("Remote server response: {}", bodyResp);
        } else {
            bodyResp = api.exec(cmd.asString(), params, data.get("files")).toString();
            logger.debug("Server response: {}", bodyResp);
        }
        return bodyResp;
    }

    /**
     * Handles CORS options requests.
     *
     * @param req  The HTTP request.
     * @param resp The HTTP response.
     * @throws IOException If an input or output exception occurred.
     */
    private void handleCorsOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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
}