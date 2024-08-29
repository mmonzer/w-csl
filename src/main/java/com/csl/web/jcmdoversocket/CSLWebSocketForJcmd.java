package com.csl.web.jcmdoversocket;

import com.ucsl.json.Json;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * CSLWebSocketForJcmd handles WebSocket communication for Jcmd commands.
 * It manages sessions, sends messages, and processes responses with timeout management.
 */
public class CSLWebSocketForJcmd {

    private static final Logger logger = LoggerFactory.getLogger(CSLWebSocketForJcmd.class);
    private static final String RESPONSE = "response";
    public static final String ID = "uuid";
    public static final String X_CORRELATION_ID = "X-Correlation-ID";
    public static final String ENDPOINT = "endpoint";
    public static final String COMMAND = "command";
    public static long TIME_OUT = 60000;

    public static String WEB_SOCKET_CMD = "/cmd";

    public static long uuidCounter = 0;

    // Concurrent map to manage WebSocket sessions by API name
    static Map<String,Session> sessionMap = new ConcurrentHashMap<>();

    // Map to keep track of pending messages and their status
    static Map<String, Json> pendingMessages = new HashMap<>();

    // Debug level for logging
    static private int debugLevel = 2;

    // Flag to indicate whether the timeout detector is running
    static boolean timeOutDetectorRunning = false;

    /**
     * Checks if the debug level is high enough to enable debug logging.
     *
     * @return true if debug level is greater than 1.
     */
    public static boolean isDebug() {
        return debugLevel > 1;
    }

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
                existingSession.disconnect();
			} catch (Exception e) {
                logger.error("Error disconnecting session", e);
			}
            existingSession.close();
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
        logger.debug("Broadcast message: {}{}", name, json);

        Session session = sessionMap.get(name);
        if (session == null) {
            delayForReconnect(name);
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
     * @param jCmd    The JSON command to execute.
     * @return The JSON response from the API.
     */
	public static Json execJCmd(String apiName, Json jCmd, String xCorrelationId) {
		startTimeOutDetector();

        Json fullMessage = Json.object();
        String uuid = String.valueOf(getUuid());

        fullMessage.set(ID, uuid);
        fullMessage.set(X_CORRELATION_ID, xCorrelationId);
        fullMessage.set("api", apiName);
        fullMessage.set("jcmd", jCmd);

        broadcastMessageJson(apiName, fullMessage);
        fullMessage.set("start_time", System.currentTimeMillis());

        pendingMessages.put(uuid, fullMessage);

        return waitForResponse(uuid, fullMessage);
    }


    /**
     * Processes an incoming WebSocket message, matching it to a pending command and storing the response.
     *
     * @param message The incoming WebSocket message as a string.
     */
    public static void messageArrived(String message) {
        logger.debug("Message received: {}", message);

        try {
            Json jsonMessage = Json.read(message);
            String uuid = jsonMessage.get("uuid").asString();

            if (!uuid.isEmpty()) {
                Json pendingMessage = pendingMessages.get(uuid);
                if (pendingMessage != null) {
                    pendingMessage.set(RESPONSE, jsonMessage);
                }
            }
        } catch (Exception e) {
            logger.error("Error processing message", e);
        }
    }

    /**
     * Starts the timeout detector to monitor pending messages and mark them as timed out if they exceed the timeout limit.
     */
    static void startTimeOutDetector() {
        if (timeOutDetectorRunning) return;
        timeOutDetectorRunning = true;

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();

            for (Map.Entry<String, Json> entry : pendingMessages.entrySet()) {
                Json message = entry.getValue();
                long startTime = message.get("start_time").asLong();
                long endTime = startTime + TIME_OUT;

                if (endTime < currentTime) {
                    logger.debug("Timeout: {}", message);
                    message.set(RESPONSE, Json.object().set("error", "TIMEOUT"));
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Waits for a response to the given command until it arrives or times out.
     *
     * @param uuid        The UUID of the command.
     * @param fullMessage The full message containing the command.
     * @return The response JSON object, or a timeout error if the response is not received in time.
     */
    private static Json waitForResponse(String uuid, Json fullMessage) {
        while (true) {
			try {
				Thread.sleep(3);
			} catch (InterruptedException e) {
                logger.error("Interrupted while waiting for response", e);
				return Json.object().set("error", "timeout");
			}

            if (fullMessage.has(RESPONSE)) {
                logger.debug("Received response: {}", fullMessage);
                pendingMessages.remove(uuid);
                Json response = fullMessage.get(RESPONSE);
                return response.has("result") ? response.get("result") : Json.object().set("error", "timeout");
            }
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
            session.getRemote().sendString(message, new WriteCallback() {
                @Override
                public void writeFailed(Throwable x) {
                    logger.error("Failed to send message", x);
                }

                @Override
                public void writeSuccess() {
                    logger.debug("Message sent successfully");
                }
            });
        } catch (Exception e) {
            logger.error("Error sending message", e);
        }
    }

    /**
     * Delays the broadcast by 1 second to allow reconnection attempts.
     *
     * @param name The API name to wait for.
     */
    private static void delayForReconnect(String name) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            logger.error("Interrupted while waiting for reconnection", e);
        }
	}
}
