package com.csl.web.jcmdoversocket;

import com.csl.logger.LoggerConstants;
import com.csl.logger.LoggerInterfaces;
import com.ucsl.json.Json;
import jakarta.websocket.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.csl.logger.CSLNetworkLogger.*;
import static com.csl.logger.LoggerConstants.X_CORRELATION_ID;

/**
 * CSLWebSocketForJcmd handles WebSocket communication for Jcmd commands.
 * It manages sessions, sends messages, and processes responses with timeout management.
 */
public class CSLWebSocketForJcmd {
    private static final Logger logger = LoggerFactory.getLogger(CSLWebSocketForJcmd.class);
    private static final String RESPONSE = "response";
    public static final String ID = "uuid";
    public static final String COMMAND = "command";
    public static final String ERROR = "error";
    private static final long TIME_OUT = 60000;

    public static final String WEB_SOCKET_CMD = "/cmd";

    private static long uuidCounter = 0;

    // Concurrent map to manage WebSocket sessions by API name
    static Map<String, Session> sessionMap = new ConcurrentHashMap<>();
    private static final AtomicBoolean connected = new AtomicBoolean(false);

    // Map to keep track of pending messages and their status
    static Map<String, CompletableFuture<Json>> pendingMessages = new ConcurrentHashMap<>();

    /**
     * Adds a new user session associated with a specific API name.
     * If a session already exists for the user, it will be closed and replaced.
     *
     * @param name    The API name (user) associated with the session.
     * @param session The WebSocket session to be added.
     */
    public static void addUser(String name, Session session) {
        name = name.toLowerCase();
        logger.info("Connect: {}", name);

        Session existingSession = sessionMap.get(name);

        if (existingSession != null) {
            try {
                existingSession.close();
            } catch (Exception e) {
                logger.error("Error disconnecting session", e);
            }
        }
        sessionMap.put(name, session);
	}

    /**
     * Broadcasts a JSON message from one user to all connected users.
     *
     * @param name The API name of the sender.
     * @param json The JSON message to broadcast.
     */
    public static void broadcastMessageJson(String name, Json json) {
        logger.trace("Broadcast message: {}{}", name, json);

        Session session = sessionMap.get(name);
        if (session == null) {
            delayForReconnect();
            session = sessionMap.get(name);
            if (session == null) {
                logger.error("Invalid API name: {}. Client not connected", name);
                return;
			}
		}

        json.set("api", name);
        String message = json.toString();

        sendMessage(session, message);
    }

    /**
     * Generates a unique UUID for each message.
     *
     * @return A unique UUID as a long value.
     */
	public static long getUuid() {
        uuidCounter++;
        if (uuidCounter > Long.MAX_VALUE - 10) uuidCounter = 0;
        return uuidCounter;
	}

    /**
     * Adds an API to the session map, associating it with a user session.
     *
     * @param apiName The API name to add.
     * @param user    The session associated with the API name.
     */
	public static void addApi(String apiName, Session user) {
		addUser(apiName, user);
	}

    /**
     * Removes a user session from the session map.
     *
     * @param user The session to be removed.
     */
	public static void removeUser(Session user) {
        List<String> keysToRemove = new ArrayList<>();
        for (Map.Entry<String, Session> entry : sessionMap.entrySet()) {
            if (user.equals(entry.getValue())) {
                keysToRemove.add(entry.getKey());
                logger.info("Remove session: {}", entry.getKey());
			}
        }

        for (String key : keysToRemove) {
            sessionMap.remove(key);
        }
    }

    /**
     * Executes a Jcmd command, sending the command to the appropriate API and waiting for the response.
     *
     * @param apiName The API name to send the command to.
     * @param jsonCmd    The JSON command to execute.
     * @return The JSON response from the API.
     */
	public static Json execJCmd(String apiName, Json jsonCmd, String xCorrelationId) {
        Json fullMessage = Json.object();
        String uuid = String.valueOf(getUuid());

        fullMessage.set(ID, uuid);
        fullMessage.set(X_CORRELATION_ID, xCorrelationId);
        fullMessage.set("api", apiName);
        fullMessage.set("jsonCommand", jsonCmd);

        debugOutboundRequest(logger, LoggerInterfaces.CSL_CLIENT.toString(), 0, "", apiName, "WS", LoggerConstants.WS_REQUEST_SENT);

        CompletableFuture<Json> futureResponse = new CompletableFuture<Json>().completeOnTimeout(Json.object(RESPONSE, Json.object(ERROR, "TIMEOUT")), TIME_OUT, TimeUnit.MILLISECONDS);
        pendingMessages.put(uuid, futureResponse);
        broadcastMessageJson(apiName, fullMessage);

        Json response;
        try {
            response = futureResponse.get().get(RESPONSE);
        } catch (InterruptedException  | ExecutionException e) {
            response =Json.object(ERROR, "Interrupted : "+e.getMessage());
        }

        debugInboundResponse(logger, LoggerInterfaces.CSL_CLIENT.toString(), 0, "", apiName, "WS", 0, LoggerConstants.WS_RESPONSE_RECV);
        return (response!=null && response.has("result")) ? response.get("result") : Json.object().set(ERROR, "timeout");
    }

    /**
     * Processes an incoming WebSocket message, matching it to a pending command and storing the response.
     *
     * @param message The incoming WebSocket message as a string.
     */
    public static synchronized void messageArrived(String message) {
        try {
            Json jsonMessage = Json.read(message);
            String uuid = jsonMessage.get(ID).asString();

            if (!uuid.isEmpty()) {
                CompletableFuture<Json> pendingMessage = pendingMessages.get(uuid);
                pendingMessages.remove(uuid);
                if (pendingMessage != null) {
                    pendingMessage.complete(Json.object(RESPONSE, jsonMessage));
                }
            }
        } catch (Exception e) {
            logger.error("Error processing message", e);
        }
    }

    /**
     * Sends a message over the given session and handles success or failure.
     *
     * @param session The WebSocket session to send the message through.
     * @param message The message to send.
     */
    private static void sendMessage(Session session, String message) {
        try {
            session.getAsyncRemote().sendText(message);
        } catch (Exception e) {
            logger.error("Error sending message", e);
        }
    }

    /**
     * Delays the broadcast by 1 second to allow reconnection attempts.
     */
    private static void delayForReconnect() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            logger.error("Interrupted while waiting for reconnection", e);
        }
	}

    public static String printSessionMap() {
        return sessionMap.toString();
    }

    public static void clearSession() {
        sessionMap.clear();
    }

    public static void startKeepAlive() {
        connected.set(true);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            if (connected.get()) {
                for (Session session : new HashSet<>(sessionMap.values())) {
                    if (session.isOpen()) {
                        sendMessage(session, "keepalive");
                    }
                }
            }

        }, 0, 30, TimeUnit.SECONDS);
    }

    public static void stopKeepAlive() {
        connected.set(false);
    }
}
