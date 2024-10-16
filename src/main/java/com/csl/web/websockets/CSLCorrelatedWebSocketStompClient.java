package com.csl.web.websockets;

import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;

public class CSLCorrelatedWebSocketStompClient extends WebSocketStompClient {

    public CSLCorrelatedWebSocketStompClient (SockJsClient sockJsClient) {
        super(sockJsClient);
    }
}
