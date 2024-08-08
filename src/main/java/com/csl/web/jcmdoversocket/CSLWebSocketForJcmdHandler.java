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

/**
 * Web socket to connect the CSL-Client
 */
@WebSocket
public class CSLWebSocketForJcmdHandler {
	private static final Logger logger = LoggerFactory.getLogger(CSLWebSocketForJcmdHandler.class);
     
    @OnWebSocketConnect
    public void onConnect(Session session) throws Exception {
		logger.info("Connected to CSL-Client websocket");
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
		logger.info("Disconnected from CSL-Client websocket ({}) : {}", statusCode, reason);
		CSLWebSocketForJcmd.removeUser(session);

		}

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
    	message=message.trim();
    	if (message.startsWith("api:")) {
    		String apiName=message.substring(4);
    		CSLWebSocketForJcmd.addApi(apiName,session);
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
		else if (message.startsWith("keep alive")) {}
    	else {
    		System.err.println("Jcmd module Invalid message:"+message);
    	}
    	
    }
    
    
   

}
