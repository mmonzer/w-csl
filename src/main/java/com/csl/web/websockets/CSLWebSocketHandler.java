package com.csl.web.websockets;

import com.csl.core.CSLContext;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebSocket
public class CSLWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(CSLWebSocketHandler.class);
    private String sender, msg;

    @OnWebSocketConnect
    public void onConnect(Session user) throws Exception {
        logger.info("A new user has connected to the CSLWebSocketHandler websocket");
        logger.trace("Connection :"+user);

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
