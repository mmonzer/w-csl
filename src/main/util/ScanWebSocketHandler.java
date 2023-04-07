package main.util;

import com.ucsl.json.Json;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.scheduling.concurrent.DefaultManagedTaskScheduler;
import org.springframework.util.MimeType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * Handle the WebSocket connections with CSL-Scan.
 */
public class ScanWebSocketHandler {
    private final String websocketNotificationsEndpoint = "/discovery/ready";
    private final String websocketStartDiscoveryEndpoint = "/discovery/start";
    private String scanManagerDiscoveryUrl;
    private ScheduledExecutorService webSocketsConnectionAttempts;
    private StompSession stompRequestsSession = null;
    private StompSession stompNotificationSession = null;
    private final Queue<List<String>> scanRequestsQueue = new ConcurrentLinkedQueue<>();
    private StompFrameHandler stompNotificationHandler = new StompSessionHandlerAdapter() {
        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            System.out.println("[STOMP]" + payload.toString());
        }
    };

    /**
     * Create a new manager with the correct URL.
     * @param scanManagerDiscoveryUrl The URL of CSL-Scan.
     */
    public ScanWebSocketHandler(String scanManagerDiscoveryUrl) {
        this.scanManagerDiscoveryUrl = scanManagerDiscoveryUrl;

        // Schedule reconnection to websockets every 2 seconds
        webSocketsConnectionAttempts = Executors.newScheduledThreadPool(1);
        webSocketsConnectionAttempts.scheduleAtFixedRate(
                this::connectStompSessionsIfNecessary,
                0,
                2,
                TimeUnit.SECONDS);
    }

    /**
     * Stop the connections with CSL-Scan
     */
    public void stop() {
        webSocketsConnectionAttempts.shutdown();
        stompRequestsSession.disconnect();
        stompNotificationSession.disconnect();
    }

    /**
     * Get the status of the websocket handling :
     * - the
     * @return
     */
    public Json getStatus() {
        Json status = Json.object();

        status.set("requestsWebSocket", (stompRequestsSession != null && stompRequestsSession.isConnected()) ? "OK" : "NOK");
        status.set("notificationsWebSocket", (stompNotificationSession != null && stompNotificationSession.isConnected()) ? "OK" : "NOK");
        status.set("scanRequestsQueue", scanRequestsQueue.size());

        return status;
    }

    /**
     * Start scanning all registered entities
     * @return An id identifying the scan for further notice.
     */
    public Json requestScan(List<String> entities) {
        if (stompRequestsSession == null || !stompRequestsSession.isConnected()) {
            scanRequestsQueue.add(entities);
            return Json.object("result", "NOK",
                    "details", "Scan service unavailable, added scan request to queue");
        } else {
            startScan(entities);
            return Json.object("result", "OK",
                    "details", "Scan started successfully");
        }
    }

    /**
     * Actually start the scan, without checking the validity of the session.
     * @param entities A list of entities to scan. May be null, resulting in a scan of all entities.
     */
    private void startScan(List<String> entities) {
        if (entities == null || entities.isEmpty()) {
            stompRequestsSession.send(websocketStartDiscoveryEndpoint, "");
        } else {
            stompRequestsSession.send(websocketStartDiscoveryEndpoint, Json.array(entities.toArray()));
        }
    }

    /**
     * Check if the websockets are connected, and tries to connect if not.
     * Blocking, should be called asynchronously.
     */
    private void connectStompSessionsIfNecessary() {
        if (stompNotificationSession == null || !stompNotificationSession.isConnected())  {
            try {
                stompNotificationSession = subscribeToNotifications();
            } catch (InterruptedException | ExecutionException | ResourceAccessException e) {
                stompNotificationSession = null;
            } catch (Throwable e) {
                System.out.println("Caught");
                stompNotificationSession = null;
            }
        }

        if (stompRequestsSession == null || !stompRequestsSession.isConnected()) {
            try {
                stompRequestsSession = connectToRequestsWebSocket();
            } catch (InterruptedException | ExecutionException  e) {
                stompRequestsSession = null;
            }
        }
    }

    /**
     * Connect the notification socket to CSL-Scan, and define the necessary callbacks (after connection and on message reception).
     * Blocks so should be called asynchronously.
     * @return The session we just created.
     * @throws ExecutionException if connection failed.
     * @throws InterruptedException if connection was interrupted.
     */
    private StompSession subscribeToNotifications() throws ExecutionException, InterruptedException {
        WebSocketStompClient stompClient = createStompClient();

        // Define the callbacks and return the future when it is realized.
        return stompClient.connect(this.scanManagerDiscoveryUrl, new StompSessionHandlerAdapter() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return super.getPayloadType(headers);
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payloadRaw) {
                super.handleFrame(headers, payloadRaw);
                Json payload = (Json) payloadRaw;

                if (payload != null) {
                    System.out.println("[STOMP] " + payload.toString());
                } else {
                    System.out.println("[STOMP] null");
                }
            }

            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                super.afterConnected(session, connectedHeaders);
                session.subscribe(websocketNotificationsEndpoint, this);
            }
        }).get();
    }

    /**
     * Connect the requests socket to CSL-Scan, and define the necessary callbacks (after connection and on message reception).
     * Blocks so should be called asynchronously.
     * @return The session we just created.
     * @throws ExecutionException if connection failed.
     * @throws InterruptedException if connection was interrupted.
     */
    private StompSession connectToRequestsWebSocket() throws ExecutionException, InterruptedException {

        WebSocketStompClient requestStompClient = createStompClient();
        // Define the callbacks and return the future when it is realized.
        return requestStompClient.connect(this.scanManagerDiscoveryUrl, new StompSessionHandlerAdapter() {
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                super.handleFrame(headers, payload);
                if (payload != null) {
                    System.out.println("[STOMP] " + payload.toString());
                    // handle response
                } else {
                    System.out.println("[STOMP] null");
                }
            }

            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                super.afterConnected(session, connectedHeaders);
                purgeScanRequestsQueue();
            }
        }).get();
    }

    /**
     * Create the STOMP client with our custom message interpreter.
     * @return a {@link WebSocketStompClient} suitable for our needs.
     */
    private WebSocketStompClient createStompClient() {
        WebSocketClient client = new StandardWebSocketClient();
        SockJsClient sockJsClient = new SockJsClient(new ArrayList<>(Arrays.asList(new WebSocketTransport(client))));

        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new JsonMessageConverter());
        stompClient.setTaskScheduler(new DefaultManagedTaskScheduler());

        return  stompClient;
    }

    /**
     * Try to execute all the scan requests in the queue.
     */
    private void purgeScanRequestsQueue() {
        List<String> scanRequest;
        while ((scanRequest = scanRequestsQueue.poll()) != null) {
            if (stompRequestsSession == null || !stompRequestsSession.isConnected()) {
                scanRequestsQueue.add(scanRequest);
                break;
            }
            startScan(scanRequest);
            if (stompRequestsSession == null || !stompRequestsSession.isConnected()) {
                scanRequestsQueue.add(scanRequest);
                break;
            }
        }
    }

    /**
     * Custom message converter to serialize and deserialize {@link Json} objects.
     */
    private static class JsonMessageConverter extends AbstractMessageConverter {
        public JsonMessageConverter() {
            super(new MimeType("application", "json"));
        }

        @Override
        protected boolean supports(Class<?> clazz) {
            return String.class.isAssignableFrom(clazz)
                    || clazz.isArray() && String.class.isAssignableFrom(clazz.getComponentType())
                    || Json.class.isAssignableFrom(clazz)
                    ;
        }

        @Override
        protected Object convertToInternal(Object payload, MessageHeaders headers, Object conversionHint) {
            if (payload instanceof Json) {
                return ((Json) payload).toString().getBytes();
            } else if (payload instanceof String) {
                return ((String) payload).getBytes();
            } else if (payload instanceof byte[]) {
                return payload;
            } else if (payload instanceof String[]) {
                Json result = Json.array();
                for (String x: (String[]) payload) {
                    result.add(x);
                }
                return result.toString().getBytes();
            } else {
                throw new RuntimeException("Bad type");
            }
        }

        @Override
        protected Json convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
            Object payload = message.getPayload();
            if (payload instanceof String) {
                return Json.read((String) payload);
            } else if (payload instanceof byte[]) {
                return Json.read(new String((byte[]) payload));
            } else {
                throw new RuntimeException("Bad conversion");
            }
        }
    }
}
