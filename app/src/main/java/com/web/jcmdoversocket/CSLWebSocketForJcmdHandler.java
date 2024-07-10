package com.csl.web.jcmdoversocket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import com.csl.alert.CSLAlertManager;
import com.csl.core.CSLContext;
import com.csl.logger.CSLLogger;
import com.csl.web.websockets.CSLWebSocket;
import com.ucsl.json.Json;

@WebSocket
public class CSLWebSocketForJcmdHandler {

    private String sender, msg;
     
    @OnWebSocketConnect
    public void onConnect(Session user) throws Exception {
    	//CSLContext.instance.logInfo("Connection :"+user);
       
    
    	System.out.println("Connect Jcmd module :"+user);
    	//CSLWebSocketForJcmd.addUser(user);
       
        
    }

    @OnWebSocketClose
    public void onClose(Session user, int statusCode, String reason) {
       // String username = CSLWebSocketForAlert.userUsernameMap.get(user);
       // CSLWebSocketForAlert.userUsernameMap.remove(user);
        CSLWebSocketForJcmd.removeUser(user);
    }

    @OnWebSocketMessage
    public void onMessage(Session user, String message) {
      
    	//System.out.println("Jcmd module received message="+message);
    	message=message.trim();
    	if (message.startsWith("api:")) {
    		String apiName=message.substring(4);
    		CSLWebSocketForJcmd.addApi(apiName,user);
    	}
    	else if (message.startsWith("res:{")&&message.endsWith("}")) {
    		message=message.substring(4);
    		//System.out.println("Response:"+message);
    		//String apiName=message.substring(4);
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
    		
    		//System.out.println("SEND TO SOCKET <"+tag+">:"+msg);
    		//System.err.println("TODO");
    		//String apiName=message.substring(4);
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
    		
    		//System.out.println("SEND TO SOCKET <"+tag+">:"+j);
    		//System.err.println("TODO");
    		//String apiName=message.substring(4);
    		CSLWebSocket.broadcastMessageJson(tag,j);
    		}
    		
    	}
    	else if (message.startsWith("alert:")) {
    		message=message.substring(6);
//    		int n=message.indexOf(":");
//    		if (n<0) {
//    			System.err.println("Invalid msg:"+message);
//    		}
//    		else {
//    			String tag= message.substring(0, n);
//    			String msg= message.substring(n+1, message.length());
    		
    		Json j= Json.read(message);
    		System.err.println("FORWARD ALERT FROM CLIENT TO UDP="+j);
    		((CSLAlertManager) CSLContext.instance.getCSLAlertManager()).sendAlertToViewerUDP(j);
    		
    		//System.out.println("SEND TO SOCKET <"+tag+">:"+j);
    		//System.err.println("TODO");
    		//String apiName=message.substring(4);
    		//CSLWebSocket.broadcastMessageJson(tag,j);
    		//}
    		
    	}
    	else {
    		System.err.println("Jcmd module Invalid message:"+message);
    	}
    	
    }
    
    
   

}
