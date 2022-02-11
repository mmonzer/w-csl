package com.csl.web.websockets;

public class CSLWebSocketForConsole {
	
	

	

//    // this map is shared between sessions and threads, so it needs to be thread-safe (http://stackoverflow.com/a/2688817)
//    static Map<Session, String> userUsernameMap = new ConcurrentHashMap<>();
//    static int nextUserNumber = 1; //Assign to username for next connecting user
//
//    
//
//    static String socketName="CONSOLE";
//
//    public static void  refresh() {
//
//		userUsernameMap.keySet().stream().forEach(session -> {
//			String name=userUsernameMap.get(session);
//			//System.out.println("Refresh socket "+socketName+": "+name+" open:"+session.isOpen());
//			Json jx=Json.object();
//			jx.set("refresh", socketName);
//
//			try {
//				if (session.isOpen())session.getRemote().sendString(jx.toString());
//				System.out.println(" send_refresh "+jx);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		});
//	}
//    
//    //Sends a message from one user to all users, along with a list of current usernames
//    public static void broadcastMessageJson( String tag, Json j) {
//    	
//    	//System.out.println("Console out:"+j);
//    	
//    	Json jx=Json.object();
//    	jx.set("loginfo", j);
//    	String s=jx.toString();
//    	
//    	CSLInterModuleCommunicationManager.instance.sendSocketMsg(socketName, jx);
//    	
//    	
//        userUsernameMap.keySet().stream().filter(Session::isOpen).forEach(session -> {
//            try {
//            	
//            	//String s=String.valueOf(new JSONObject()
//                  //      .put(tag, j.toString()));
//                        //.put("userMessage","testtest"));
//            	
//            	
//            	//IDSTrace.log(IDSTrace.WEB_SOCKET, "SEND JSON "+s);
//            	//System.out.println("SEND JSON "+s);
//                session.getRemote().sendString(s);
//                    //.put("userlist", userUsernameMap.values())
//               // ));
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
//    }
//    
//    
//  //Sends a message from one user to all users, along with a list of current usernames
//    public static void broadcastMessageString( String tag, String s) {
//    	
//    	//System.out.println("Console out:"+j);
//    	
//        userUsernameMap.keySet().stream().filter(Session::isOpen).forEach(session -> {
//            try {
//            	
//            	IDSTrace.log(IDSTrace.WEB_SOCKET, "SEND String "+s);
//            	System.out.println("SEND String "+s);
//                session.getRemote().sendString(s);
//                    //.put("userlist", userUsernameMap.values())
//               // ));
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
//    }
}
