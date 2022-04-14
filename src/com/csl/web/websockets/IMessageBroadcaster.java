package com.csl.web.websockets;

import com.xcsl.json.Json;

public interface IMessageBroadcaster {

	void broadcastMessageJson( String socketName, Json j);
	void broadcastMessageString( String socketName,  String s);
}
