package com.csl.web.websockets;

import org.eclipse.jetty.websocket.api.annotations.WebSocket;

@WebSocket
public class CSLWebSocketHandlerForVariables {

//    private String sender, msg;
//
//    @OnWebSocketConnect
//    public void onConnect(Session user) throws Exception {
//    	CSLContext.instance.logInfo("Connection :"+user);
//        String username = "User" + CSLWebSocketForVariables.nextUserNumber++;
//        CSLWebSocketForVariables.userUsernameMap.put(user, username);
//        //CSLWebSocket.broadcastMessage(sender = "Server", msg = (username + " joined the chat"));
//    }
//
//    @OnWebSocketClose
//    public void onClose(Session user, int statusCode, String reason) {
//        String username = CSLWebSocketForVariables.userUsernameMap.get(user);
//        CSLWebSocketForVariables.userUsernameMap.remove(user);
//       // CSLWebSocket.broadcastMessage(sender = "Server", msg = (username + " left the chat"));
//    }
//
//    @OnWebSocketMessage
//    public void onMessage(Session user, String message) {
//       // Chat.broadcastMessage(sender = Chat.userUsernameMap.get(user), msg = message);
//    }

}
