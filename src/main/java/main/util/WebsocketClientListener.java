package main.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * ChatServer Client
 *
 * @author Jiji_Sasidharan
 */
@ClientEndpoint
public class WebsocketClientListener {
	private static final Logger logger = LoggerFactory.getLogger(WebsocketClientListener.class);

	static List<WebsocketClientListener> list = new ArrayList<WebsocketClientListener>();

	Session userSession = null;
	private MessageHandler messageHandler;
	String uriName="";

	static public WebsocketClientListener get(String s) {

		for (WebsocketClientListener w:list) {
			if (w.uriName.compareTo(s)==0) {
				logger.info("Socket already open :"+s);
				return w;
			}
		}
		try {
			WebsocketClientListener w = new WebsocketClientListener(s,new URI(s)); 
			logger.info("Socket created :"+s);
			
			list.add(w);
			return w;
			
		} catch (URISyntaxException ex) {
			System.err.println("URISyntaxException exception: " + s);
			logger.error("URISyntaxException exception: " + s);
			
		}

		return null;
	}

	private void remove(String s) {
		
		List<WebsocketClientListener> list2 = new ArrayList<WebsocketClientListener>(); 
		
		for (WebsocketClientListener w:list) {
			if (w.uriName.compareTo(s)==0) {
				list2.add(w);
				logger.info("Socket deleted :"+w.uriName);
			}
		}
		
		list.removeAll(list2);
	}

	private WebsocketClientListener(String name, URI endpointURI) {
		try {
			WebSocketContainer container = ContainerProvider.getWebSocketContainer();
			this.uriName=name;
			container.connectToServer(this, endpointURI);
		} catch (Exception e) {
			System.err.println("Cannot connect to "+endpointURI);
			logger.error("Cannot connect to "+endpointURI);
		}
	}

	/**
	 * Callback hook for Connection open events.
	 *
	 * @param userSession the userSession which is opened.
	 */
	@OnOpen
	public void onOpen(Session userSession) {
		logger.info("Opening websocket listener "+userSession.getRequestURI());
		this.userSession = userSession;
	}

	/**
	 * Callback hook for Connection close events.
	 *
	 * @param userSession the userSession which is getting closed.
	 * @param reason the reason for connection close
	 */
	@OnClose
	public void onClose(Session userSession, CloseReason reason) {
		logger.info("closing websocket listener ");
		this.userSession = null;

		remove(uriName);
		
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
}