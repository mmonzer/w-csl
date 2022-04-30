package com.csl.web.jcmdoversocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.csl.intercom.broker.ISocketMsgListener;
import com.csl.intercom.jsoncmd.JServiceLoader;
import com.xcsl.ids.IDSTrace;
import com.xcsl.json.Json;

public class CSLWebSocketForJcmd {


	public static  long uuidctr=0;
	private static final String RESPONSE = "response";
	public static long TIME_OUT=10000;



	public static String WEB_SOCKET_CMD="/cmd";


	//static HashMap<String, String> websocketTags = new HashMap<String, String >();

	// this map is shared between sessions and threads, so it needs to be thread-safe (http://stackoverflow.com/a/2688817)
	//static Map<String,Map<Session, String>> allSocketsUsernameMap = new ConcurrentHashMap<>();
	//static int nextUserNumber = 1; //Assign to username for next connecting user

	static Map<String,Session> sessionMap = new ConcurrentHashMap<>();

	static Map<String,Json> pendingMessages= new HashMap<>();

	static private int idebug=2;

	static  public boolean isDebug() {return idebug>1;}
	static public boolean isShowInfo() {return idebug>0;}
	static public void setDebugLevel(int d) {idebug=d;}


	static public String cleanSocketName(String name) {

		if (name.startsWith("/")) name=name.substring(1);
		return name.toLowerCase();
	}



	static public void addUser(String name,Session session) {


		//System.out.println(user.getUpgradeRequest().getRequestURI());
		//System.out.println(user.getUpgradeRequest().getRequestURI().getPath());
		//CSLContext.instance.logInfo("Connection :"+user);
		//String username = "User" + (nextUserNumber++);
		//CSLWebSocketForConsole.userUsernameMap.put(user, username);

		//String socketName=cleanSocketName(user.getUpgradeRequest().getRequestURI().getPath());

		name=name.toLowerCase();
		System.out.println("connect :"+name);

		Session s=sessionMap.get(name);

		if (s!=null) {
			try {
				s.disconnect();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			s.close();
		}

		sessionMap.put(name,session);	

	}






//	public static void  refresh(String name) {
//
//		Session session=sessionMap.get(name);
//		if (session==null) {
//			System.err.println("Invalid api name "+name+" client not connected");
//			return ;
//		}
//		System.out.println("Refresh socket "+name+": "+name+" open:"+session.isOpen());
//		Json jx=Json.object();
//		jx.set("refresh", name);
//
//		try {
//			if (session.isOpen())session.getRemote().sendString(jx.toString());
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//	}


	//Sends a message from one user to all users, along with a list of current usernames
	public static void broadcastMessageJson( String name, Json j) {


		System.out.println("TEST_BROADCAST:"+name+j);
		Session session=sessionMap.get(name);
		if (session==null) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			session=sessionMap.get(name);
			if (session==null) {
				System.err.println("Invalid api name "+name+" client not connected");
				return ;
			}
		}

		//	Json jx=Json.object();
		j.set("api", name);
		String s=j.toString();


		try {
	//		session.getRemote().sendString(s);
			session.getRemote().sendStringByFuture(s);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}


//	//Sends a message from one user to all users, along with a list of current usernames
//	public static void broadcastMessageString( String name,  String s) {
//
//		Session session=sessionMap.get(name);
//		if (session==null) {
//			System.err.println("Invalid api name "+name+" client not connected");
//			return ;
//		}
//
//
//		try {
//			session.getRemote().sendString(s);
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}


	public static long getUuid() {
		uuidctr++;
		if (uuidctr>Long.MAX_VALUE-10) uuidctr=0;
		return uuidctr;
	}


	public static void addApi(String apiName, Session user) {
		// TODO Auto-generated method stub
		addUser(apiName, user);
	}



	public static void removeUser(Session user) {
		// TODO Auto-generated method stub

		//System.out.println("Remove session ");
		List <String> keysToRemove = new ArrayList<String>();
		for (String key : sessionMap.keySet()) {

			//System.out.println(key + ":" + sessionMap.get(key));
			Session x= sessionMap.get(key);
			if (user.equals(x)) {
				keysToRemove.add(key);
				System.out.println("Remove session :"+key);
			}

		}
		for (String k:keysToRemove) 
			sessionMap.remove(k);
		//to del keys
	}


	public static Json execJCmd(String apiName, Json jCmd) {

		/*	jcmd.set("uuid", getUuid());

		System.out.println("Remote Exec Jcmd <"+apiName+"> "+jcmd);

		broadcastMessageJson(apiName, jcmd);
		return Json.object().set("info", "remote exec");
	}



	public static  Json execCmd(String apiName,Json jCmd) {*/

		
		startTimeOutDetector();

		Json fullMsg=Json.object();

		String key=""+getUuid();


		//fullMsg.set(REQUEST,jCmd);
		fullMsg.set("uuid",key);
		fullMsg.set("api",apiName);
		fullMsg.set("jcmd", jCmd);

		broadcastMessageJson(apiName, fullMsg);

		fullMsg.set("start_time",System.currentTimeMillis());

		pendingMessages.put(key,fullMsg);


		while (true) {

			try {
				Thread.sleep(3);
			//	if (isDebug()) System.out.println("wait for response:"+fullMsg);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return Json.object().set("error","timeout");
			}

			if (fullMsg.has(RESPONSE)) {
				if (isDebug()) System.out.println("*** "+fullMsg);
				pendingMessages.remove(key);
				Json rep= fullMsg.get(RESPONSE);
				if (rep.has("result")) return rep.get("result");
				return Json.object().set("error", "timeout");
			}

		}



	}


	static public void messageArrived(String message)  {
		if (isDebug()) System.out.println("x*************  message is : "+message);

		String s=message;

		try {


			Json j=Json.read(s);
			//System.out.println("JSON:"+j);

			String key="";

			if (j.has("uuid")) key=j.get("uuid").asString();

			if (!key.isEmpty()) {
				Json jo=pendingMessages.get(key);
				if (jo!=null) {
					jo.set(RESPONSE, j);
				}
			}

		} catch 
		(Exception e) {
			System.out.println(e);
		}
	}



	static boolean timeOutDetectorRunning=false;

	static void startTimeOutDetector() {
		if (timeOutDetectorRunning) return;
		timeOutDetectorRunning=true;

		ScheduledExecutorService executorService;
		executorService = Executors.newSingleThreadScheduledExecutor();
		executorService.scheduleAtFixedRate(
				new Runnable() {
					public void run() {
						long current_time = System.currentTimeMillis();
						//System.out.println("Time out detector at time "+current_time);
						List<String> toDel= new ArrayList<String>();

						for (Map.Entry<String,Json> entry : pendingMessages.entrySet()) {

							Json message=entry.getValue();
							long start_time=message.get("start_time").asLong();
							long end_time = start_time + TIME_OUT;
							if (end_time<current_time) {
								if (isDebug()) System.out.println("Time out:"+message);
								//toDel.add(entry.getKey());
								message.set(RESPONSE, Json.object().set("error","TIMEOUT"));
							}

						}

					}
				},
				0, 1, TimeUnit.SECONDS);
	}
}
