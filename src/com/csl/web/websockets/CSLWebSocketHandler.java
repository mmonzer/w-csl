package com.csl.web.websockets;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import com.csl.core.CSLContext;

@WebSocket
public class CSLWebSocketHandler {

    private String sender, msg;

    @OnWebSocketConnect
    public void onConnect(Session user) throws Exception {
    	CSLContext.instance.logInfo("Connection :"+user);
       

    	System.out.println("Connect :"+user);
    	CSLWebSocket.addUser(user);
       
        
    }

    @OnWebSocketClose
    public void onClose(Session user, int statusCode, String reason) {
       // String username = CSLWebSocketForAlert.userUsernameMap.get(user);
       // CSLWebSocketForAlert.userUsernameMap.remove(user);
        CSLWebSocket.removeUser(user);
    }

    @OnWebSocketMessage
    public void onMessage(Session user, String message) {
      
    	System.out.println("OnMessage user="+user+" message="+message);
    }

}
