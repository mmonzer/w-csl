package com.csl.web;

import com.csl.core.Config;
import com.csl.logger.CSLNetworkLogger;
import com.csl.logger.LoggerInterfaces;
import jakarta.websocket.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@ClientEndpoint(subprotocols = {"xsCrossfire"}, configurator = WebsocketClientEndpoint.Configurator.class)
public class WebsocketClientEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(WebsocketClientEndpoint.class);
    public static final String WEBSOCKET_CONNECTION = "websocket/connection";
    Session userSession = null;
    private MessageHandler messageHandler;
    private URI endpointURI;
    private static String apiKey;
    private boolean connectedFlagForLogs = false;
    private static final WebSocketContainer container = ContainerProvider.getWebSocketContainer();
    private static boolean isConnecting = false;

    public static class Configurator extends ClientEndpointConfig.Configurator {
        @Override
        public void beforeRequest(Map<String, List<String>> headers) {
            if (apiKey != null) {
                headers.put("Authorization", List.of("Api-Key " + apiKey));
            }
        }
    }

    public void connect() {
        if (isOpen()) {
            logger.info("WS is already connected, skipping reconnect");
            return;
        }
        if (isConnecting) {
            logger.info("WS is connecting, skipping reconnect");
            return;
        }
        isConnecting = true;
        try {
            logger.info("Opening websocket at {}", endpointURI);
            this.userSession = container.connectToServer(this, endpointURI);
            // TODO : UpgradeWebsocketException thrown but also logged. Need cleaning.
        } catch (Exception e) {
            isConnecting = false;
            logger.warn("Exception occurred when connecting to websocket {} : {}", endpointURI, e.getMessage());
        }
    }

    public WebsocketClientEndpoint(URI endpointURI) {
        this(endpointURI, null);
    }

    public WebsocketClientEndpoint(URI endpointURI, String apiKey) {
        this.endpointURI = endpointURI;
        WebsocketClientEndpoint.apiKey = apiKey;
    }

    /**
     * Callback hook for Connection open events.
     *
     * @param userSession the userSession which is opened.
     */
    @OnOpen
    public synchronized void onOpen(Session userSession) {
        CSLNetworkLogger.info(logger, WEBSOCKET_CONNECTION, LoggerInterfaces.WS.toString(), "Opened websocket " + userSession.getRequestURI() +" : "+ userSession);
        logger.info("Connected to WCSL websocket {}", endpointURI);
        this.userSession = userSession;
        userSession.setMaxIdleTimeout(Config.instance.Server.getWebsocketTimeout());
        CSLNetworkLogger.debug(logger, WEBSOCKET_CONNECTION, LoggerInterfaces.WS.toString(), "Timeout = " + userSession.getMaxIdleTimeout() +" : "+ userSession);

        isConnecting = false;
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param userSession the userSession which is getting closed.
     * @param reason      the reason for connection close
     */
    @OnClose
    public synchronized void onClose(Session userSession, CloseReason reason) {
        CSLNetworkLogger.warn(logger, WEBSOCKET_CONNECTION, LoggerInterfaces.WS.toString(), "Closing websocket " + userSession.getRequestURI()+ " Reason:" + reason.getReasonPhrase() +" : "+ userSession);
        this.userSession = null;
        isConnecting = false;
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param session the userSession that got an error.
     * @param error      throwable
     */
    @OnError
    public synchronized void onError(Session session, Throwable error) {
        logger.error("Connection lost in WS with server with error : {}",(error.getMessage()!=null)?error.getMessage(): "unknown");
        this.userSession = null;
        isConnecting = false;
    }

    /**
     * Callback hook for Message Events. This method will be invoked when a client send a message.
     *
     * @param message The text message
     */
    @OnMessage
    public void onMessage(String message) {
        if (this.messageHandler != null) {
            this.messageHandler.handleMessage(message);
        }
    }

    /**
     * register message handler
     *
     * @param msgHandler
     */
    public void setMessageHandler(MessageHandler msgHandler) {
        this.messageHandler = msgHandler;
    }

    /**
     * Send a message.
     *
     * @param message message to send
     */
    public void sendMessage(String message) {
        this.userSession.getAsyncRemote().sendText(message);
    }

    /**
     * Send a message if the websocket is open
     *
     * @param message to send
     */
    public void sendMessageIfOpen(String message) {
        if (isOpen()) {
            this.sendMessage(message);
        }
    }

    /**
     * Message handler.
     *
     * @author Jiji_Sasidharan
     */
    public interface MessageHandler {

        public void handleMessage(String message);
    }

    public boolean isOpen() {
        if (userSession == null) {
            return false;
        }
        try {
            return userSession.isOpen();
        } catch (Exception ignored) {
            return false;
        }
    }
}