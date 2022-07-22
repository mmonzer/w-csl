package com.csl.web;




import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.csl.intercom.jsoncmd.ApiCommands;
import com.csl.intercom.jsoncmd.JServiceLoader;
import com.xcsl.interfaces.IApiCommands;
import com.csl.logger.CSLLogger;
import com.csl.util.CSLConfigFileServer;
import com.csl.web.auth.AuthentificationManager;
import com.csl.web.auth.ServerConfig;
import com.csl.web.jcmdoversocket.CSLWebSocketForJcmd;
import com.csl.web.jcmdoversocket.CSLWebSocketForJcmdHandler;
import com.csl.web.websockets.CSLWebSocket;
import com.csl.web.websockets.CSLWebSocketHandler;
import com.xcsl.json.Json;
import com.xcsl.json.JsonUtil;

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

		//if (j==null) return;

		this.serverConfig=sc;

		sparkServer= Service.ignite();
		initialized=true;
		
		//userDir=sc.getUserDir();
				
		if (!sc.isRunning()) return;
		
		/*userDir = System.getProperty("user.dir");

		running=JsonUtil.getBooleanFromJson(j,"on", false);
		if (!running) return;

		//jCmdManager.init(j);


		verbose=JsonUtil.getBooleanFromJson(j,"verbose", false);
		debug=JsonUtil.getBooleanFromJson(j,"debug", false);

		port = JsonUtil.getIntFromJson(j, "webserver_port",8000);

		rootdir=JsonUtil.getStringFromJson(j, "web_rootdir","");
		rootdir=replaceUserDir(rootdir, userDir);
		if (!rootdir.isEmpty()) setExternalWebSiteRoot(rootdir);

		//Json jConfig=CSLContext.context.getConfig().get("ihm_server_conf");
		rootdir2=JsonUtil.getStringFromJson(j, "web_rootdir2","");
		rootdir2=replaceUserDir(rootdir2, userDir);
		if (!rootdir2.isEmpty()) setExternalWebSiteRoot(rootdir2);
		//System.out.println("Set IHM web roor dir:"+rootdir2);

		//Json jConfig=CSLContext.context.getConfig().get("ihm_server_conf");
		rootdir3=JsonUtil.getStringFromJson(j, "web_rootdir3","");
		rootdir3=replaceUserDir(rootdir3, userDir);
		if (!rootdir3.isEmpty()) setExternalWebSiteRoot3(rootdir3);


		

		core_commands= JsonUtil.getBooleanFromJson(j,"core_commands", false);
		//vars_commands (+ notifications of update)
		vars_commands= JsonUtil.getBooleanFromJson(j,"vars_commands", false);

		modbus_commands= JsonUtil.getBooleanFromJson(j,"modbus_commands", false);
		//external_commands = JsonUtil.getBooleanFromJson(j,"external_commands", false);

		//custom_commands = JsonUtil.getBooleanFromJson(j,"custom_commands", false);

		//json_commands = JsonUtil.getBooleanFromJson(j,"json_commands", false);
		database_commands = JsonUtil.getBooleanFromJson(j,"database_commands", false);

		//config_file_commands = JsonUtil.getBooleanFromJson(j,"config_file_commands", false);
		//mx_commands = JsonUtil.getBooleanFromJson(j,"mx_commands", false);

		send_alerts= JsonUtil.getBooleanFromJson(j,"send_alerts", false);
		send_console_output= JsonUtil.getBooleanFromJson(j,"send_console_output", false);

		if (verbose) {
			System.out.println("\nCSL Server:");
			System.out.println("===========");
			System.out.println("  on  :"+running);
			System.out.println("  port:"+port);

			System.out.println("  rootdir:"+rootdir);
			System.out.println("  rootdir2:"+rootdir2);
			System.out.println("  rootdir3:"+rootdir3);

			System.out.println("  core_commands   :"+core_commands);
			System.out.println("  vars_commands   :"+vars_commands);

			System.out.println("  modbus_commands :"+modbus_commands);
			//System.out.println("  external_commands:"+external_commands);
			//System.out.println("  config_file_commands:"+config_file_commands);

			//System.out.println("  json_commands:"+json_commands);
			System.out.println("  database_commands:"+database_commands);

			System.out.println("  send_alerts:"+send_alerts);
			System.out.println("  send_console_outputs:"+send_console_output);

		}
*/

		sparkServer.exception(Exception.class, (e, req, res) -> e.printStackTrace()); // print all exceptions


		sparkServer.staticFiles.externalLocation(sc.getRootdir()); //userDir+"/public");
		sparkServer.staticFiles.expireTime(600);

		if (!sc.getRootdir2().isEmpty()) {
			//if (!rootdir2.isEmpty())
				sparkServer.staticFiles.externalLocation2(sc.getRootdir2());
		}

		
		if (!sc.getRootdir3().isEmpty()) {
				sparkServer.staticFiles.externalLocation3(sc.getRootdir3());
		}
//		if (!sc.getRootdir2().isEmpty()) {
//			//if (!rootdir2.isEmpty())
//				sparkServer.staticFiles.externalLocation2(sc.getRootdir2());
//		}

		
		
		sparkServer.port(sc.getPort()); //CSLContext.context.getCurrentPortForWEB());



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
		
		// PROTECTED ENDPOINT FOR DEVELOPER ROLE_PROPERTY
		sparkServer.get("/apihelp", (request, response) -> {

			
			Json jj=Json.object();
			Set<String> paramKeys= request.queryParams();
			List<String> varNames= new ArrayList<String>(paramKeys);


			for (String name:varNames) {
				String[] z = request.queryParamsValues(name);
				if (z!=null) jj.set(name,z[0]);

			}

			jj.set("url", request.url());
			
			String fullUrl=request.url();
			
			if (request.queryString() != null) {
				fullUrl=request.url()+'?'+request.queryString();
		        
		    }
			jj.set("full_url", fullUrl);
			
			
			String s= JServiceLoader.getApiHelpPage(jj);
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

		sparkServer.before((req, res) -> {

			if (sc.isDebug()) System.out.println("WEB_DEBUG:"+req.pathInfo()+"  "+req.requestMethod());


			res.header("Access-Control-Allow-Origin", "*");
			res.header("Access-Control-Allow-Headers", "*");
			res.type("application/json");
		});






	
		
		//if (external_commands) ExternalCommands.instance.init();

		/*if (config_file_commands) {
			cslConfigFileServer=new CSLConfigFileServer();
			cslConfigFileServer.initCommandsOnServer(this);
		}*/

		//		if (database_commands) 
		//			CSLContext.instance.getDatabaseServer().addRoute(this);
		//		

		/*if (json_commands) {
			execJsonCommands = new ExecJsonCommands(this);
			execJsonCommands.init(getJCmdManager());
		}*/


		

		for (IApiCommands api:JServiceLoader.getApiCommandsList()) {

			String path=api.getName();
			System.out.println("REGISTER API:<"+path+">");

			// ne sert à rien
			/*if (path.isEmpty()) {
				if (execJsonCommands==null) {
					execJsonCommands = new ExecJsonCommands(this);
					execJsonCommands.init(getJCmdManager());
				}

				for (String name:api.getListOfCommands())	{
					System.err.println("Register cmd:"+name);
					getJCmdManager().registerCmd(name, api.getJCmd(name));
				}
			}
			// tout est ds api
			else*/
			{
				if (ADD_GET_ROUTE) addGetRoute(getPathFilterForGet(api),getGetRoute(api)); // api.getGetRoute());
				addPostRoute(getPathNameForPost(api), getPostRoute(api)); // api.getPostRoute());
			}
		}

		//if (send_alerts) AlertSenderViaWebSocket.init();
		//if (send_console_output) ConsoleOutputSenderViaWebSocket.init();


		//decomposer sous forme de fcts


		sparkServer.after((req, res) -> {

			if (sc.isDebug()) System.out.println("req:"+req.pathInfo());

			//			if ((req.pathInfo().compareTo(WEB_SOCKET_VARIABLES)!=0)&&
			//					(req.pathInfo().compareTo(WEB_SOCKET_ALERT)!=0)&&
			//					(req.pathInfo().compareTo(WEB_SOCKET_DATABASE)!=0)&&
			//					(req.pathInfo().compareTo(WEB_SOCKET_CONSOLE)!=0))
			if (!isWebsocketPath(req.pathInfo()))
			{
				if (res.body() == null) { // if we didn't try to return a rendered response
					res.body(renderInvalid(req, res));
				}
			}
		});


		//ici



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

	//	if (debug) System.out.println("Exec " + cmd + " " + params);

		
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

		//System.out.println("API GET : "+path);
		
		Set<String> paramKeys = req.queryParams();

		// String sresponse = req.body();

		String s = req.pathInfo();
		if (s.length() > 1)
			s = s.substring(1);

		//System.out.println("pathInfo="+s);
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

	private List<String> listOfRemoteApi= new ArrayList<String>();

	void startRefreshWebSocketTask(int n) {

		if (n<=0) return;

		Runnable  r= new Runnable() {

			@Override
			public void run() {
				//System.out.println("refresh");
				if (serverConfig.isSend_alerts()) CSLWebSocket.refresh(CSLWebSocket.WEB_SOCKET_ALERT); //CSLWebSocketForAlert.refresh();
				if (serverConfig.isSend_console_output()) CSLWebSocket.refresh(CSLWebSocket.WEB_SOCKET_CONSOLE); //CSLWebSocketForConsole.refresh();
				if (serverConfig.isVars_commands()) CSLWebSocket.refresh(CSLWebSocket.WEB_SOCKET_VARIABLES); //CSLWebSocketForVariables.refresh();
				if (serverConfig.isDatabase_commands()) CSLWebSocket.refresh(CSLWebSocket.WEB_SOCKET_DATABASE); //CSLWebSocketForDatabase.refresh();
				if (serverConfig.isJcmd_commands()) CSLWebSocket.refresh(CSLWebSocket.WEB_SOCKET_CMD); //CSLWebSocketForDatabase.refresh();

			}
		};

		scheduler.scheduleAtFixedRate(r, n, n, TimeUnit.SECONDS);
	}



	private String replaceUserDir(String dir,String userDir) {

		if (dir.startsWith(".")) {
			return userDir+dir.substring(1);
		}
		return dir;
	}

	//private JCmdManager getJCmdManager() { return jCmdManager;}



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


		//   DatabaseCommands.init(); 




		//	exception(Exception.class, (e, req, res) -> e.printStackTrace()); // print all exceptions



		;

		sparkServer.init();


		startRefreshWebSocketTask(REFRESH_SOCKET_PERIOD);


		if (serverConfig.isVerbose()) {
			System.out.println("");
			System.out.println("Starting:");
			System.out.println("=========");

			System.out.println("  Web server listening on "+serverConfig.getPort());
		}
	}



	// stop the server
	public void stop() {

		sparkServer.stop();
		scheduler.shutdownNow();

		setStarted(false);
	}





	public static String ROUTE_MODE_GET="get";
	public static String ROUTE_MODE_POST="post";

	boolean isWebsocketPath(String path) {
		if (listOfWebsocketPath.indexOf(path)<0) return false;
		return true;
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
		System.err.println("  adding get path "+path);
		sparkServer.get(path,r);	
	}

	public void addPostRoute(String path, Route r) {
		System.err.println("  adding post path "+path);
		sparkServer.post(path,r);	
	}

	public void addWebsocket(String path,Class<?> handler ) {

		if (!path.startsWith("/")) path="/"+path;

		if (listOfWebsocketPath.indexOf(path)<0)
			listOfWebsocketPath.add(path);
		else {
			System.err.println("websocket in use:"+path);
		}
		sparkServer.webSocket(path, handler);
	}


//	private void setExternalWebSiteRoot(String string) {
//		// TODO Auto-generated method stub
//		if (isStarted()) {
//			System.err.println("Cannot modify web ste root whe server is started :"+string);
//		}
//
//		rootdir=string;
//	}
//	
//	private void setExternalWebSiteRoot2(String string) {
//		// TODO Auto-generated method stub
//		if (isStarted()) {
//			System.err.println("Cannot modify web ste root whe server is started :"+string);
//		}
//
//		rootdir2=string;
//	}
//
//	private void setExternalWebSiteRoot3(String string) {
//		// TODO Auto-generated method stub
//		if (isStarted()) {
//			System.err.println("Cannot modify web ste root whe server is started :"+string);
//		}
//
//		rootdir3=string;
//	}

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

		////System.out.println("Invalid : <"+req.pathInfo()+">");


		return "CSL Server : Invalid command ";
	}

	//	public void addApiCommands(ApiCommands api) {
	//		// TODO Auto-generated method stub
	//		System.out.println("Register api for http:"+api);
	//		listOfAPIToRegister.add(api);
	//		System.out.println(listOfAPIToRegister);
	//	}



	public boolean isDebug() {
		// TODO Auto-generated method stub
		return serverConfig.isDebug();
	}

	public void addRemoteApi(String apiname) {
		// TODO Auto-generated method stub
		listOfRemoteApi.add(apiname.toLowerCase());
	}










	//	private static String renderTemplate(String template, Map model) {
	//		return new VelocityTemplateEngine().render(new ModelAndView(model, template));
	//	}
	//
	//	@FunctionalInterface
	//	private interface ICRoute extends Route {
	//		default Object handle(Request request, Response response) throws Exception {
	//			handle(request);
	//			return "";
	//		}
	//		void handle(Request request) throws Exception;
	//	}
	//
	//	
	//	private static String uploadFile(Request request) {
	//		  // TO allow for multipart file uploads
	//		  request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(""));
	//		  try {
	//		    // "file" is the key of the form data with the file itself being the value
	//		    Part filePart = request.raw().getPart("file");
	//		    
	//		    // The name of the file user uploaded
	//		    String uploadedFileName = filePart.getSubmittedFileName();
	//		    InputStream stream = filePart.getInputStream();
	//		    // Write stream to file under storage folder
	//		    Files.copy(stream, Paths.get("storage").resolve(uploadedFileName), StandardCopyOption.REPLACE_EXISTING);
	//		  } catch (IOException | ServletException e) {
	//		    return "Exception occurred while uploading file" + e.getMessage();
	//		  }
	//		  return "File successfully uploaded";
	//		}
	//		private static String downloadFile(String fileName) {
	//		  Path filePath = Paths.get("storage").resolve(fileName);
	//		  File file = filePath.toFile();
	//		  if (file.exists()) {
	//		    try {
	//		      // Read from file and join all the lines into a string
	//		      return Files.readAllLines(filePath).stream().collect(Collectors.joining());
	//		    } catch (IOException e) {
	//		      return "Exception occurred while reading file" + e.getMessage();
	//		    }
	//		  }
	//		  return "File doesn't exist. Cannot download";
	//		}
	//		private static int countFiles() {
	//		  // Count the number of files in the storage folder
	//		  return new File("storage").listFiles().length;
	//		}
	//		private static String deleteFile(String fileName) {
	//		  File file = Paths.get("storage").resolve(fileName).toFile();
	//		  if (file.exists()) {
	//		    file.delete();
	//		    return "File deleted";
	//		  } else {
	//		    return "File " + fileName + " doesn't exist";
	//		  }
	//		}
	//		

	/*
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 */









	//
	//		
	//	private static String save_jsonfile(Request req, Response res) {
	//
	//			
	//			
	//			String fileName=req.params(":file");
	//			fileName=FileCommands.cleanFileName(fileName);
	//			IDSTrace.log(IDSTrace.WEB_DATABASE,
	//					"Save jsonfile:"+fileName);
	//		
	//			//System.out.println("Save file :"+fileName);
	//
	//			//System.out.println("POST : test");
	//			String sresponse = req.body();
	//			//System.out.println("\n"+sresponse);
	//			//System.out.println("path:"+req.pathInfo()); 
	//			
	//			String result="";
	//			
	//			//if (s.compareToIgnoreCase("setfile")==0) 
	//			{
	//				Json data = Json.read(sresponse);
	//				Json contents=data.get("contents");
	//				IDSTrace.log(IDSTrace.WEB_DATABASE,
	//						"Save Contents="+data.get("contents"));
	//				
	//				String uuid=JsonUtil.getStringFromJson(data, "uuid","");
	//				
	//				//String fileName=inputJson.get("filename").asString();
	//				//String contents=inputJson.get("contents").asString();
	//				
	//				if ((fileName!=null)&&(contents!=null)) {
	//					result=FileCommands.writeFile(fileName+".json", contents.toString());
	//				
	////					 Json j=Json.object();
	////	    		        j.set("action","modified");
	////	    		        j.set("name",fileName);
	////	    		        j.set("uuid", uuid);
	////	    		     CSLWebSocketForDatabase.broadcastMessageJson("database",j );
	//					
	//	    		     FileCommands.sendEventFileModified(fileName, uuid);
	//					
	//				} else {
	//					if (fileName==null) result=result+"Invalid file name ";
	//					if (contents==null) result=result+"Invalid contents ";
	//			
	//				}
	//			}
	//		
	//			
	//			Json j=Json.object();
	//			j.set("result","ok");
	//			
	//			return j.toString();
	//		}
	//		
	//		
	//		
	//		private static String load_jsonfile(Request req, Response res) {
	//
	//			
	//			String fileName=req.params(":file");
	//			//System.out.println("Load file :"+fileName);
	//			fileName=FileCommands.cleanFileName(fileName);
	//			IDSTrace.log(IDSTrace.WEB_DATABASE,
	//					"Load jsonfile:"+fileName);
	//		
	//			
	//			Set<String> paramKeys= req.queryParams();
	//			
	//			String sresponse = req.body();
	//			//System.out.println("\n"+sresponse);
	//			//System.out.println("path:"+req.pathInfo()); 
	//			//String s=req.pathInfo();
	//			//if (s.length()>1) s=s.substring(1);
	//			
	//			String result="";
	//			
	//			if (fileName!=null) {
	//				result=FileCommands.readFile(fileName+".json");
	//			} else {
	//				result="No filename";
	//			}
	//			
	//			System.out.println("File contents:"+result);
	//			IDSTrace.log(IDSTrace.WEB_DATABASE,
	//					"File Contents="+result);
	//		
	//			Json j=Json.object();
	//			Json z=Json.read(result);
	//			j.set("contents",z);
	//			result=j.toString();
	//			
	//			//res.body(result);
	//			res.body(result);
	//			return result;
	//		}
	//



	/*
	 * 
	 * 
	 * 
	 * 
	 */






}
