package com.csl.web.jcmdoversocket;

import com.csl.core.CSLContext;
import com.csl.web.websockets.CSLWebSocket;
import com.csl.auth.SupportedClients;
import com.ucsl.json.Json;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Web socket to connect the CSL-Client
 */
@ServerEndpoint(CSLWebSocketForJcmd.WEB_SOCKET_CMD)
public class CSLWebSocketForJcmdHandler {
	private Session session;
	private static final Logger logger = LoggerFactory.getLogger(CSLWebSocketForJcmdHandler.class);
     
    @OnOpen
    public void onConnect(Session session) {
                logger.info("A new session has connected to the CSLWebSocketForJcmdHandler websocket  : {}", session);
                logger.trace("Connection : {}", session);

                this.session = session;

                String clientId = null;
                String password = null;
                try {
                        clientId = session.getRequestParameterMap().getOrDefault("id", java.util.List.of("")).get(0);
                        password = session.getRequestParameterMap().getOrDefault("password", java.util.List.of("")).get(0);
                } catch (Exception ignored) {}

                if (!SupportedClients.authenticate(clientId, password)) {
                        logger.warn("Rejecting WS connection for client {}", clientId);
                        try {
                                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "Authentication failed"));
                        } catch (Exception e) {
                                logger.error("Error closing session", e);
                        }
                        return;
                }

                CSLWebSocketForJcmd.addApi(clientId, session);
                CSLWebSocketForJcmd.startKeepAlive();
        }

    @OnClose
    public void onClose(CloseReason close) {
		logger.info("Disconnected from CSL-Client websocket : {}", close.getReasonPhrase());
		CSLWebSocketForJcmd.removeUser(session);
		CSLWebSocketForJcmd.stopKeepAlive();
	}

	@OnError
	public void onError(Session session, Throwable error) {
		logger.error("Error on websocket {} : {}", session, error.getMessage());
	}

    @OnMessage
    public void onMessage(String message) {
		message=message.trim();
        if (message.startsWith("res:{")&&message.endsWith("}")) {
                message=message.substring(4);
                CSLWebSocketForJcmd.messageArrived(message);
        }
    	
    	else if (message.startsWith("wss:")) {
    		message=message.substring(4);
    		int n=message.indexOf(":");
    		if (n<0) {
                logger.error("Invalid msg:{}", message);
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
                logger.error("Invalid msg:{}", message);
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
			logger.warn("deprecated : forward alert to user");
			logger.debug("deprecated : forward alert to user {}", j);
    		CSLContext.getInstance().getCSLAlertManager().sendAlertToViewerUDP(j);
    	}
		else if (message.compareToIgnoreCase("keep alive") == 0)  {
			// do nothing
		}
    	else {
            logger.warn("Jcmd module Invalid message:{}", message);
    	}
    	
    }
    
    
   

}
