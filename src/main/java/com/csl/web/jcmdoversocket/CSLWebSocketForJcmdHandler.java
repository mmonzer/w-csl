package com.csl.web.jcmdoversocket;

import com.csl.alert.CSLAlertManager;
import com.csl.core.CSLContext;
import com.csl.web.websockets.CSLWebSocket;
import com.ucsl.json.Json;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebSocket
public class CSLWebSocketForJcmdHandler {
	private static final Logger logger = LoggerFactory.getLogger(CSLWebSocketForJcmdHandler.class);

    private String sender, msg;
     
    @OnWebSocketConnect
    public void onConnect(Session user) throws Exception {
		logger.info("A new user has connected to the CSLWebSocketForJcmdHandler websocket");
		logger.trace("Connection :"+user);
    }

    @OnWebSocketClose
    public void onClose(Session user, int statusCode, String reason) {
        CSLWebSocketForJcmd.removeUser(user);
    }

    @OnWebSocketMessage
    public void onMessage(Session user, String message) {
    	message=message.trim();
    	if (message.startsWith("api:")) {
    		String apiName=message.substring(4);
    		CSLWebSocketForJcmd.addApi(apiName,user);
    	}
    	else if (message.startsWith("res:{")&&message.endsWith("}")) {
    		message=message.substring(4);
    		CSLWebSocketForJcmd.messageArrived(message);
    	}
    	
    	else if (message.startsWith("wss:")) {
    		message=message.substring(4);
    		int n=message.indexOf(":");
    		if (n<0) {
    			System.err.println("Invalid msg:"+message);
    		}
    		else {
    			String tag= message.substring(0, n);
    			String msg= message.substring(n+1, message.length());
    		CSLWebSocket.broadcastMessageString(tag, msg);
    		}
    		
    	}
    	else if (message.startsWith("wsj:")) {
    		message=message.substring(4);
    		int n=message.indexOf(":");
    		if (n<0) {
    			System.err.println("Invalid msg:"+message);
    		}
    		else {
    			String tag= message.substring(0, n);
    			String msg= message.substring(n+1, message.length());
    		Json j= Json.read(msg);
    		CSLWebSocket.broadcastMessageJson(tag,j);
    		}
    		
    	}
    	else if (message.startsWith("alert:")) {
    		message=message.substring(6);
    		
    		Json j= Json.read(message);
    		System.err.println("FORWARD ALERT FROM CLIENT TO UDP="+j);
    		((CSLAlertManager) CSLContext.instance.getCSLAlertManager()).sendAlertToViewerUDP(j);
    	}
		else if (message.compareToIgnoreCase("keep alive") == 0)  {
			// do nothing
		}
    	else {
    		System.err.println("Jcmd module Invalid message:"+message);
    	}
    	
    }
    
    
   

}
