package com.csl.web;

import com.csl.core.Config;
import com.csl.intercom.jsoncmd.JServiceLoader;
import com.csl.logger.CSLNetworkLogger;
import com.csl.logger.LoggerInterfaces;
import jakarta.websocket.*;
import main.CSLIDSMainClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
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
    private static final WebSocketContainer container = ContainerProvider.getWebSocketContainer();
    private static final AtomicBoolean isConnecting = new AtomicBoolean(false);
    LocalDateTime lastConnectionDateTime = null;

    public static class Configurator extends ClientEndpointConfig.Configurator {
        @Override
        public void beforeRequest(Map<String, List<String>> headers) {
            if (apiKey != null) {
                headers.put("Authorization", List.of("Api-Key " + apiKey));
            }
        }
    }

    public static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            System.out.println("API Key is too short to mask.");
            return null;
        }

        String firstPart = apiKey.substring(0, 4); // First 4 characters
        String lastPart = apiKey.substring(apiKey.length() - 4); // Last 4 characters
        String maskedKey = firstPart + "****" + lastPart;

        return maskedKey;
    }


    public void connect() {
        synchronized (WebsocketClientEndpoint.class) { // Ensures thread-safety
            logger.debug("Attempting to connect to WebSocket server at {} with API Key {}", endpointURI, maskApiKey(apiKey));

            if (isOpen()) {
                logger.info("WS is already connected, skipping reconnect");
                return;
            }
            LocalDateTime currentDateTime =  LocalDateTime.now();
            if (lastConnectionDateTime!=null && Duration.between(lastConnectionDateTime, currentDateTime).getSeconds()<10) {;
                logger.info("WebSocket connection cooling down, skipping reconnect.");
                return;
            }

            if (!isConnecting.compareAndSet(false, true)) {
                logger.info("WebSocket connection is already in progress, skipping reconnect.");
                return;
            }

            try {
                logger.info("Attempting to open websocket at {}", endpointURI);
                lastConnectionDateTime = currentDateTime;
                this.userSession = container.connectToServer(this, endpointURI);
                // TODO : UpgradeWebsocketException thrown but also logged. Need cleaning.
            } catch (Exception e) {
                isConnecting.set(false); // Reset the flag regardless of success or failure
                logger.warn("Exception occurred when connecting to WebSocket {}: {}", endpointURI, e.getMessage());
            }
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

        isConnecting.set(false);

        // register endpoints
        logger.info("Registering API endpoints with the server");
        JServiceLoader.apiMap.keySet().forEach(apiName -> sendMessageIfOpen("api:" + apiName));
        // TODO : clean  cyclic imports to send api names from here and not from the MainCLient.connectIfNecessary
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
        isConnecting.set(false);
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
        isConnecting.set(false);
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