package com.csl.ids;

import com.csl.web.websockets.CSLWebSocket;
import com.ucsl.interfaces.IConsole;
import com.ucsl.json.Json;

public class IDSConsole implements IConsole {
	
	private final IDSParams idsParams;


	public IDSConsole(IDSParams idsParams) {
		this.idsParams=idsParams;
	}

	@Override
	public void print(String outputName, String s) {
		println(outputName, s);
	}

	public void println(String target,String line) {
		if (idsParams.isSendToBrowser()) {
			Json j = Json.object();
			j.set("line", line);
			j.set("console_id",target);
			CSLWebSocket.broadcastMessageJson(CSLWebSocket.WEB_SOCKET_CONSOLE,j );
		}
		if (idsParams.isSendToConsole()) 
			System.out.println(line);

	}
}
