
package com.csl.web.htmlcommands;



import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.csl.intercom.jsoncmd.JCmdManager;
import com.csl.web.CSLHttpServer;
import com.xcsl.json.Json;

import spark.Request;
import spark.Response;

public class ExecJsonCommands {

	

	
	 boolean initialized=false;
	 JCmdManager jCmdManager=null;
	
	private CSLHttpServer cslHttpServer;

	public ExecJsonCommands(CSLHttpServer cslHttpServer) {
		
		this.cslHttpServer=cslHttpServer;
		
	}
	
	
	public  void init(JCmdManager jCmdManager2) {
		if (initialized) return;
		
		jCmdManager=jCmdManager2;
					
		
		cslHttpServer.addPostRoute("/exec_jsoncmd", (req, res) -> exec_jsoncmd(req,res));
		cslHttpServer.addGetRoute("/exec_jsoncmd", (req, res)  -> renderGetCommand(req,res));
		
		initialized=true;
	}
	
	
	
	public  boolean isInitialized() {
		return initialized;
	}



	private  String exec_jsoncmd(Request req, Response res) {




		//System.out.println("POST : test");
		String sresponse = req.body();
		System.out.println("\n"+sresponse);
		System.out.println("path:"+req.pathInfo()); 

		String result="";

		//if (s.compareToIgnoreCase("setfile")==0) 

		Json data = Json.read(sresponse);
		Json cmd=data.get("cmd");
		Json params=data.get("params");

		if (cmd==null) {
			System.out.println("Invalid jcmd:"+cmd);
		}
		if (params==null) {
			params=Json.object();
		}

		System.out.println("Exec "+cmd+" "+params);

		String cresult=jCmdManager.exec(cmd.asString(), params).toString();


		return cresult;
	}
	
	private   String renderGetCommand(Request req, Response res) {

		Set<String> paramKeys= req.queryParams();


		//String sresponse = req.body();
		
		String s=req.pathInfo();
		if (s.length()>1) s=s.substring(1);
	
		String cmd="???";
		
		List<String> varNames= new ArrayList<String>(paramKeys);
	
		Json params=Json.object();
		for (String name:varNames) {
			String value = req.queryParams(name);
			if (name.compareTo("cmd")==0) {
				cmd=value;
			}
			else {
				params.set(name, value);
					
			}
		}
		
		
		System.out.println("Exec "+cmd+" "+params);

		String cresult=jCmdManager.exec(cmd, params).toString();
		
		return cresult;
	}
}
