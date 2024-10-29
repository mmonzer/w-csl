package com.csl.web.websockets;

import com.ucsl.json.Json;
import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CSLWebSocketForVariables {
	
	static String socketName="VARIABLES";

	public static void  refresh() {

		userUsernameMap.keySet().stream().forEach(session -> {
			String name=userUsernameMap.get(session);
			Json jx=Json.object();
			jx.set("refresh", socketName);

			try {
				if (session.isOpen())session.getRemote().sendString(jx.toString());
			} catch (IOException e) {
				
				// e.printStackTrace();
			}
		});
	}

    // this map is shared between sessions and threads, so it needs to be thread-safe (http://stackoverflow.com/a/2688817)
    static Map<Session, String> userUsernameMap = new ConcurrentHashMap<>();
}
