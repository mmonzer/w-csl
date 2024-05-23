package com.csl.web.auth;

import com.csl.intercom.jsoncmd.JServiceLoader;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ServerConfig {


    int port=8000;
	
	//ajouter lecture depuis un fichier .json
	
	//à mttre ds JsonUtilMore

    String rootdir="./public";
    String rootdir2="";
	String rootdir3="";
    private String userDir;
    private boolean running;
    private boolean verbose;

    private boolean core_commands;
    private boolean vars_commands;
	private boolean modbus_commands;
    private boolean database_commands;
    private boolean jcmd_commands;
	private boolean mx_commands;
    private boolean send_alerts;
    private boolean send_console_output;
	
	
	public ServerConfig() {
		// TODO Auto-generated constructor stub
	}
	
	public ServerConfig(Json j) {
		
		userDir = JServiceLoader.getUserDir();

		running=JsonUtil.getBooleanFromJson(j,"on", false);
		if (!running) return;

		
		verbose=JsonUtil.getBooleanFromJson(j,"verbose", false);
        boolean debug = JsonUtil.getBooleanFromJson(j, "debug", false);

		port = JsonUtil.getIntFromJson(j, "webserver_port",8000);

		rootdir=JsonUtil.getStringFromJson(j, "web_rootdir","");
		rootdir=replaceUserDir(rootdir, userDir);
	
		rootdir2=JsonUtil.getStringFromJson(j, "web_rootdir2","");
		rootdir2=replaceUserDir(rootdir2, userDir);
	
		rootdir3=JsonUtil.getStringFromJson(j, "web_rootdir3","");
		rootdir3=replaceUserDir(rootdir3, userDir);
	

	
		core_commands= JsonUtil.getBooleanFromJson(j,"core_commands", false);
		vars_commands= JsonUtil.getBooleanFromJson(j,"vars_commands", false);

		modbus_commands= JsonUtil.getBooleanFromJson(j,"modbus_commands", false);
		database_commands = JsonUtil.getBooleanFromJson(j,"database_commands", false);
		jcmd_commands = JsonUtil.getBooleanFromJson(j,"jcmd_commands", true);

		mx_commands = JsonUtil.getBooleanFromJson(j,"mx_commands", false);

		send_alerts= JsonUtil.getBooleanFromJson(j,"send_alerts", false);
		send_console_output= JsonUtil.getBooleanFromJson(j,"send_console_output", false);

			System.out.println("\nCSL Server:");
			System.out.println("===========");
			System.out.println("  on  :"+running);
			System.out.println("  port:"+port);

			System.out.println("  rootdir:"+rootdir);
			System.out.println("  rootdir2:"+rootdir2);
			System.out.println("  rootdir3:"+rootdir3);

			System.out.println("  core_commands   :"+core_commands);
			System.out.println("  vars_commands   :"+vars_commands);

			System.out.println("  modbus_commands :"+modbus_commands);
			
			System.out.println("  database_commands:"+database_commands);

			System.out.println("  send_alerts:"+send_alerts);
			System.out.println("  send_console_outputs:"+send_console_output);

	}
	
	private String replaceUserDir(String dir,String userDir) {

		if (dir.startsWith(".")) {
			return userDir+dir.substring(1);
		}
		return dir;
	}

    public boolean isDebug() {
		return true;
	}

    public boolean isMx_commands() {
		return mx_commands;
	}

	
}
