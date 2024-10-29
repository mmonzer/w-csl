package com.csl.web.websockets;

import com.csl.intercom.broker.ISocketMsgListener;
import com.csl.intercom.jsoncmd.JServiceLoader;
import com.ucsl.json.Json;
import jakarta.websocket.Session;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * The CSLWebSocket class manages WebSocket connections, broadcasts messages,
 * and integrates with the broker for inter-module communication.
 */
public class CSLWebSocket {

    public static boolean VIA_BROKER = true;

    public static final String WEB_SOCKET_ALERT = "/alerts";
    public static final String WEB_SOCKET_CONSOLE = "/console";
    public static final String WEB_SOCKET_DATABASE = "/database";
    public static final String WEB_SOCKET_VARIABLES = "/chat";

    // Maps for managing WebSocket connections and tags
    private static final HashMap<String, String> websocketTags = new HashMap<>();
    private static final Map<String, Map<Session, String>> allSocketsUsernameMap = new ConcurrentHashMap<>();
    private static final Set<Session> sessions = new CopyOnWriteArraySet<>();
    private static int nextUserNumber = 1; // Assign to username for the next connecting user

    // IMessageBroadcaster instance for broadcasting messages
    private static IMessageBroadcaster messageBroadcaster = new IMessageBroadcaster() {
        @Override
        public void broadcastMessageString(String socketName, String message) {
            System.out.println("Broadcasting string to <" + socketName + ">: " + message);
            Map<Session, String> socketUsernameMap = getSocketUsernameMap(socketName);
            socketUsernameMap.keySet().stream().filter(Session::isOpen).forEach(session -> {
                try {
                    session.getAsyncRemote().sendText(message);
                } catch (Exception e) {
                    // e.printStackTrace();
                }
            });
        }

        @Override
        public void broadcastMessageJson(String socketName, Json jsonMessage) {
            String tag = websocketTags.get(socketName);
            if (tag == null) {
                System.err.println("Invalid socket name: " + socketName);
                return;
            }

            Json jsonWrapper = Json.object();
            jsonWrapper.set(tag, jsonMessage);
            String messageString = jsonWrapper.toString();

            if (VIA_BROKER) {
                JServiceLoader.getCSLInterModuleCommunicationManager().sendSocketMsg(socketName, messageString);
            } else {
                for (Session session : sessions)  {
                    session.getAsyncRemote().sendText(messageString);
                }
            }
        }
    };

    /**
     * Registers a custom message broadcaster.
     *
     * @param broadcaster The IMessageBroadcaster instance to register.
     */
    public static void registerMessageBroadcaster(IMessageBroadcaster broadcaster) {
        messageBroadcaster = broadcaster;
    }

    /**
     * Registers all WebSocket paths and integrates with the broker if enabled.
     */
    public static void registerAll() {
        VIA_BROKER = JServiceLoader.getCSLInterModuleCommunicationManager().isUseBroker();

        register(WEB_SOCKET_ALERT, "alert");
        register(WEB_SOCKET_CONSOLE, "loginfo");
        register(WEB_SOCKET_DATABASE, "database");
        register(WEB_SOCKET_VARIABLES, "userMessage");

        if (VIA_BROKER) {
            ISocketMsgListener listener = (websocketName, message) -> {
                Map<Session, String> socketUsernameMap = getSocketUsernameMap(websocketName);
                socketUsernameMap.keySet().stream().filter(Session::isOpen).forEach(session -> {
                    try {
                        session.getAsyncRemote().sendText(message);
                    } catch (Exception e) {
                        // e.printStackTrace();
                    }
                });
            };
            JServiceLoader.getCSLInterModuleCommunicationManager().registerSocketMsgListener(listener);
        }
    }

    /**
     * Cleans and standardizes the WebSocket path name.
     *
     * @param name The original WebSocket path name.
     * @return The cleaned and standardized WebSocket path name.
     */
    public static String cleanSocketName(String name) {
        if (name.startsWith("/")) name = name.substring(1);
        return name.toLowerCase();
    }

    /**
     * Gets the list of registered WebSocket paths.
     *
     * @return A list of WebSocket paths.
     */
    public static List<String> getListOfWebsocketsPath() {
        return new ArrayList<>(websocketTags.keySet());
    }

    /**
     * Registers a WebSocket path with an associated tag.
     *
     * @param socketName The WebSocket path name.
     * @param socketTag  The tag associated with the WebSocket.
     */
    public static void register(String socketName, String socketTag) {
        websocketTags.put(socketName, socketTag);
    }

    /**
     * Adds a user session to the specified WebSocket path.
     *
     * @param session The user session to add.
     */
    public static void addUser(Session session) {
        sessions.add(session);
        String username = "User" + (nextUserNumber++);
        String socketName = cleanSocketName(session.getRequestURI().getPath());

        Map<Session, String> socketUsernameMap = getSocketUsernameMap(socketName);
        socketUsernameMap.put(session, username);
    }

    /**
     * Removes a user session from the specified WebSocket path.
     *
     * @param session The user session to remove.
     */
    public static void removeUser(Session session) {
        sessions.remove(session);
        String socketName = cleanSocketName(session.getRequestURI().getPath());

        Map<Session, String> socketUsernameMap = getSocketUsernameMap(socketName);
        socketUsernameMap.remove(session);
    }

    /**
     * Gets the map of user sessions associated with a WebSocket path.
     *
     * @param socketName The WebSocket path name.
     * @return The map of user sessions and their associated usernames.
     */
    public static Map<Session, String> getSocketUsernameMap(String socketName) {
        socketName = cleanSocketName(socketName);

        return allSocketsUsernameMap.computeIfAbsent(socketName, k -> new ConcurrentHashMap<>());
    }

    /**
     * Refreshes the WebSocket by sending a refresh message to all connected sessions.
     *
     * @param socketName The WebSocket path name.
     */
    public static void refresh(String socketName) {
        Map<Session, String> socketUsernameMap = getSocketUsernameMap(socketName);
        socketUsernameMap.keySet().forEach(session -> {
            String username = socketUsernameMap.get(session);

            Json refreshMessage = Json.object().set("refresh", socketName);
            if (session.isOpen()) {
                    session.getAsyncRemote().sendText(refreshMessage.toString());
            }
        });
    }

    /**
     * Broadcasts a JSON message to all users connected to a specific WebSocket.
     *
     * @param socketName  The WebSocket path name.
     * @param jsonMessage The JSON message to broadcast.
     */
    public static void broadcastMessageJson(String socketName, Json jsonMessage) {
        messageBroadcaster.broadcastMessageJson(socketName, jsonMessage);
    }

    /**
     * Broadcasts a string message to all users connected to a specific WebSocket.
     *
     * @param socketName The WebSocket path name.
     * @param message    The string message to broadcast.
     */
    public static void broadcastMessageString(String socketName, String message) {
        messageBroadcaster.broadcastMessageString(socketName, message);
    }
}
