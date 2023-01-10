package main.xcom;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.websocket.*;

import com.ucsl.json.Json;


@ClientEndpoint(subprotocols = {"xsCrossfire"}, configurator = WebsocketClientEndpoint.Configurator.class)
public class WebsocketClientEndpoint {

    Session userSession = null;
    private MessageHandler messageHandler;
    private URI endpointURI;
    public static String apiKey = null;

    public static class Configurator extends javax.websocket.ClientEndpointConfig.Configurator {
        @Override
        public void beforeRequest(Map<String, List<String>> headers)
        {
//            String API_KEY = "FQ0dekrg.N5G8tw9On2UHrrncoPdhlmJCeEN4gwTp";

            if (apiKey != null) {
                System.out.println(" (Adding Api-Key to the headers: " + apiKey + ")");
                List<String> authvalues = new ArrayList<>();
                authvalues.add("Api-Key " + apiKey);
                headers.put("Authorization", authvalues);
            }
            super.beforeRequest(headers);
        }
    }
    
    public WebsocketClientEndpoint(URI endpointURI) {
       this.endpointURI=endpointURI;
       connect();
	}

    public WebsocketClientEndpoint(URI endpointURI, String apiKey) {
        this.endpointURI=endpointURI;
        this.apiKey = apiKey;
        connect();
    }

    private void connect()  {
    	 try {
             WebSocketContainer container = ContainerProvider.getWebSocketContainer();
             container.connectToServer(this, endpointURI);
         } catch (Exception e) {
            // throw new RuntimeException(e);
             System.err.println(e);
         }
    }
    
    /**
     * Callback hook for Connection open events.
     *
     * @param userSession the userSession which is opened.
     */
    @OnOpen
    public void onOpen(Session userSession) {
        System.out.println("Opening websocket "+userSession.getRequestURI());
        this.userSession = userSession;
       System.out.println("Timeout="+ userSession.getMaxIdleTimeout());
        userSession.setMaxIdleTimeout(60000);
         {
    		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
    		LocalDateTime now = LocalDateTime.now();  
    		System.out.println(dtf.format(now));  
    	}
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param userSession the userSession which is getting closed.
     * @param reason the reason for connection close
     */
    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        System.out.println("closing websocket");
        //System.out.println(userSession);
        System.out.println(reason);
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
		// TODO Auto-generated method stub
		if (userSession==null) return false;
		return userSession.isOpen();
	}
}