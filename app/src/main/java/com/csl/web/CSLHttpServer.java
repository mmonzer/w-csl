package com.csl.web;

import com.csl.intercom.jsoncmd.JServiceLoader;
import com.csl.web.auth.AuthentificationManager;
import com.csl.web.auth.ServerConfig;
import com.csl.web.jcmdoversocket.CSLWebSocketForJcmd;
import com.csl.web.jcmdoversocket.CSLWebSocketForJcmdHandler;
import com.csl.web.websockets.CSLWebSocket;
import com.csl.web.websockets.CSLWebSocketHandler;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * This class uses the ICRoute interface to create void routes.
 * The response for an ICRoute is rendered in an after-filter.
 */
public class CSLHttpServer {
	static boolean ADD_GET_ROUTE=false;

	Service sparkServer=null;
	ServerConfig serverConfig=null;

	private List<String> listOfWebsocketPath= new ArrayList<String>();

	static public int REFRESH_SOCKET_PERIOD=280;  // 280 sec (timeout after 300 sec)

	private   boolean initialized =false;
	@Setter
    @Getter
    private   boolean started=false;
	private static final Logger logger = LoggerFactory.getLogger(CSLHttpServer.class);

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	private final List<String> listOfRemoteApi = new ArrayList<String>();

	public void initServer(Json j) { //String rootdir, int port, boolean verbose) {
		boolean on=JsonUtil.getBooleanFromJson(j, "on",true);
		if (!on) return;

		ServerConfig sc= new ServerConfig(j);
		initServer(sc);

		AuthentificationManager am = new AuthentificationManager();

		am.addAuthentification(sparkServer);
	}


	public void initServer(ServerConfig sc) {
		if (initialized) {
			logger.error("Already initialized");
			System.exit(0);
		}

		this.serverConfig = sc;
		sparkServer = Service.ignite();
		initialized = true;

		if (!sc.isRunning()) return;

		sparkServer.exception(Exception.class, (e, req, res) -> logger.error("Spark exception", e)); // print all exceptions
		sparkServer.staticFiles.externalLocation(sc.getRootdir());
		sparkServer.staticFiles.expireTime(600);

		if (!sc.getRootdir2().isEmpty()) {
			sparkServer.staticFiles.externalLocation2(sc.getRootdir2());
		}

		if (!sc.getRootdir3().isEmpty()) {
			sparkServer.staticFiles.externalLocation3(sc.getRootdir3());
		}

		sparkServer.port(sc.getPort());

		// Websockets must be registered before regular HTTP routes
		// region -- register websockets
		CSLWebSocket.registerAll();
		if (sc.isSend_alerts()) {
			addWebsocket(CSLWebSocket.WEB_SOCKET_ALERT, CSLWebSocketHandler.class);
		}
		if (sc.isSend_console_output()) {
			addWebsocket(CSLWebSocket.WEB_SOCKET_CONSOLE, CSLWebSocketHandler.class);
		}
		if (sc.isVars_commands()) {
			addWebsocket(CSLWebSocket.WEB_SOCKET_VARIABLES, CSLWebSocketHandler.class);
		}
		if (sc.isDatabase_commands()) {
			addWebsocket(CSLWebSocket.WEB_SOCKET_DATABASE, CSLWebSocketHandler.class);
		}
		if (sc.isJcmd_commands()) {
			addWebsocket(CSLWebSocketForJcmd.WEB_SOCKET_CMD, CSLWebSocketForJcmdHandler.class);
		}
		// endregion -- register websockets

		// PROTECTED ENDPOINT FOR DEVELOPER ROLE_PROPERTY
		sparkServer.get("/apihelp", (request, response) -> {
			Json jj = Json.object();
			Set<String> paramKeys = request.queryParams();
			List<String> varNames = new ArrayList<String>(paramKeys);

			for (String name : varNames) {
				String[] z = request.queryParamsValues(name);
				if (z != null) jj.set(name, z[0]);
			}

			jj.set("url", request.url());

			String fullUrl = request.url();

			if (request.queryString() != null) {
				fullUrl = request.url() + '?' + request.queryString();

			}
			jj.set("full_url", fullUrl);

			String s = JServiceLoader.getApiHelpPage(jj);
			response.type("text/html");
			response.body(s);
			return s;
		});

		sparkServer.staticFiles.header("Access-Control-Allow-Origin", "*");

		sparkServer.options("/*", (req, res) -> {
			String accessControlRequestHeaders = req.headers("Access-Control-Request-Headers");
			if (accessControlRequestHeaders != null) {
				res.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
			}

			String accessControlRequestMethod = req.headers("Access-Control-Request-Method");
			if (accessControlRequestMethod != null) {
				res.header("Access-Control-Allow-Methods", accessControlRequestMethod);
			}

			return "OK";
		});

		sparkServer.before("/cmd", (req, res) -> {
			String body = req.body();
			System.out.println("before cmd");
			System.out.println(body);
		});
		sparkServer.before("/taps", (req, res) -> {
			String body = req.body();
			System.out.println("before taps");
			System.out.println(body);
		});
		// Before-filters are evaluated before each request, and can read the request and read/modify the response.
		// to stop execution, use halt()
		sparkServer.before((req, res) -> {
			if (sc.isDebug()) System.out.println("WEB_DEBUG:" + req.pathInfo() + "  " + req.requestMethod());

			res.header("Access-Control-Allow-Origin", "*");
			res.header("Access-Control-Allow-Headers", "*");
			res.type("application/json");
		});


		for (IApiCommands api : JServiceLoader.getApiCommandsList()) {

			String path = api.getName();
			System.out.println("REGISTER API:<" + path + ">");

			if (ADD_GET_ROUTE)
				addGetRoute(getPathFilterForGet(api), getGetRoute(api)); // api.getGetRoute());
			// FIXME else ?
			addPostRoute(getPathNameForPost(api), getPostRoute(api)); // api.getPostRoute());

		}

		//decomposer sous forme de fcts

		// After-filters are evaluated after each request, and can read the request and read/modify the response:
		sparkServer.after((req, res) -> {
			if (sc.isDebug()) System.out.println("req:" + req.pathInfo());

			if (!isWebsocketPath(req.pathInfo())) {
				if (res.body() == null) { // if we didn't try to return a rendered response
					res.body(renderInvalid(req, res));
				}
			}
		});

	}
	
	
	public String getPathFilterForGet(IApiCommands api) {
		String s=api.getPathFilter();
		if (!s.startsWith("/")) s="/"+s;
		return s;
	}
	
	public String getPathNameForPost(IApiCommands api) {
		String s=api.getName();
		if (!s.startsWith("/")) s="/"+s;
		return s;
	}

	public Route getPostRoute(IApiCommands api) {
		return (req, res) -> execPostCommand(api,req, res);
	}

	public Route getGetRoute(IApiCommands api) {
		return (req, res) -> renderGetCommand(api, req, res);
	}

	private String execPostCommand(IApiCommands api, Request req, Response res) {

		String sresponse = req.body();
		logger.debug("\n<" + sresponse+">");
		System.out.println("path:" + req.pathInfo());

		String result = "";

		Json data = Json.read(sresponse);
		Json cmd = data.get("cmd");
		Json params = data.get("params");

		if (cmd == null) {
			logger.warn("Invalid jcmd:" + cmd);
		}
		if (params == null) {
			params = Json.object();
		}

		String cresult ="";

		if (listOfRemoteApi.contains(api.getName())) {
			cresult= CSLWebSocketForJcmd.execJCmd(api.getName(), data).toString();
			logger.debug("REMOTE SERVER RESPONSE:"+cresult);
		}
		else {
			cresult=  api.exec(cmd.asString(), params).toString();
			logger.debug("SERVER RESPONSE:"+cresult);
		}

		return cresult;
	}

	private String renderGetCommand(IApiCommands api,  Request req, Response res) {
		Set<String> paramKeys = req.queryParams();

		String s = req.pathInfo();
		if (s.length() > 1)
			s = s.substring(1);

		String cmd = "???";

		List<String> varNames = new ArrayList<String>(paramKeys);

		Json params = Json.object();
		for (String name : varNames) {
			String value = req.queryParams(name);
			if (name.compareTo("cmd") == 0) {
				cmd = value;
			}
			if (name.compareTo("exec_jsoncmd") == 0) {
				cmd = value;
			}
			else {
				params.set(name, value);

			}
		}

		logger.debug("Exec " + cmd + " " + params);

		String cresult = api.exec(cmd, params).toString();

		return cresult;
	}


	void startRefreshWebSocketTask(int n) {
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
		scheduler.scheduleAtFixedRate(r, n, n, TimeUnit.SECONDS);
	}

    public void start() {
		if (!initialized) {
			logger.error("CSL Web server not initialized, cannot start");
			System.exit(0);
		}

		logger.debug("current user dir = " + serverConfig.getUserDir());
		setStarted(true);

		sparkServer.init();

		startRefreshWebSocketTask(REFRESH_SOCKET_PERIOD);

		if (serverConfig.isVerbose()) {
			logger.info("Starting");
			logger.info("Web server listening on {}", serverConfig.getPort());
		}
	}

	// stop the server
	public void stop() {
		sparkServer.stop();
		scheduler.shutdownNow();

		setStarted(false);
	}

	boolean isWebsocketPath(String path) {
		return listOfWebsocketPath.indexOf(path) >= 0;
	}

	public void addGetRoute(String path, Route r) {
		System.err.println("  adding get path " + path);
		sparkServer.get(path, r);
	}

	public void addPostRoute(String path, Route r) {
		System.err.println("  adding post path " + path);
		sparkServer.post(path, r);
	}

	public void addWebsocket(String path, Class<?> handler) {
		if (!path.startsWith("/")) path = "/" + path;

		if (listOfWebsocketPath.indexOf(path) < 0)
			listOfWebsocketPath.add(path);
		else {
			System.err.println("websocket in use:" + path);
		}
		sparkServer.webSocket(path, handler);
	}

	private String renderInvalid(Request req, Response res) {
		return "CSL Server : Invalid command ";
	}

	public boolean isDebug() {
		return serverConfig.isDebug();
	}

	public void addRemoteApi(String apiname) {
		listOfRemoteApi.add(apiname.toLowerCase());
	}

}
