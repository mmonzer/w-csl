package main;


import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.csl.alert.CSLAlertManager;
import com.csl.core.CSLContext;
import com.csl.core.NoLogging;
import com.csl.intercom.broker.MosquittoConfig;
import com.csl.intercom.jsoncmd.ApiGetHelp;
import com.csl.intercom.jsoncmd.JServiceLoader;
import com.csl.web.database.CSLServiceJsonDataBase;
import com.csl.web.jcmdoversocket.IAlertForwarder;
import com.csl.web.websockets.CSLWebSocket;
import com.csl.web.websockets.IMessageBroadcaster;

import com.ucsl.interfaces.IApiCommands;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import com.xcsl.miniserver.ApiHttpServer;

import main.services.AlertsService;
import main.services.CSLServiceDemo;
import main.services.CSLServiceIDS;
import main.services.MonitorService;
import main.services.NmapServices;
import main.services.TapsServices;
import main.util.CSLRunningArgs;
import main.xcom.WebsocketClientEndpoint;

public class CSLIDSMainClient {

	static String SERVER_IP = "127.0.0.1";
	static String SERVER_URL_PREFIX = "";
	static String API_KEY = "";
	static int SERVER_PORT = 8000;
	static boolean USE_SSL = false;

	static WebsocketClientEndpoint clientEndPoint = null;

	static HashMap<String, IApiCommands> apiMap = new HashMap<String, IApiCommands>();
	
	static IMessageBroadcaster messageBroadcaster= 
    		new IMessageBroadcaster() {
				
				@Override
				public void broadcastMessageString(String socketName, String s) {
					// TODO Auto-generated method stub
					
					System.out.println("Send string over ws:"+s);
					if (clientEndPoint!=null) {
						if (!clientEndPoint.isOpen()) return;
						clientEndPoint.sendMessage("wss:"+socketName+":"+s);
					}
				}
				
				@Override
				public void broadcastMessageJson(String socketName, Json j) {
							    	
					//System.out.println("Send json over ws:"+j);
					
					if (clientEndPoint!=null) {
						if (!clientEndPoint.isOpen()) return;
						clientEndPoint.sendMessage("wsj:"+socketName+":"+j);
					}
			    	
				}
			};
			
	static IAlertForwarder alertForwarder= new IAlertForwarder() {
		
		@Override
		public void sendAlert(Json alert) {
			System.out.println("********Forward alert:\n"+alert+"\n*************");
			if (clientEndPoint!=null) {
				if (!clientEndPoint.isOpen()) return;
				clientEndPoint.sendMessage("alert:"+alert);
			}
			
		}
	};


	/***
	 * Adds the < serviceName, service > to the apiMap hashmap from the JServiceLoader's commandsList (listOfAPIToRegister)
	 */
	static public void iniServices() {
		
		
		for (IApiCommands api:JServiceLoader.getApiCommandsList()) {

			String path=api.getName();
			System.out.println("REGISTER API:<"+path+">");
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
	 *     		}
	 *     }
	 * }
	 * NOTE that each message is handled by a new thread
	 */
	static public void connectToServer() {
		try {
			String wsProtocol = USE_SSL ? "wss" : "ws";
			String s = null;
			if (SERVER_PORT > 0) {
				s = wsProtocol + "://" + SERVER_IP + ":" + SERVER_PORT + SERVER_URL_PREFIX +"/cmd";
			} else
				s = wsProtocol + "://" + SERVER_IP + SERVER_URL_PREFIX + "/cmd";

			System.out.print("Try to connect to WS server " + s);

			clientEndPoint = new WebsocketClientEndpoint(new URI(s));
			clientEndPoint.apiKey = API_KEY;
			if (!clientEndPoint.isOpen()) {
				System.out.println("  --> failed");
				return;
			} else
				System.out.println("   --> connected");

			// add listener
			clientEndPoint.addMessageHandler(new WebsocketClientEndpoint.MessageHandler() {
				public void handleMessage(String message) {

					System.out.println("MESSAGE:" + message);
					message = message.trim();
					if (message.startsWith("{") && message.endsWith("}")) {

						Json j = Json.read(message);
						System.out.println("received:" + j);
						//if (j.get("database")==null) return;
						//j=j.get("database");
						String m_uuid = "";

						String apiname = JsonUtil.getStringFromJson(j, "api", "");


						Runnable r = new Runnable() {
							public void run() {
								Json result = Json.object().set("error", "api not found ");

								if (apiname.isEmpty()) {

								} else {

									IApiCommands api = apiMap.get(apiname);
									Json jcmd = j.get("jcmd");
									if (jcmd == null) result.set("error", "jcmd not found");

									if ((api != null) && (jcmd != null)) result = api.execJcmd(jcmd);
								}


								Json r = Json.object();
								r.set("uuid", j.get("uuid"));
								r.set("result", result);
								System.out.println("****RESULT:" + r);
								clientEndPoint.sendMessage("res:" + r);

							}
						};

						Thread t = new Thread(r);
						t.start();

					}
				}
			});


			for (String sx : apiMap.keySet()) {
				clientEndPoint.sendMessage("api:" + sx);

			}
			Thread.sleep(100);


		} catch (InterruptedException ex) {
			System.err.println("InterruptedException exception: " + ex.getMessage());
		} catch (URISyntaxException ex) {
			System.err.println("URISyntaxException exception: " + ex.getMessage());
		}

	}

	
	static public void  printTime() {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
		LocalDateTime now = LocalDateTime.now();  
		System.out.println(dtf.format(now));  
	}

	/***
	 * At a scheduled rate, check if the connection to the server socket (cmd) is open, if not try to connect
	 */
	static public void startRemoteConnectTask() {
	ScheduledExecutorService executorService;
	executorService = Executors.newSingleThreadScheduledExecutor();

	executorService.scheduleAtFixedRate(
			new Runnable() {
				public void run() {
					boolean reconnect=false;
					if (clientEndPoint!=null) {
						//if (!clientEndPoint.isOpen()) System.out.println("Session open="+clientEndPoint.isOpen());
						reconnect=!clientEndPoint.isOpen();
					}
					else reconnect=true;
					
					if (reconnect) connectToServer();
					//else System.out.println("Connected");
				}
			},
			0, 1, TimeUnit.SECONDS);
	
	}
	
	
	static public void startTest() {
		ScheduledExecutorService executorService;
		executorService = Executors.newSingleThreadScheduledExecutor();
		executorService.scheduleAtFixedRate(
				new Runnable() {
					public void run() {
						Json j2 = Json.object();
						j2.set("line", "Test console");
						j2.set("console_id","learn");
						//			CSLWebSocketForConsole.broadcastMessageJson("log", j);
						
						System.out.println(j2);
						CSLWebSocket.broadcastMessageJson(CSLWebSocket.WEB_SOCKET_CONSOLE,j2 );
					
					}
				},
				0, 1, TimeUnit.SECONDS);
		
		}
	
	
	static void test() {
		Json j2 = Json.object();
		j2.set("line", "Test console");
		j2.set("console_id","learn");
		//			CSLWebSocketForConsole.broadcastMessageJson("log", j);
		CSLWebSocket.broadcastMessageJson(CSLWebSocket.WEB_SOCKET_CONSOLE,j2 );
	
	}
	
	
    public static void main(String[] args) {

    	org.eclipse.jetty.util.log.Log.setLog(new NoLogging());

    	Json configObj =CSLContext.instance.getConfig();
    	CSLContext.instance.init(new CSLRunningArgs().parseArgs(args).setHasIdsRunner(true));

		CSLContext.instance.setDebug(true);

		// region -- read configuration
		// This is the client, override configuration is needed not to launch servers
    	configObj.get("database_server_conf").set("on", false);
    	configObj.get("web_server_conf").set("on", false);
    	configObj.get("udp_server_conf").set("on", true);

		// The proxy server to connect
		SERVER_IP = JsonUtil.getStringFromJson(configObj, "global/ip_server_remote", "127.0.0.1");
		SERVER_URL_PREFIX = JsonUtil.getStringFromJson(configObj, "global/server_remote_url_prefix", "");
		if (SERVER_URL_PREFIX == null) {
			SERVER_URL_PREFIX = "";
		}

		Boolean force_host_name_resolution = JsonUtil.getBooleanFromJson(configObj, "global/force_host_name_resolution", false);
		// Try to resolve host name (mainly for Docker hostnames)
		if (force_host_name_resolution) {
			try {
				SERVER_IP = InetAddress.getByName(SERVER_IP).getHostAddress();
			} catch (UnknownHostException e) {
				System.out.println("[ERROR] " + e.getMessage());
			}
		}
		SERVER_PORT= JsonUtil.getIntFromJson(configObj, "global/port_server_remote", 0);
		USE_SSL = JsonUtil.getBooleanFromJson(configObj, "global/use_ssl", false);
		API_KEY = JsonUtil.getStringFromJson(configObj, "global/api_key", "");
		// endregion -- read configuration

		boolean USE_BROKER=false;

		JServiceLoader.setModuleName("IDS",new MosquittoConfig().setUseBroker(USE_BROKER));

		JServiceLoader.registerService(new CSLServiceDemo(), configObj, true);
		JServiceLoader.registerService(new CSLServiceIDS(), configObj, true);
		JServiceLoader.registerService(new AlertsService(), configObj, true);
		JServiceLoader.registerService(new MonitorService(), configObj, true);
		JServiceLoader.registerService(new TapsServices(), configObj, true);
		JServiceLoader.registerService(new CSLServiceJsonDataBase(), configObj, true);

		JServiceLoader.registerService(new NmapServices(), configObj, true);
			

		iniServices();

		startRemoteConnectTask();	// connect/reconnect
    	CSLWebSocket.registerMessageBroadcaster(messageBroadcaster);
   
		
		CSLContext.instance.postInit(false,true);
		CSLContext.instance.start();

    	CSLContext.instance.getIdsRunner().start();
    	((CSLAlertManager) CSLContext.instance.getCSLAlertManager()).registerAlertForwarder(alertForwarder);
    	

		// FIXME: Client & Server are creating the same HTTP Server at the same port
		ApiHttpServer apiHttpServer = new ApiHttpServer().createServer(
				new InetSocketAddress(9000),
				JServiceLoader.getApiCommandsList(),
				new ApiGetHelp());
    }
}