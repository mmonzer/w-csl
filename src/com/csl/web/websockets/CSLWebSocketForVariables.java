package com.csl.web.websockets;

import com.csl.core.CSLContext;
import com.ucsl.json.Json;
import org.eclipse.jetty.websocket.api.Session;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CSLWebSocketForVariables {
	
	static String socketName="VARIABLES";

	public static void  refresh() {

		userUsernameMap.keySet().stream().forEach(session -> {
			String name=userUsernameMap.get(session);
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

//	public static IVariableModificationListener listenerForWebsocket = new IVariableModificationListener() {
//		
//		@Override
//		public void modifying(String name, String path, String oldValue, String newValue) {
//			// TODO Auto-generated method stub
//			String msg ="{ \"name\":"+name+",\"old\":"+oldValue+",\"new\":"+newValue+"}";
//			Json jj=Json.object();
//			jj.at("time",CSLContext.instance.getTimeFromStartingTime());
//			jj.at("name",name);
//			//jj.at("oldValue",oldValue);
//			jj.at("newValue",newValue);
//			
//			
//			////System.out.println("Modif: "+jj.toString());
//			CSLWebSocketForVariables.broadcastMessage(jj.toString());
//		}
//	};
	

    // this map is shared between sessions and threads, so it needs to be thread-safe (http://stackoverflow.com/a/2688817)
    static Map<Session, String> userUsernameMap = new ConcurrentHashMap<>();
    static int nextUserNumber = 1; //Assign to username for next connecting user

//    public static void main(String[] args) {
//        staticFiles.location("/public"); //index.html is served at localhost:4567 (default port)
//        staticFiles.expireTime(600);
//        webSocket("/chat", ChatWebSocketHandler.class);
//        init();
//    }

    
    //Sends a message from one user to all users, along with a list of current usernames
    public static void broadcastMessage( String message) {
        userUsernameMap.keySet().stream().filter(Session::isOpen).forEach(session -> {
            try {
                session.getRemote().sendString(String.valueOf(new JSONObject()
                    .put("userMessage", message)
                    //.put("userlist", userUsernameMap.values())
                ));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    
    //Sends a message from one user to all users, along with a list of current usernames
    public static void broadcastMessage(String sender, String message) {
        userUsernameMap.keySet().stream().filter(Session::isOpen).forEach(session -> {
            try {
                session.getRemote().sendString(String.valueOf(new JSONObject()
                    .put("userMessage", createHtmlMessageFromSender(sender, message))
                    .put("userlist", userUsernameMap.values())
                ));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    //Builds a HTML element with a sender-name, a message, and a timestamp,
    private static String createHtmlMessageFromSender(String sender, String message) {
//        return article(
//            b(sender + " says:"),
//            span(attrs(".timestamp"), new SimpleDateFormat("HH:mm:ss").format(new Date())),
//            p(message)
//        )
        		return "Hello time="+CSLContext.instance.getTimeFromStartingTime();
    }

    
   
    
    
//    //Sends a message from one user to all users, along with a list of current usernames
//    public static void broadcastMessageJson( String tag, Json j) {
//        userUsernameMap.keySet().stream().filter(Session::isOpen).forEach(session -> {
//            try {
//            	
//            	String s=String.valueOf(new JSONObject()
//                        .put(tag, j.toString())
//                        .put("userMessage","testtest"));
//            	System.out.println("SEND JSON "+s);
//                session.getRemote().sendString(s);
//                    //.put("userlist", userUsernameMap.values())
//               // ));
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
//    }
}
