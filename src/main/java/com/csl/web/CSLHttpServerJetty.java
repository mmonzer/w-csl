package com.csl.web;

import com.csl.core.Config;
import com.csl.intercom.jsoncmd.ApiCommands;
import com.csl.intercom.jsoncmd.JServiceLoader;
import com.csl.logger.CSLApplicativeLogger;
import com.csl.logger.LoggerCustomEndpoints;
import com.csl.logger.LoggerInterfaces;
import com.csl.util.JCmd;
import com.csl.util.ThreadUtils;
import com.csl.web.jcmdoversocket.CSLWebSocketForJcmd;
import com.csl.web.jcmdoversocket.CSLWebSocketForJcmdHandler;
import com.csl.web.jettyutils.JettyFilterServlet;
import com.csl.web.jettyutils.JettyServerErrorHandler;
import com.csl.web.jettyutils.CustomJettyWebSocketServlet;
import com.csl.web.websockets.CSLWebSocket;
import com.csl.web.websockets.CSLWebSocketHandler;
import com.csl.web.websockets.WebSocketServlet;
import com.ucsl.json.Json;
import jakarta.websocket.server.ServerEndpointConfig;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.api.WebSocketBehavior;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.eclipse.jetty.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.csl.logger.LoggerConstants.X_CORRELATION_ID;
import static java.lang.System.exit;

/**
 * CSLHttpServerJetty is an HTTP server implementation using Jetty.
 * This class handles the initialization, configuration, and management of the Jetty server,
 * including WebSocket support and API command handling.
 */
public class CSLHttpServerJetty {

    public static final String PATH_SEPARATOR = "/";
    private Server jettyServer = null;
    private ServletContextHandler context = null;

    private boolean initialized = false;
    private boolean isRemote = true;

    private static final CSLApplicativeLogger logger = CSLApplicativeLogger.getLogger(CSLHttpServerJetty.class);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // Global configuration constants
    public static final int REFRESH_SOCKET_PERIOD = 280;

    private final List<String> listOfRemoteApi = new ArrayList<>();
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
        initServer(new InetSocketAddress(config.getWebserverPort()));
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
        context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        jettyServer.setHandler(context);
        initialized = true;

        jettyServer.setErrorHandler(new JettyServerErrorHandler());

        setupContext();

        if (isRemote) {
            //setupWebSocketPolicy();  // TODO : policy
//            registerWebSockets();
//            JettyWebSocketServletContainerInitializer.configure(context, null);
//            ServletHolder wsHolderConsole = new ServletHolder(CSLWebSocket.WEB_SOCKET_CONSOLE, new WebSocketServlet(CSLWebSocketHandler.class));
//            context.addServlet(wsHolderConsole, CSLWebSocket.WEB_SOCKET_CONSOLE);
//            ServletHolder wsHolderJCmd = new ServletHolder(CSLWebSocket.WEB_SOCKET_CMD, new WebSocketServlet(CSLWebSocketForJcmdHandler.class));
//            context.addServlet(wsHolderJCmd, CSLWebSocket.WEB_SOCKET_CMD);

            CSLWebSocket.registerAll();
            JakartaWebSocketServletContainerInitializer.configure(context, (context, container) ->
            {
                // Add echo endpoint to server container
                if (!listOfWebsocketPath.contains(CSLWebSocket.WEB_SOCKET_CMD)) {
                    listOfWebsocketPath.add(CSLWebSocket.WEB_SOCKET_CMD);
                } else {
                    logger.warn("WebSocket path already in use: {}", CSLWebSocket.WEB_SOCKET_CMD);
                }
                ServerEndpointConfig jcmdHandler = ServerEndpointConfig.Builder.create(CSLWebSocketForJcmdHandler.class, CSLWebSocket.WEB_SOCKET_CMD).build();
                container.addEndpoint(jcmdHandler);if (!listOfWebsocketPath.contains(CSLWebSocket.WEB_SOCKET_CONSOLE)) {
                listOfWebsocketPath.add(CSLWebSocket.WEB_SOCKET_CONSOLE);
            } else {
                logger.warn("WebSocket path already in use: {}", CSLWebSocket.WEB_SOCKET_CONSOLE);
            }
                ServerEndpointConfig consoleHandler = ServerEndpointConfig.Builder.create(CSLWebSocketHandler.class, CSLWebSocket.WEB_SOCKET_CONSOLE).build();
                container.addEndpoint(consoleHandler);
            });



            addCorsOptionsServlet();
        }

        addApiHelpPageServlet();
        registerApiCommands();
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

            logger.debug("Web server started on port {} ", Config.instance.Server.getWebserverPort());
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
                // X-Correlation-ID
                handleWebSocketUpgrade(req, api);
                Json data;
                if (req.getContentType() != null && req.getContentType().contains("json")) {
                    data = handlerJsonRequest(req);
                } else if (req.getContentType() != null && req.getContentType().contains("multipart")) {
                    data = handlerMultipartRequest(req);
                } else {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Content type not supported");
                    logger.warn("Wrong format");
                    return;
                }

                // Get the full request URI
                String requestUri = req.getRequestURI();  // e.g., /japi/discovery/upload_entity_http_connection_file

                // Split the URI to extract the cmd part
                String[] urlParts = requestUri.split(PATH_SEPARATOR);

                String cmdStr = null;
                // Ensure there are enough parts to extract endpoint and cmd
                if (urlParts.length >= 3) {
                    String endpoint = urlParts[1]; // discovery
                    cmdStr = urlParts[2];      // upload_entity_http_connection_file
                    data.set(JCmd.CMD, cmdStr);
                } else {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URL format");
                    return;
                }
                Json cmd = data.get(JCmd.CMD);
                Json params = data.get(JCmd.PARAMETERS);

                if (cmd == null) {
                    logger.warn("Invalid command: {}", cmd);
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid command: " + cmd);
                    return;
                }

                String bodyResp = executeApiCommand(api, data, cmd, params, req.getHeader(X_CORRELATION_ID));

                resp.getWriter().write(bodyResp);
            }
        };
    }

    /**
     * Handles the body parsing of a JSON formated requests
     *
     * @param request request with body to parse
     * @return the body in JSON format
     * @throws IOException if error reading body
     */
    protected Json handlerJsonRequest(HttpServletRequest request) throws IOException {
        return Json.read(readRequestBody(request));
    }

    /**
     * Handles the body parsing of a Multipart requests
     *
     * @param request HTTP request with body to parse
     * @return the body parsed into JSON format
     * @throws IOException      if error reading body
     * @throws ServletException if error reading parts
     */
    protected Json handlerMultipartRequest(HttpServletRequest request) throws IOException, ServletException {
        // Enable multi-part configuration
        request.setAttribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(System.getProperty("java.io.tmpdir"),
                (long) 1024 * 1024 * 100, (long) 1024 * 1024 * 100, 1024 * 1024 * 100));

        // Process the uploaded urlParts
        Json body = Json.object();
        Json files = Json.array();
        for (Part part : request.getParts()) {
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
    public CustomJettyWebSocketServlet addWebSocket(String path, Class<?> handler) {
        if (!path.startsWith(PATH_SEPARATOR)) path = PATH_SEPARATOR + path;

        if (!listOfWebsocketPath.contains(path)) {
            listOfWebsocketPath.add(path);
        } else {
            logger.warn("WebSocket path already in use: {}", path);
        }

        return new CustomJettyWebSocketServlet(handler);
    }

    /**
     * Starts a task to refresh WebSocket connections at a regular interval.
     *
     * @param interval The interval (in seconds) to refresh the WebSockets.
     */
    public void startRefreshWebSocketTask(int interval) {
        if (interval <= 0) return;

        Runnable refreshTask = () -> {
            if (Config.instance.Server.getSendAlerts())
                // Refresh the CSLWebSocketForAlert
                CSLWebSocket.refresh(CSLWebSocket.WEB_SOCKET_ALERT);
            if (Config.instance.Server.getSendConsoleOutput())
                // Refresh the CSLWebSocketForConsole
                CSLWebSocket.refresh(CSLWebSocket.WEB_SOCKET_CONSOLE);
            if (Config.instance.Server.getVarsCommands())
                // Refresh the CSLWebSocketForVariables
                CSLWebSocket.refresh(CSLWebSocket.WEB_SOCKET_VARIABLES);
            if (Config.instance.Server.getDatabaseCommands())
                // Refresh the CSLWebSocketForDatabase
                CSLWebSocket.refresh(CSLWebSocket.WEB_SOCKET_DATABASE);
            if (Config.instance.Server.getJcmdCommands())
                // Refresh the CSLWebSocketForJcmd
                CSLWebSocket.refresh(CSLWebSocket.WEB_SOCKET_CMD);
        };

        ThreadUtils.uncorrelatedSingleThreadScheduledAtFixedRate(scheduler, refreshTask, interval, interval, TimeUnit.SECONDS, LoggerCustomEndpoints.KEEP_ALIVE_IHM_WS, LoggerInterfaces.CSL_SERVER);
    }

    /**
     * Adds a remote API to the list of APIs that will be handled as remote.
     *
     * @param endpointPath The name of the endpoint API.
     */
    public void registerHttpEndpoint(String endpointPath) {
        listOfRemoteApi.add(endpointPath.toLowerCase());
    }

    // Private Helper Methods

    /**
     * Configures the server context and sets up basic filters.
     */
    private void setupContext() {
        context.setContextPath(PATH_SEPARATOR);
        // Add a common header to all the requests on all the paths
        context.addFilter(JettyFilterServlet.class, "/*", EnumSet.of(DispatcherType.REQUEST));
    }

    /**
     * Sets up the WebSocket policy.
     */
    private void setupWebSocketPolicy() {
        WebSocketPolicy policy = new WebSocketPolicy() {
            @Override
            public WebSocketBehavior getBehavior() {
                return null;
            }

            @Override
            public Duration getIdleTimeout() {
                return null;
            }

            @Override
            public int getInputBufferSize() {
                return 0;
            }

            @Override
            public int getOutputBufferSize() {
                return 0;
            }

            @Override
            public long getMaxBinaryMessageSize() {
                return 0;
            }

            @Override
            public long getMaxTextMessageSize() {
                return 0;
            }

            @Override
            public long getMaxFrameSize() {
                return 0;
            }

            @Override
            public boolean isAutoFragment() {
                return false;
            }

            @Override
            public void setIdleTimeout(Duration duration) {

            }

            @Override
            public void setInputBufferSize(int i) {

            }

            @Override
            public void setOutputBufferSize(int i) {

            }

            @Override
            public void setMaxBinaryMessageSize(long l) {

            }

            @Override
            public void setMaxTextMessageSize(long l) {

            }

            @Override
            public void setMaxFrameSize(long l) {

            }

            @Override
            public void setAutoFragment(boolean b) {

            }
        };
        policy.setMaxTextMessageSize(1024 * 1024);
    }

    /**
     * Registers the WebSocket servlets based on the server configuration.
     */
    private void registerWebSockets() {
        CSLWebSocket.registerAll();

        // CSLWebSocketHandler is the WS to the HMI (alerts deprecated?, console used, variables deprecated?, database deprecated?)
        // CSLWebSocketForJcmd is the WS with CSL-Client
        if (Config.instance.Server.getSendAlerts()) {
            context.addServlet(new ServletHolder(addWebSocket(CSLWebSocket.WEB_SOCKET_ALERT, CSLWebSocketHandler.class)),
                    CSLWebSocket.WEB_SOCKET_ALERT);
        }
        if (Config.instance.Server.getSendConsoleOutput()) {
            context.addServlet(new ServletHolder(addWebSocket(CSLWebSocket.WEB_SOCKET_CONSOLE, CSLWebSocketHandler.class)),
                    CSLWebSocket.WEB_SOCKET_CONSOLE);
        }
        if (Config.instance.Server.getVarsCommands()) {
            context.addServlet(new ServletHolder(addWebSocket(CSLWebSocket.WEB_SOCKET_VARIABLES, CSLWebSocketHandler.class)),
                    CSLWebSocket.WEB_SOCKET_VARIABLES);
        }
        if (Config.instance.Server.getDatabaseCommands()) {
            context.addServlet(new ServletHolder(addWebSocket(CSLWebSocket.WEB_SOCKET_DATABASE, CSLWebSocketHandler.class)),
                    CSLWebSocket.WEB_SOCKET_DATABASE);
        }
        if (Config.instance.Server.getJcmdCommands()) {
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
            context.addServlet(new ServletHolder(createPostServlet(api)), PATH_SEPARATOR + api.getPathFilter());
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
            context.addServlet(new ServletHolder(addWebSocket(api.getName(), CSLWebSocketHandler.class)), PATH_SEPARATOR + api.getName());
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
    private String executeApiCommand(ApiCommands api, Json data, Json cmd, Json params, String xCorrelationId) {
        String bodyResp;
        if (listOfRemoteApi.contains(api.getName().toLowerCase())) {
            logger.traceReq(LoggerInterfaces.CSL_CLIENT, "Sending command with body {} ...", params);
            bodyResp = CSLWebSocketForJcmd.execJCmd(api.getName(), data, xCorrelationId).toString();
            logger.traceResp(LoggerInterfaces.CSL_CLIENT, "Sent command with body {} : {}", params, bodyResp);
        } else {
            logger.traceReq(LoggerInterfaces.LOCAL, "Executing command with body {} ...", params);
            bodyResp = api.exec(cmd.asString(), params.set(X_CORRELATION_ID, xCorrelationId), data.get("files")).toString();
            logger.traceResp(LoggerInterfaces.LOCAL, "Executed command with body {} : {}", params, bodyResp);
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
    private void handleCorsOptions(HttpServletRequest req, HttpServletResponse resp) {
        String accessControlRequestHeaders = req.getHeader("Access-Control-Request-Headers");
        if (accessControlRequestHeaders != null) {
            resp.setHeader("Access-Control-Allow-Headers", accessControlRequestHeaders);
        }

        String accessControlRequestMethod = req.getHeader("Access-Control-Request-Method");
        if (accessControlRequestMethod != null) {
            resp.setHeader("Access-Control-Allow-Methods", accessControlRequestMethod);
        }
    }

    /**
     * Add a remote api
     *
     * @param apiname : name of the remote api
     */
    public void registerRemoteApi(String apiname) {
        listOfRemoteApi.add(apiname.toLowerCase());
    }

    /**
     * Create API help servlet holder
     *
     * @return the corresponding servlet Holder
     */
    private ServletHolder createApiHelpServletHolder() {
        return new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                Json cmd = Json.object();
                Set<String> paramKeys = req.getParameterMap().keySet();
                List<String> varNames = new ArrayList<>(paramKeys);

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
     *
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