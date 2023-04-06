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

    private StompSession subscribeToNotifications() throws ExecutionException, InterruptedException {
        WebSocketClient webSocketClient = new StandardWebSocketClient();
        SockJsClient sockJsClient = new SockJsClient(new ArrayList<>(Arrays.asList(new WebSocketTransport(webSocketClient))));

        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new JsonMessageConverter());
        stompClient.setTaskScheduler(new DefaultManagedTaskScheduler());
        return stompClient.connect(this.scanManagerDiscoveryUrl, new StompSessionHandlerAdapter() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return super.getPayloadType(headers);
            }

            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                String pl = new String(payload);
                exception.printStackTrace(System.err);
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                super.handleFrame(headers, payload);
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

    private StompSession connectToRequestsWebSocket() throws ExecutionException, InterruptedException {
        WebSocketClient requestClient = new StandardWebSocketClient();
        SockJsClient sockJsRequestClient = new SockJsClient(new ArrayList<>(Arrays.asList(new WebSocketTransport(requestClient))));

        WebSocketStompClient requestStompClient = new WebSocketStompClient(sockJsRequestClient);
        requestStompClient.setMessageConverter(new MappingJackson2MessageConverter());
        requestStompClient.setTaskScheduler(new DefaultManagedTaskScheduler());

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
     * Actually start the scan, without checking the validity of the session.
     * @param entities A list of entities to scan. May be null, resulting in a scan of all entities.
     */
    private void startScan(List<String> entities) {
        if (entities == null || entities.isEmpty()) {
            stompRequestsSession.send(websocketStartDiscoveryEndpoint, "");
        } else {
            String[] entitiesArray = entities.toArray(new String[0]);
            stompRequestsSession.send(websocketStartDiscoveryEndpoint, entitiesArray);
        }
    }

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

    private static class JsonMessageConverter extends AbstractMessageConverter {
        public JsonMessageConverter() {
            super(new MimeType("application", "json"));
        }

        @Override
        protected boolean supports(Class<?> clazz) {
            return String.class.isAssignableFrom(clazz) || (clazz.isArray() && String.class.isAssignableFrom(clazz.getComponentType()));
        }

        @Override
        protected String convertToInternal(Object payload, MessageHeaders headers, Object conversionHint) {
            if (payload instanceof String) {
                return (String) payload;
            } else if (payload instanceof byte[]) {
                return new String((byte[]) payload);
            } else if (payload instanceof String[]) {
//                return "[" + String.join("\",\"", (String[]) payload) + "]";
                Json result = Json.array();
                for (String x: (String[]) payload) {
                    result.add(x);
                }
                return result.toString();
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
