package com.csl.web.websockets;

import com.csl.intercom.broker.ISocketMsgListener;
import com.csl.intercom.jsoncmd.JServiceLoader;
import com.ucsl.json.Json;
import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CSLWebSocket {
	
	static public boolean VIA_BROKER=true; 
	
	public static String WEB_SOCKET_ALERT="/alerts";
	public static String WEB_SOCKET_CONSOLE="/console";
	public static String WEB_SOCKET_DATABASE="/database";
	public static String WEB_SOCKET_VARIABLES="/chat";
	public static String WEB_SOCKET_CMD="/cmd";


	static HashMap<String, String> websocketTags = new HashMap<String, String >();

    // this map is shared between sessions and threads, so it needs to be thread-safe (http://stackoverflow.com/a/2688817)
    static Map<String,Map<Session, String>> allSocketsUsernameMap = new ConcurrentHashMap<>();
    static int nextUserNumber = 1; //Assign to username for next connecting user

    static IMessageBroadcaster messageBroadcaster= 
    		new IMessageBroadcaster() {
				
				@Override
				public void broadcastMessageString(String socketName, String s) {
					// TODO Auto-generated method stub
					System.out.println("Broadcast str <"+socketName+">"+s);
					Map<Session, String> socketUsernameMap=getSocketUsernameMap(socketName);
					socketUsernameMap.keySet().stream().filter(Session::isOpen).forEach(session -> {
				          try {
				          	System.out.println("SEND String "+s);
				              session.getRemote().sendString(s);
				          } catch (Exception e) {
				              e.printStackTrace();
				          }
				      });
				}
				
				@Override
				public void broadcastMessageJson(String socketName, Json j) {
					String tag= websocketTags.get(socketName);
			    	if (tag==null) {
			    		System.err.println("Invalid socket name "+socketName);
			    		return;
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
			            	session.getRemote().sendString(s);
			                
			            } catch (Exception e) {
			                e.printStackTrace();
			            }
			        });
			    	}
				}
			};
			
			
    
	static public void registerMessageBroadcaster(IMessageBroadcaster m) {
		messageBroadcaster=m;
	}
			
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
        String username = "User" + (nextUserNumber++);
        
        String socketName=cleanSocketName(user.getUpgradeRequest().getRequestURI().getPath());
        
        System.out.println("connect :"+socketName+ " username:"+username);
    	
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
			System.out.println("Refresh socket "+socketName+": "+name+" open:"+session.isOpen());
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
    	messageBroadcaster.broadcastMessageJson(socketName, j);
    }
    
    
  //Sends a message from one user to all users, along with a list of current usernames
  public static void broadcastMessageString( String socketName,  String s) {
	  messageBroadcaster.broadcastMessageString(socketName, s);
  }
}
