package com.csl.web.websockets;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;

public class CSLCorrelatedWebSocketStompClient extends WebSocketStompClient {

    public CSLCorrelatedWebSocketStompClient (SockJsClient sockJsClient) {
        super(sockJsClient);
    }
}
