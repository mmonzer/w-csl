package com.csl.web.websockets;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.csl.web.websockets.CSLWebSocket.cleanSocketName;

/**
 * Web socket handler for the HMI. Possible multiple sessions.
 */

@ServerEndpoint(CSLWebSocket.WEB_SOCKET_CONSOLE)
public class CSLWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(CSLWebSocketHandler.class);
    private Session session;

    @OnClose
    public void onWebSocketClose(CloseReason close)
    {
        logger.info("Disconnected from User Client websocket though path {}", cleanSocketName(session.getRequestURI().getPath()));
        CSLWebSocket.removeUser(session);
    }

    @OnOpen
    public void onWebSocketConnect(Session session)
    {
        this.session = session;
        logger.trace("Connection : {}",session);
        logger.info("A new user has connected to the CSLWebSocketHandler websocket through path {}", cleanSocketName(session.getRequestURI().getPath()));
    	CSLWebSocket.addUser(session);
    }

    @OnMessage
    public void onWebSocketText(String message)
    {
        System.out.println("OnMessage user="+session+" message="+message);
    }

}
