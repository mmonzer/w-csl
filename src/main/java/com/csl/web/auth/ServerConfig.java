package com.csl.web.auth;

import com.csl.core.Config;
import com.csl.intercom.jsoncmd.JServiceLoader;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ServerConfig {
    int port=8000;

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

	public ServerConfig(Config.Server config) {
		userDir = JServiceLoader.getUserDir();

		running=config.getOn();
		if (!running) return;

		verbose=config.getVerbose();

		port = config.getWebserverPort();

		rootdir=config.getWebRootdir();
		rootdir=replaceUserDir(rootdir, userDir);

		rootdir2=config.getWebRootdir2();
		rootdir2=replaceUserDir(rootdir2, userDir);

		rootdir3=config.getWebRootdir3();
		rootdir3=replaceUserDir(rootdir3, userDir);

		core_commands= config.getCoreCommands();
		vars_commands= config.getVarsCommands();

		modbus_commands= config.getModbusCommands();
		database_commands = config.getDatabaseCommands();
		jcmd_commands = config.getJcmdCommands();

		mx_commands = config.getMxCommands();

		send_alerts= config.getSendAlerts();
		send_console_output= config.getSendConsoleOutput();

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

	
}
