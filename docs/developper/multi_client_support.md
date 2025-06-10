# Multi-client Support in CSL_SERVER

This document explains how the server handles several CSL_CLIENT instances connected at the same time and how to enable this feature.

## WebSocket registration

`CSLWebSocketForJcmdHandler` exposes the `/cmd` WebSocket endpoint. Each client connects with its identifier and credentials provided as query parameters:

```
ws://server:port/cmd?id=<client-id>&password=<password>
```

During `@OnOpen`, the server validates these credentials using the `supported_clients.json` file and registers the client session if valid.

```java
@OnMessage
public void onMessage(String message) {
    message = message.trim();
    if (message.startsWith("api:")) {
        String apiName = message.substring(4);
        CSLWebSocketForJcmd.addApi(apiName, session);
    }
    // ...
}
```

The handler forwards the session to `CSLWebSocketForJcmd.addApi` which stores it in a concurrent map keyed by the API name.

## Managing sessions

`CSLWebSocketForJcmd` maintains the map of active sessions:

```java
static Map<String, Session> sessionMap = new ConcurrentHashMap<>();
```

Messages can be sent to a specific client using `broadcastMessageJson(apiName, json)` which looks up the session in this map and delivers the payload.

## Keep‑alive logic

To keep connections alive, the server periodically sends `keepalive` messages to every active session. The new implementation keeps a counter of connected clients so the keep‑alive thread runs as long as at least one client remains connected.

```java
private static final AtomicInteger connectionCount = new AtomicInteger(0);

public static void startKeepAlive() {
    connectionCount.incrementAndGet();
    connected.set(true);
}

public static void stopKeepAlive() {
    int remaining = connectionCount.decrementAndGet();
    if (remaining <= 0) {
        connectionCount.set(0);
        connected.set(false);
    }
}
```

Every five seconds `startKeepAliveThread` checks `connected` and broadcasts a `keepalive` message when needed.

## Usage

1. Each CSL_CLIENT provides its credentials via the WebSocket URL using `id` and `password` parameters.
2. The server stores the session for each validated client and continues sending keep‑alive packets while at least one client is connected.
3. API calls or notifications can be directed to a specific client with `broadcastMessageJson(clientId, json)` or similar methods.

This mechanism allows the server to host multiple clients simultaneously over WebSocket without interfering with each other.

### Configuration files

The server reads `supported_clients.json` from the classpath to determine the list of allowed clients and their credentials. Each client must have a local `clientConfig.env` file providing `CLIENT_ID` and `PASSWORD`. These values are appended to the WebSocket URL when connecting.
