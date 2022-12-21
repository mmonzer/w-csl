package com.csl.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.csl.intercom.jsoncmd.ApiCommands;
import com.csl.intercom.jsoncmd.JServiceLoader;
import com.csl.logger.CSLLogger;
import com.csl.util.CSLConfigFileServer;
import com.csl.web.auth.AuthentificationManager;
import com.csl.web.auth.ServerConfig;
import com.csl.web.jcmdoversocket.CSLWebSocketForJcmd;
import com.csl.web.jcmdoversocket.CSLWebSocketForJcmdHandler;
import com.csl.web.websockets.CSLWebSocket;
import com.csl.web.websockets.CSLWebSocketHandler;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;

/*
import static spark.Spark.after;
import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.init;
import static spark.Spark.options;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.staticFiles;
import static spark.Spark.webSocket;
 */

import spark.Request;
import spark.Response;
import spark.Route;
import spark.Service;


/**
 * This class uses the ICRoute interface to create void routes.
 * The response for an ICRoute is rendered in an after-filter.
 */
public class CSLHttpServer {

	static boolean ADD_GET_ROUTE=false;

	//	static public CSLHttpServer instance = new CSLHttpServer();

	Service sparkServer=null;
	ServerConfig serverConfig=null;


	//	private static boolean start_WEB_SOCKET_VARIABLES=false;
	//	private static boolean start_WEB_SOCKET_ALERT=false;
	//	private static boolean start_WEB_SOCKET_DATABASE=false;
	//	private static boolean start_WEB_SOCKET_CONSOLE=false;
	//	

	//	public static String WEB_SOCKET_VARIABLES="/chat";
	//	public static String WEB_SOCKET_ALERT="/alerts";
	//	public static String WEB_SOCKET_DATABASE="/database";
	//	public static String WEB_SOCKET_CONSOLE="/console";
	//
	//	private static String externalWebSiteRoot2=null;;	
	//	private static String externalWebSiteRoot=System.getProperty("user.dir")+"/public";

	private List<String> listOfWebsocketPath= new ArrayList<String>();



	static public int REFRESH_SOCKET_PERIOD=280;  // 280 sec (timeout after 300 sec)

	//private JCmdManager jCmdManager= new JCmdManager();

	//private int port=-1;
	//private boolean verbose =false;
	//private boolean running =false;
	//private   boolean debug =false;

	private   boolean initialized =false;
	private   boolean started=false;

	//private   String userDir="";
	//private   String rootdir="";
	//private   String rootdir2="";
	//private   String rootdir3="";

	//private   Boolean core_commands=false;
	//private   Boolean vars_commands=false;
	//private   Boolean modbus_commands=false;
	
	//private   Boolean json_commands=false;
	//private   Boolean custom_commands=false;
	//private   Boolean external_commands=false;
	
	
	//private   Boolean database_commands=false;
	//private   Boolean send_alerts=false;
	//private   Boolean send_console_output=false;
	
	////private   boolean config_file_commands =false;
	//private   boolean mx_commands =false;


	//List<ApiCommands> listOfAPIToRegister = new ArrayList<ApiCommands>();

	CSLConfigFileServer cslConfigFileServer=null;

	//ExecJsonCommands execJsonCommands=null;

	public void reinitServer(Json j) {
		
		boolean on=JsonUtil.getBooleanFromJson(j, "on",true);
		if (!on) return;
	

		if (sparkServer!=null) stop();
		initialized=false;
		initServer(j);

	}

	public void initServer(Json j) { //String rootdir, int port, boolean verbose) {

		
		boolean on=JsonUtil.getBooleanFromJson(j, "on",true);
		if (!on) return;
	
		ServerConfig sc= new ServerConfig(j);
		initServer(sc);
		
		AuthentificationManager am = new AuthentificationManager();
		
		am.addAuthentification(sparkServer);
	}


	public void initServer(ServerConfig sc) { //String rootdir, int port, boolean verbose) {

		if (initialized) {
			System.err.println("already initialized");
			System.exit(0);
		}

		this.serverConfig = sc;
		sparkServer = Service.ignite();
		initialized = true;

		if (!sc.isRunning()) return;

		sparkServer.exception(Exception.class, (e, req, res) -> e.printStackTrace()); // print all exceptions
		sparkServer.staticFiles.externalLocation(sc.getRootdir()); //userDir+"/public");
		sparkServer.staticFiles.expireTime(600);

		if (!sc.getRootdir2().isEmpty()) {
			sparkServer.staticFiles.externalLocation2(sc.getRootdir2());
		}

		if (!sc.getRootdir3().isEmpty()) {
			sparkServer.staticFiles.externalLocation3(sc.getRootdir3());
		}

		sparkServer.port(sc.getPort()); //CSLContext.context.getCurrentPortForWEB());

		// Websockets must be registered before regular HTTP routes
		// region -- register websockets
		CSLWebSocket.registerAll();
		if (sc.isSend_alerts()) {
			//AlertSenderViaWebSocket.init(this);
			addWebsocket(CSLWebSocket.WEB_SOCKET_ALERT, CSLWebSocketHandler.class);
		}
		if (sc.isSend_console_output()) {
			//ConsoleOutputSenderViaWebSocket.init(this);
			addWebsocket(CSLWebSocket.WEB_SOCKET_CONSOLE, CSLWebSocketHandler.class);
		}
		if (sc.isVars_commands()) {
			//VariablesNotificationSenderViaWebSocket.init(this);
			addWebsocket(CSLWebSocket.WEB_SOCKET_VARIABLES, CSLWebSocketHandler.class);
		}
		if (sc.isDatabase_commands()) {
			//DataBaseNotificationSenderViaWebSocket.init(this);
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

		//if (send_alerts) AlertSenderViaWebSocket.init();
		//if (send_console_output) ConsoleOutputSenderViaWebSocket.init();


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

		//System.out.println("API POST : "+path);
		String sresponse = req.body();
		System.out.println("\n<" + sresponse+">");
		System.out.println("path:" + req.pathInfo());

		String result = "";

		// if (s.compareToIgnoreCase("setfile")==0)

		Json data = Json.read(sresponse);
		Json cmd = data.get("cmd");
		Json params = data.get("params");

		if (cmd == null) {
			System.out.println("Invalid jcmd:" + cmd);
		}
		if (params == null) {
			params = Json.object();
		}
		
		String cresult ="";
		
		if (listOfRemoteApi.contains(api.getName())) {
			cresult= CSLWebSocketForJcmd.execJCmd(api.getName(), data).toString();
			System.out.println("REMOTE SERVER RESPONSE:"+cresult);
			
			
		}
		else {
			cresult=  api.exec(cmd.asString(), params).toString();
			System.out.println("SERVER RESPONSE:"+cresult);
			
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

		System.out.println("Exec " + cmd + " " + params);

		String cresult = api.exec(cmd, params).toString();

		return cresult;
	}
	

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	public static String ROUTE_MODE_GET = "get";
	public static String ROUTE_MODE_POST = "post";
	private final List<String> listOfRemoteApi = new ArrayList<String>();

	void startRefreshWebSocketTask(int n) {

		if (n <= 0) return;

		Runnable r = new Runnable() {

			@Override
			public void run() {
				//System.out.println("refresh");
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

			}
		};

		scheduler.scheduleAtFixedRate(r, n, n, TimeUnit.SECONDS);
	}

	private String replaceUserDir(String dir, String userDir) {

		if (dir.startsWith(".")) {
			return userDir + dir.substring(1);
		}
		return dir;
	}

	public boolean isStarted() {
		return started;
	}

	public void setStarted(boolean started) {
		this.started = started;
	}

	public void start() {

		if (!initialized) {
			System.err.println("CSL Web server not initialized, cannot start");
			System.exit(0);
		}

		CSLLogger.instance.info("current user dir = " + serverConfig.getUserDir());
		setStarted(true);

		sparkServer.init();

		startRefreshWebSocketTask(REFRESH_SOCKET_PERIOD);

		if (serverConfig.isVerbose()) {
			System.out.println();
			System.out.println("Starting:");
			System.out.println("=========");

			System.out.println("  Web server listening on " + serverConfig.getPort());
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


	//	static public void addRoute(String mode,String path, Route r) {
	//		if (mode.compareToIgnoreCase("get")==0)
	//			get(path,r);	
	//		else if (mode.compareToIgnoreCase("post")==0)
	//			post(path,r);	
	//		else 
	//			System.err.println("Invalid mode :"+mode+" path:"+path);
	//
	//	}

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


	public int getCurrentPortForWEB() {
		// TODO Auto-generated method stub
		return serverConfig.getPort();
	}


	//    private static String renderEditTodo(Request req) {
	//        return renderTemplate("velocity/editTodo.vm", new HashMap(){{ put("todo", TodoDao.find(req.params("id"))); }});
	//    }
	//
	//    private static String renderTodos(Request req) {
	//
	//    	 String z = req.contentType();
	//     	//BufferedReader br = request.getReader();
	//     	String sresponse = "???";
	//     	sresponse=req.body();
	//     	//System.out.println(z+"\n"+sresponse);
	//     	//System.out.println( req.requestMethod());
	//     	//System.out.println( req.pathInfo());
	//
	//        String statusStr = req.queryParams("status");
	//        Map<String, Object> model = new HashMap<>();
	//        model.put("todos", TodoDao.ofStatus(statusStr));
	//        model.put("filter", Optional.ofNullable(statusStr).orElse(""));
	//        model.put("activeCount", TodoDao.ofStatus(Status.ACTIVE).size());
	//        model.put("anyCompleteTodos", TodoDao.ofStatus(Status.COMPLETE).size() > 0);
	//        model.put("allComplete", TodoDao.all().size() == TodoDao.ofStatus(Status.COMPLETE).size());
	//        model.put("status", Optional.ofNullable(statusStr).orElse(""));
	//        if ("true".equals(req.queryParams("ic-request"))) {
	//            return renderTemplate("velocity/todoList.vm", model);
	//        }
	//        return renderTemplate("velocity/index.vm", model);
	//    }
	//
	private String renderInvalid(Request req, Response res) {
		return "CSL Server : Invalid command ";
	}

	public boolean isDebug() {
		// TODO Auto-generated method stub
		return serverConfig.isDebug();
	}

	public void addRemoteApi(String apiname) {
		// TODO Auto-generated method stub
		listOfRemoteApi.add(apiname.toLowerCase());
	}

}
