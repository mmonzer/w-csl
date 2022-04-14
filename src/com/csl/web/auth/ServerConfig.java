package com.csl.web.auth;

import com.csl.intercom.jsoncmd.JServiceLoader;
import com.xcsl.json.Json;
import com.xcsl.json.JsonUtil;

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
	private boolean debug;

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
		debug=JsonUtil.getBooleanFromJson(j,"debug", false);

		port = JsonUtil.getIntFromJson(j, "webserver_port",8000);

		rootdir=JsonUtil.getStringFromJson(j, "web_rootdir","");
		rootdir=replaceUserDir(rootdir, userDir);
	
		rootdir2=JsonUtil.getStringFromJson(j, "web_rootdir2","");
		rootdir2=replaceUserDir(rootdir2, userDir);
	
		rootdir3=JsonUtil.getStringFromJson(j, "web_rootdir3","");
		rootdir3=replaceUserDir(rootdir3, userDir);
	

	
		core_commands= JsonUtil.getBooleanFromJson(j,"core_commands", false);
		//vars_commands (+ notifications of update)
		vars_commands= JsonUtil.getBooleanFromJson(j,"vars_commands", false);

		modbus_commands= JsonUtil.getBooleanFromJson(j,"modbus_commands", false);
		//external_commands = JsonUtil.getBooleanFromJson(j,"external_commands", false);

		//custom_commands = JsonUtil.getBooleanFromJson(j,"custom_commands", false);

		//json_commands = JsonUtil.getBooleanFromJson(j,"json_commands", false);
		database_commands = JsonUtil.getBooleanFromJson(j,"database_commands", false);
		jcmd_commands = JsonUtil.getBooleanFromJson(j,"jcmd_commands", true);

		//config_file_commands = JsonUtil.getBooleanFromJson(j,"config_file_commands", false);
		mx_commands = JsonUtil.getBooleanFromJson(j,"mx_commands", false);

		send_alerts= JsonUtil.getBooleanFromJson(j,"send_alerts", false);
		send_console_output= JsonUtil.getBooleanFromJson(j,"send_console_output", false);

		
		//if (verbose) 
		{
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
	}
	
	private String replaceUserDir(String dir,String userDir) {

		if (dir.startsWith(".")) {
			return userDir+dir.substring(1);
		}
		return dir;
	}

	
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public String getRootdir() {
		return rootdir;
	}
	public void setRootdir(String rootdir) {
		this.rootdir = rootdir;
	}
	public String getRootdir2() {
		return rootdir2;
	}
	public void setRootdir2(String rootdir2) {
		this.rootdir2 = rootdir2;
	}
	public String getRootdir3() {
		return rootdir3;
	}
	public void setRootdir3(String rootdir3) {
		this.rootdir3 = rootdir3;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public boolean isDebug() {
		return true;
		//return debug;
	}

	public boolean isCore_commands() {
		return core_commands;
	}

	public boolean isVars_commands() {
		return vars_commands;
	}

	public boolean isModbus_commands() {
		return modbus_commands;
	}

	public boolean isDatabase_commands() {
		return database_commands;
	}
	
	public boolean isJcmd_commands() {
		return jcmd_commands;
	}

	public boolean isSend_alerts() {
		return send_alerts;
	}

	public boolean isSend_console_output() {
		return send_console_output;
	}

	public String getUserDir() {
		return userDir;
	}

	public boolean isRunning() {
		return running;
	}

	public boolean isMx_commands() {
		return mx_commands;
	}
	
	
	
	
	
	
	
	
}
