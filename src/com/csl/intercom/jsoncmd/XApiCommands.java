package com.csl.intercom.jsoncmd;

import com.csl.intercom.broker.CSLInterModuleCommunicationManager;
import com.ucsl.json.Json;
import com.ucsl.json.JsonMoreUtil;
import com.ucsl.json.JsonUtil;

import spark.Request;
import spark.Response;
import spark.Route;

public class XApiCommands {

	static boolean debug =true;

	private String path = "";

	private CSLInterModuleCommunicationManager interModuleCommunicationManager;

	private String name;

	
	
	public XApiCommands(String name, CSLInterModuleCommunicationManager imcm ) {
		
		this.interModuleCommunicationManager=imcm;
		this.name=name;
	}
	
	public String getPathNameForPost() {
		String s=name;
		if (!s.startsWith("/")) s="/"+s;
		return s;
	}
	
	public Route getPostRoute() {
		return (req, res) -> execPostCommand(req, res);
	}

	public String getCleanApiName() {
		
		String s=path;
		if (s.endsWith("/"))
			s= s.substring(0,s.length() - 1);
		
		if (s.startsWith("/")) s=s.substring(1);
		
		return s;
	}
	
	public String getName() {
	
		return getCleanApiName();
	}
	
	private String execPostCommand(Request req, Response res) {

		String sresponse = req.body();

		String result = "";

		Json data = Json.read(sresponse);
		Json cmd = data.get("cmd");
		Json params = data.get("params");

		if (cmd == null) {
			System.out.println("Invalid jcmd:" + cmd);
		}
		if (params == null) {
			params = Json.object();
		}

		if (debug) System.out.println("Exec " + cmd + " " + params);
	
		Json r=interModuleCommunicationManager.executeExternalCommand(name, data ) ;
		
		
		System.out.println("Result from Xcall");
		System.out.println(JsonMoreUtil.json2str(r));
		return JsonUtil.prettyPrint(r);
	}
	
}
