package com.csl.web.websockets;

import org.eclipse.jetty.websocket.api.annotations.WebSocket;

@WebSocket
public class CSLWebSocketHandlerForConsole {

//    private String sender, msg;
//
//    @OnWebSocketConnect
//    public void onConnect(Session user) throws Exception {
//    	System.out.println("connect :"+user);
//    	System.out.println(user.getUpgradeRequest().getRequestURI());
//    	System.out.println(user.getUpgradeRequest().getRequestURI().getPath());
//    	CSLContext.instance.logInfo("Connection :"+user);
//        String username = "User" + CSLWebSocketForVariables.nextUserNumber++;
//        CSLWebSocketForConsole.userUsernameMap.put(user, username);
//        
//    }
//
//    @OnWebSocketClose
//    public void onClose(Session user, int statusCode, String reason) {
//        String username = CSLWebSocketForVariables.userUsernameMap.get(user);
//        CSLWebSocketForConsole.userUsernameMap.remove(user);
//       
//    }
//
//    @OnWebSocketMessage
//    public void onMessage(Session user, String message) {
//      
//    }

}
