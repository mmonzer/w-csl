package com.csl.web.websockets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jetty.websocket.api.Session;

import com.csl.intercom.broker.ISocketMsgListener;
import com.csl.intercom.jsoncmd.JServiceLoader;
import com.xcsl.ids.IDSTrace;
import com.xcsl.json.Json;

public class CSLWebSocket {
	
	static public boolean VIA_BROKER=true; 
	
	public static String WEB_SOCKET_ALERT="/alerts";
	public static String WEB_SOCKET_CONSOLE="/console";
	public static String WEB_SOCKET_DATABASE="/database";
	public static String WEB_SOCKET_VARIABLES="/chat";


	static HashMap<String, String> websocketTags = new HashMap<String, String >();

    // this map is shared between sessions and threads, so it needs to be thread-safe (http://stackoverflow.com/a/2688817)
    static Map<String,Map<Session, String>> allSocketsUsernameMap = new ConcurrentHashMap<>();
    static int nextUserNumber = 1; //Assign to username for next connecting user

    
    static public void registerAll() {
    	
    	
    	VIA_BROKER=JServiceLoader.getCSLInterModuleCommunicationManager().isUseBroker();
    	
    	register(WEB_SOCKET_ALERT, "alert");
    	register(WEB_SOCKET_CONSOLE, "loginfo");
    	register(WEB_SOCKET_DATABASE, "database");
    	register(WEB_SOCKET_VARIABLES, "userMessage");
    	
    	if (VIA_BROKER) {
    		
    		ISocketMsgListener is = new ISocketMsgListener() {
    			
    			@Override
    			public void messageArrived(String websocketName, String msg) {
    				// TODO Auto-generated method stub
    				System.out.println("Forwarding to websocket:"+websocketName);
    				System.out.println("Message:"+msg);
    				Map<Session, String> socketUsernameMap=getSocketUsernameMap(websocketName);
    				socketUsernameMap.keySet().stream().filter(Session::isOpen).forEach(session -> {
    		            try {
    		            	
    		            	IDSTrace.log(IDSTrace.WEB_SOCKET, "SEND JSON  "+msg);
    		            	session.getRemote().sendString(msg);
    		                
    		            } catch (Exception e) {
    		                e.printStackTrace();
    		            }
    		        });
    			}
    		};
    		
    		JServiceLoader.getCSLInterModuleCommunicationManager().registerSocketMsgListener(is);
    	}
    	
    }
    
    static public String cleanSocketName(String name) {
    	
    	if (name.startsWith("/")) name=name.substring(1);
    	return name.toLowerCase();
    }
    
    static public List<String> getListOfWebsocketsPath() {
    	return  new ArrayList<>(websocketTags.keySet());
    }
    
    static public void register(String socketName, String socketTag) {
    	
    	websocketTags.put(socketName, socketTag);
    	
    }
    
    
    static public void addUser(Session user) {
    	
    	
    	//System.out.println(user.getUpgradeRequest().getRequestURI());
    	//System.out.println(user.getUpgradeRequest().getRequestURI().getPath());
    	//CSLContext.instance.logInfo("Connection :"+user);
        String username = "User" + (nextUserNumber++);
        //CSLWebSocketForConsole.userUsernameMap.put(user, username);
        
        String socketName=cleanSocketName(user.getUpgradeRequest().getRequestURI().getPath());
        
        System.out.println("connect :"+user+" to "+socketName);
    	
        Map<Session, String> socketUsernameMap=getSocketUsernameMap(socketName);
       
        socketUsernameMap.put(user,username);	
        		
    }

    
    static public void removeUser(Session user) {
    	
    	
    	String socketName=cleanSocketName(user.getUpgradeRequest().getRequestURI().getPath());
        
        System.out.println("disconnnect :"+user+" from "+socketName);
    	
        Map<Session, String> socketUsernameMap=getSocketUsernameMap(socketName);
        
        socketUsernameMap.remove(user);	
        		
    }

    
    static public Map<Session, String> getSocketUsernameMap(String socketName) {
    	socketName=cleanSocketName(socketName);
    	
    	Map<Session, String> socketUsernameMap=allSocketsUsernameMap.get(socketName);
        if (socketUsernameMap==null) {
        	socketUsernameMap=new ConcurrentHashMap<>();
        	allSocketsUsernameMap.put(socketName,socketUsernameMap);
        }
        return socketUsernameMap;
    }
    
    public static void  refresh(String socketName) {

    	Map<Session, String> socketUsernameMap=getSocketUsernameMap(socketName);
		socketUsernameMap.keySet().stream().forEach(session -> {
			String name=socketUsernameMap.get(session);
			//System.out.println("Refresh socket "+socketName+": "+name+" open:"+session.isOpen());
			Json jx=Json.object();
			jx.set("refresh", socketName);

			try {
				if (session.isOpen())session.getRemote().sendString(jx.toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}
    
    
    //Sends a message from one user to all users, along with a list of current usernames
    public static void broadcastMessageJson( String socketName, Json j) {
    	
    	String tag= websocketTags.get(socketName);
    	if (tag==null) {
    		System.err.println("Invalid socket name "+socketName);
    	}
    	
    	Json jx=Json.object();
    	jx.set(tag, j);
    	String s=jx.toString();
    	
    	
    	if (VIA_BROKER) {
    		JServiceLoader.getCSLInterModuleCommunicationManager().sendSocketMsg(socketName, s);
    	}
    	else {
    	Map<Session, String> socketUsernameMap=getSocketUsernameMap(socketName);
		socketUsernameMap.keySet().stream().filter(Session::isOpen).forEach(session -> {
            try {
            	
            	IDSTrace.log(IDSTrace.WEB_SOCKET, "SEND JSON  "+s);
            	session.getRemote().sendString(s);
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    	}
    }
    
    
  //Sends a message from one user to all users, along with a list of current usernames
  public static void broadcastMessageString( String socketName,  String s) {
  	
  	//System.out.println("Console out:"+j);
  	
	  Map<Session, String> socketUsernameMap=getSocketUsernameMap(socketName);
	socketUsernameMap.keySet().stream().filter(Session::isOpen).forEach(session -> {
          try {
          	
          	IDSTrace.log(IDSTrace.WEB_SOCKET, "SEND String "+s);
          	System.out.println("SEND String "+s);
              session.getRemote().sendString(s);
                  //.put("userlist", userUsernameMap.values())
             // ));
          } catch (Exception e) {
              e.printStackTrace();
          }
      });
  }
}
