package main.xcom;

import com.csl.core.Config;
import com.csl.logger.CSLNetworkLogger;
import com.csl.logger.LoggerInterfaces;
import jakarta.websocket.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@ClientEndpoint(subprotocols = {"xsCrossfire"}, configurator = WebsocketClientEndpoint.Configurator.class)
public class WebsocketClientEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(WebsocketClientEndpoint.class);
    public static final String WEBSOCKET_CONNECTION = "websocket/connection";
    Session userSession = null;
    private MessageHandler messageHandler;
    private URI endpointURI;
    private static String apiKey;
    private boolean connected = false;
    private static final WebSocketContainer container = ContainerProvider.getWebSocketContainer();

    public static class Configurator extends ClientEndpointConfig.Configurator {
        @Override
        public void beforeRequest(Map<String, List<String>> headers) {
            if (apiKey != null) {
                headers.put("Authorization", List.of("Api-Key " + apiKey));
            }
        }
    }

    public synchronized void connect() {
        try {
            this.userSession = container.connectToServer(this, endpointURI);
            // TODO : UpgradeWebsocketException thrown but also logged. Need cleaning.
            if (!connected) {
                connected = true;
                logger.info("Connected to websocket {}", endpointURI);
            }
        } catch (Exception e) {
            if (connected) {
                connected = false;
                logger.warn("Error connecting to websocket {}", endpointURI);
            }
        }
    }

    public WebsocketClientEndpoint(URI endpointURI) {
        this(endpointURI, null);
    }

    public WebsocketClientEndpoint(URI endpointURI, String apiKey) {
        this.endpointURI = endpointURI;
        WebsocketClientEndpoint.apiKey = apiKey;
        connect();
    }

    /**
     * Callback hook for Connection open events.
     *
     * @param userSession the userSession which is opened.
     */
    @OnOpen
    public void onOpen(Session userSession) {
        CSLNetworkLogger.info(logger, WEBSOCKET_CONNECTION, LoggerInterfaces.WS.toString(), "Opening websocket " + userSession.getRequestURI());
        this.userSession = userSession;
        userSession.setMaxIdleTimeout(Config.instance.Server.getWebsocketTimeout());
        CSLNetworkLogger.debug(logger, WEBSOCKET_CONNECTION, LoggerInterfaces.WS.toString(), "Timeout = " + userSession.getMaxIdleTimeout());

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        CSLNetworkLogger.debug(logger, WEBSOCKET_CONNECTION, LoggerInterfaces.WS.toString(), "Sending message to websocket " + dtf.format(now));
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param userSession the userSession which is getting closed.
     * @param reason      the reason for connection close
     */
    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        CSLNetworkLogger.info(logger, WEBSOCKET_CONNECTION, LoggerInterfaces.WS.toString(), "Closing websocket " + userSession.getRequestURI());
        CSLNetworkLogger.debug(logger, WEBSOCKET_CONNECTION, LoggerInterfaces.WS.toString(), "Closing websocket " + userSession.getRequestURI() + " User session:" + userSession + " Reason:" + reason.getReasonPhrase());
        this.userSession = null;
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
    public void addMessageHandler(MessageHandler msgHandler) {
        this.messageHandler = msgHandler;
    }

    /**
     * Send a message.
     *
     * @param message
     */
    public void sendMessage(String message) {
        this.userSession.getAsyncRemote().sendText(message);
    }

    /**
     * Message handler.
     *
     * @author Jiji_Sasidharan
     */
    public static interface MessageHandler {

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