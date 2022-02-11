package com.csl.intercom.jsoncmd;

import java.util.HashMap;

import com.csl.logger.FileLog;
import com.xcsl.json.Json;
import com.xcsl.json.JsonUtil;

public class JCmdManager {
	
	

	HashMap<String, JsonCmd> listOfCommands = new HashMap<String, JsonCmd>();
	private Boolean logJsonCommandsToFile;
	private String logdir;
	private String filename;
	private long max_size;
	private FileLog fileLog;

	public Json exec(String name, Json params) {

		JsonCmd j = listOfCommands.get(name);

		if (j == null) {
			Json jresult = Json.object();
			jresult.set("error", "Command <" + name + "> not found");
			return jresult;
		}

		System.out.println("Exec " + name + "  params:" + params);
		if (logJsonCommandsToFile) {
			Json jj=Json.object();
			jj.set("exec", name);
			jj.set("params", params);
			
			fileLog.RecordLogMessage(jj.toString());
		}
		Json result= j.exec(params);
		if (result==null) {result=Json.object(); result.set("result","null");}
		if (logJsonCommandsToFile) {
			Json jj=Json.object();
			jj.set("result", result);
			
			fileLog.RecordLogMessage(jj.toString());
		}
		
		return result;
	}

	public String registerCmd(String name, JsonCmd j) {
		if (listOfCommands.get(name) != null)
			return "Command with this already registered";
		if (j == null)
			return "invalid command (null)";
		listOfCommands.put(name, j);
		return "ok";
	}

	/*
	 * "logJsonCommandsToFile":true, "log_dir":"./idslogs/jcmds",
	 * "log_prefix_filename":"JCMD", "max_size_of_log_files":10000
	 */

	public void init(Json jConfig) {
		
		
		listOfCommands.clear();

		this.logJsonCommandsToFile = JsonUtil.getBooleanFromJson(jConfig, "logJsonCommandsToFile", false);
		if (logJsonCommandsToFile) {

			String f=JsonUtil.getStringFromJson(jConfig, "log_dir", "./idslogs/jcmds");
			this.logdir = JServiceLoader.buildFullPathInUserDir(f);
			this.filename = JsonUtil.getStringFromJson(jConfig, "log_prefix_filename", "JCMD");
			this.max_size = JsonUtil.getLongFromJson(jConfig, "max_size_of_log_files", 100000);
			this.fileLog = new FileLog(logdir, filename, max_size, JServiceLoader::getSystemCurrentTimeMillis);

		}
	}

}
