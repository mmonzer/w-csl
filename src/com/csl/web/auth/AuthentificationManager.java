package com.csl.web.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.csl.web.auth.controller.AuthController;
import com.csl.web.auth.controller.HelpController;
import com.csl.web.auth.controller.UserController;
import com.csl.web.auth.user.Role;
import com.csl.web.auth.user.User;
import com.csl.web.auth.user.UserService;
import com.ucsl.json.Json;

import spark.Request;
import spark.Response;
import spark.Service;

public final class AuthentificationManager {
	
	boolean debug=false;
	
	private List<String> listOfWebsocketPath= new ArrayList<String>();


	
	
	private static String base="secretjwt123457fshdfqsgcvszejb";

    private static final String SECRET_JWT = base+base+base+base;

    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();

    private final TokenService tokenService = new TokenService(SECRET_JWT);

    private UserService userService = new UserService();

	private HelpController helpController;
    
    
	/*
	
    public void initServer(Service sparkServer,ServerConfig config ) {
    	
    	Spark.port(config.getPort()); //CSLContext.context.getCurrentPortForWEB());
    	
    	
    	
    	String rootdir=replaceUserDir(config.getRootdir(), JServiceLoader.getUserDir());
    	sparkServer.staticFiles.externalLocation(rootdir); //userDir+"/public");
		sparkServer.staticFiles.expireTime(600);

		if (!config.getRootdir2().isEmpty()) Spark.staticFiles.externalLocation2(config.getRootdir2());
		if (!config.getRootdir3().isEmpty()) Spark.staticFiles.externalLocation3(config.getRootdir3());
		

		
		CSLWebSocket.registerAll();
		if (config.isSend_alerts()) {
			//AlertSenderViaWebSocket.init(this);
			addWebsocket(CSLWebSocket.WEB_SOCKET_ALERT, CSLWebSocketHandler.class);
		}
		if (config.isSend_console_output()) {
			//ConsoleOutputSenderViaWebSocket.init(this);
			addWebsocket(CSLWebSocket.WEB_SOCKET_CONSOLE, CSLWebSocketHandler.class);
		}
		if (vars_commands) {
			//VariablesNotificationSenderViaWebSocket.init(this);
			addWebsocket(CSLWebSocket.WEB_SOCKET_VARIABLES, CSLWebSocketHandler.class);
		}
		if (database_commands) {
			//DataBaseNotificationSenderViaWebSocket.init(this);
			addWebsocket(CSLWebSocket.WEB_SOCKET_DATABASE, CSLWebSocketHandler.class);
		}
		
		
		Spark.staticFiles.header("Access-Control-Allow-Origin", "*");

		Spark.get("/test",  (req, res) -> {res.type("text/html"); String s="<h1>tets</h1>"; res.body(s); return ""; });
		
		Spark.options("/*", (req, res) -> {
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

		Spark.before((req, res) -> {

			if (debug) System.out.println("WEB_DEBUG:"+req.pathInfo()+"  "+req.requestMethod());
			

			res.header("Access-Control-Allow-Origin", "*");
			res.header("Access-Control-Allow-Headers", "*");
			res.type("application/json");
		});

		
		
		

		Spark.after((req, res) -> {

			if (debug) System.out.println("req:"+req.pathInfo());

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
		
		   Spark.exception(Exception.class, (e, request, response) -> {
	            System.err.println("Exception while processing request");
	            e.printStackTrace();
	        });

		
    }
    
    

	private String replaceUserDir(String dir,String userDir) {

		if (dir.startsWith(".")) {
			return userDir+dir.substring(1);
		}
		return dir;
	}
    
    private String renderInvalid(Request req, Response res) {

		return "CSL Server : Invalid command ";
	}
    
	
    boolean isWebsocketPath(String path) {
		if (listOfWebsocketPath.indexOf(path)<0) return false;
		return true;
	}
    public void addWebsocket(String path,Class<?> handler ) {

    	System.out.println(" adding websocket "+path);
		if (!path.startsWith("/")) path="/"+path;
		
		if (listOfWebsocketPath.indexOf(path)<0)
			listOfWebsocketPath.add(path);
		else {
			System.err.println("websocket in use:"+path);
		}
		Spark.webSocket(path, handler);
	}
    */
	
	
    public void addAuthentification(Service sparkServer) {
    	
    	//initServer(new ServerConfig());

    	//userService.register("user123", "$2a$10$9xHgxps5MJ85eBl74RMhsuORACRuvDngy.ftbB/3G9lWxMGDIz8lO", "John", "Lacey",false);
    	userService.register("user123", "123456", "John", "Lacey",true).assignRole(Role.INTEGRATOR).assignRole(Role.ADMIN);
    	
        new AuthController( sparkServer,userService, tokenService, debug).init();
        new UserController(sparkServer,tokenService, debug).init();

        helpController=new HelpController(sparkServer, tokenService, true).init();
        
        // PERIODIC TOKENS CLEAN UP
        EXECUTOR_SERVICE.scheduleAtFixedRate(() -> {
            if (debug) System.out.println("Removing expired tokens");
            tokenService.removeExpired();
        }, 60, 60, TimeUnit.SECONDS); // every minute

        
        //Spark.get("/users", (req, res)  -> renderGetCommand(req,res));
        
      // Spark.post("*", (req, res)  -> renderPostCommand(req,res));
        
     
        for (User u:userService.getUsers() ) {
        	System.out.println(u);
        	
        }
    }

    private Object renderGetCommand(Request req, Response res) {
		// TODO Auto-generated method stub
    	Json rep=Json.object();
    	rep.set("test", "hello");
		return rep.toString();
	}

    
    private Object renderPostCommand(Request req, Response res) {
 		// TODO Auto-generated method stub
     	Json rep=Json.object();
     	rep.set("test", "connect:ok");
 		return rep.toString();
 	}

    
    /*
    
    public void addPostRoute(String path, Route r) {
		System.out.println("  adding post path "+path);
		Spark.post(path,r);	
		
	}
    
    
    public void registerApi(String name) {
    	XApiCommands api= new XApiCommands(name);
    	addPostRoute(api.getPathNameForPost(), api.getPostRoute());
    	helpController.addApiName(name);
    }
    
    public void initRoutes() {

    	registerApi("devdb");
    	registerApi("cve");
    	registerApi("ids");
    	registerApi("alerts");
    	
    	
         
    }
    
    
   */
    /*
	// BOOTSTRAP
    public static void main(String[] args) {
    	
    	
    	
    	startBroker();
    	
        AuthentificationManager cslApiGateway = new AuthentificationManager();
        cslApiGateway.init();
        
        cslApiGateway.initRoutes();
        
       
    }
    */

}
