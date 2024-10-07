package com.csl.web.websockets;

import com.csl.core.CSLContext;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.csl.web.websockets.CSLWebSocket.cleanSocketName;

/**
 * Web socket handler for the HMI. Possible multiple sessions.
 */
@WebSocket
public class CSLWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(CSLWebSocketHandler.class);

    @OnWebSocketConnect
    public void onConnect(Session user) throws Exception {
        logger.trace("Connection :"+user);
        logger.info("A new user has connected to the CSLWebSocketHandler websocket through path {}", cleanSocketName(user.getUpgradeRequest().getRequestURI().getPath()));
    	CSLWebSocket.addUser(user);
    }

    @OnWebSocketClose
    public void onClose(Session user, int statusCode, String reason) {
        logger.info("Disconnected from User Client websocket though path {}", cleanSocketName(user.getUpgradeRequest().getRequestURI().getPath()));
        CSLWebSocket.removeUser(user);
    }

    @OnWebSocketMessage
    public void onMessage(Session user, String message) {
    	System.out.println("OnMessage user="+user+" message="+message);
    }

}
