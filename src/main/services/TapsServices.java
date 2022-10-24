package main.services;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;

import com.csl.core.CSLContext;
import com.csl.intercom.jsoncmd.ApiCommands;
import com.csl.intercom.jsoncmd.ApiCommandsFactory;
import com.csl.intercom.jsoncmd.JsonCmdHelp;
import com.jcraft.jsch.JSchException;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.interfaces.ICSLService;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.interfaces.IJsonCmdHelp;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;

import ch.qos.logback.core.recovery.ResilientSyslogOutputStream;
import main.extensions.SshUtils;

public class TapsServices implements ICSLService {
	//ApiCommands apiCommands= new ApiCommands("");
	




	private static final String START_TAPS = "cd ~/csl/scripts && sh launchTap.sh & exit";
	private static final String STOP_TAPS = "cd ~/csl/scripts && sh killTaps.sh";

	private static final String REPLAY = "cd ~/csl/scripts && sh replay.sh ";

	
	private static final String STOP_SURICATA = "cd ~/csl/scripts && sh killSuricata.sh";
	private static final String START_SURICATA = "cd ~/csl/scripts && sh launchSuricata.sh";
	private static final String CLEAR_SURICATA_LOG = "sudo rm /var/log/suricata/suricata.log";

	
	IApiCommands apiCommands= new ApiCommandsFactory().createApiCommands("");
	
	String name="taps";
	String configFileSectionName="ssh_service";	
	static ArrayList<Json> configuredTaps;  
	static String localIP;	
	static String localPort;
	static String knownHostFilePath;

	static String idsconf;
	
	private static String readFile(String fileName) throws IOException {
		String jsonRaw = "";
		File fichierRegles = new File(fileName);
	    InputStream lecteur = new BufferedInputStream(new FileInputStream(fichierRegles));
	    InputStreamReader ipsr =new InputStreamReader(lecteur, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(ipsr);
        String ligne;
        while ((ligne=br.readLine())!=null){
           jsonRaw+=ligne+"\r\n";
        }
        br.close(); 
        return jsonRaw;
	}
	
	private static Json readJsonFile(String fileName) throws IOException {
		String jsonRaw = "";
		File fichierRegles = new File(fileName);
	    InputStream lecteur = new BufferedInputStream(new FileInputStream(fichierRegles));
	    InputStreamReader ipsr =new InputStreamReader(lecteur);
        BufferedReader br = new BufferedReader(ipsr);
        String ligne;
        while ((ligne=br.readLine())!=null){
           jsonRaw+=ligne+"\n";
        }
        br.close(); 
        return Json.read(jsonRaw);
	}
	
	public static Json stopTaps(String name) {
		String id = "", password ="";
		String ip = null;
		for(Json j : configuredTaps) {
			if(j.at("idname").asString().contentEquals(name)) {
				ip = j.at("ip").asString();
				id = j.at("username").asString();
				password = j.at("password").asString();
			}
		}
		SshUtils ssh = new SshUtils(id,password,ip,22/*,knownHostFilePath*/);	
		String command = STOP_TAPS;
		String output = null;
		try {
			output = ssh.remoteExec(command);
		} catch (JSchException | IOException e) {
			e.printStackTrace();
		}
		ssh.endConnection();

		Json out = Json.object();
		out.at("result", output);
		return out;
	}
	
	public static Json startTaps(String name) {
		String id = "", password ="";
		String ip = null;
		
		for(Json j : configuredTaps) {
			if(j.at("idname").asString().contentEquals(name)) {
				name=j.get("idname").asString();
				ip = j.at("ip").asString();
				id = j.at("username").asString();
				password = j.at("password").asString();
			}
		}
		System.out.println("Start tap:"+name+" "+ip);
		SshUtils ssh = new SshUtils(id,password,ip,22/*,knownHostFilePath*/);	
		String command = START_TAPS;
		String output = null;
		try {
			output = ssh.remoteExecNoWait(command);
		} catch (JSchException | IOException e) {
			e.printStackTrace();
		}
		ssh.endConnection();

		Json out = Json.object();
		out.at("result", output);
		return out;
	}
	
	
	public static Json startReplay(String name, String pcap) {
		String id = "", password ="";
		String ip = null;
		
		for(Json j : configuredTaps) {
			if(j.at("idname").asString().contentEquals(name)) {
				name=j.get("idname").asString();
				ip = j.at("ip").asString();
				id = j.at("username").asString();
				password = j.at("password").asString();
			}
		}
		System.out.println("Start script replay <"+pcap+"> on tap :"+name+" "+ip);
		SshUtils ssh = new SshUtils(id,password,ip,22/*,knownHostFilePath*/);	
		String command = REPLAY+pcap;
		System.out.println("Command :"+command);
		String output = null;
		try {
			output = ssh.remoteExecNoWait(command);
		} catch (JSchException | IOException e) {
			e.printStackTrace();
		}
		ssh.endConnection();

		Json out = Json.object();
		out.at("result", output);
		return out;
	}
	
	public static Json clearLogs(String name) {
		String id = "", password ="";
		String ip = null;
		for(Json j : configuredTaps) {
			if(j.at("idname").asString().contentEquals(name)) {
				ip = j.at("ip").asString();
				id = j.at("username").asString();
				password = j.at("password").asString();
			}
		}
		SshUtils ssh = new SshUtils(id,password,ip,22/*,knownHostFilePath*/);	
		String command = CLEAR_SURICATA_LOG;
		String output = null;
		try {
			output = ssh.remoteExec(command);
		} catch (JSchException | IOException e) {
			e.printStackTrace();
		}
		ssh.endConnection();

		Json out = Json.object();
		out.at("result", output);
		return out;
	}
	
	private static void writeToFile(String s, String path) throws IOException {

		System.out.println("write "+path);
		System.out.println(s);
	      FileWriter myWriter = new FileWriter(path);
	      myWriter.write(s);
	      myWriter.close();
	}
	
	public static void newTap(String name) {
		File theDir = new File(idsconf+"/taps/"+name);
		if (!theDir.exists()){
		    theDir.mkdirs();
		}		
		Json j = Json.object();
		j.at("idname",name);
		j.at("tap_id",name);
		j.at("includes",new ArrayList<>());

		configuredTaps.add(j);
		
		Json basicConf = null;
		try {
			basicConf = readJsonFile(idsconf+"/taps/basicNetworkConf.json");
			basicConf.at("tap_id",name);
			basicConf.at("csl_node_ip",localIP);
			basicConf.at("csl_node_port",localPort);
			writeToFile(basicConf.toString(), idsconf+"/taps/"+name+"/tapReseau.json");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			basicConf = readJsonFile(idsconf+"/taps/basicProcessConf.json");
			basicConf.at("tap_id",name);
			for(Json jj : basicConf.at("modules").asJsonList()) {
				if(jj.at("name").asString().contentEquals("module2")) {
					jj.at("config").at("host_target",localIP);
					jj.at("config").at("port_target",localPort);
				}
			}

			writeToFile(basicConf.toString(), idsconf+"/taps/"+name+"/tapProcess.json");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void deleteTap(String name) {
		ArrayList<Json> tapsclone = (ArrayList<Json>) configuredTaps.clone();
		for(Json j : configuredTaps) {
			if(j.at("idname").asString().contentEquals(name)) {
				tapsclone.remove(j);
			}
		}
		configuredTaps = tapsclone;
		try {
			FileUtils.deleteDirectory(new File(idsconf+"/taps/"+name));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void setIp(String name, String ip) {
		for(Json j : configuredTaps) {
			
			System.out.println("tap="+j);
			if(j.at("idname").asString().contentEquals(name)) {
				j.set("ip",ip);
				System.out.println(j);
				System.out.println(configuredTaps);
				
			}
		}
		System.out.println(configuredTaps);

	}
	
	public static Json getSuricataLogs(String name) {
		String username = "", password ="";

		Json result = Json.object();
		for(Json j : configuredTaps) {
			if(j.at("idname").asString().contentEquals(name)) {
				String ip = j.at("ip").asString();
				username = j.at("username").asString();
				password = j.at("password").asString();
				System.out.println("id "+username+" password "+password+" ip "+ip);
				SshUtils ssh = new SshUtils(username,password,ip,22/*,knownHostFilePath*/);
				try {
					ssh.getFile("/var/log/suricata/suricata.log",idsconf+"/taps/"+name+"/suricataLogs.txt");
				} catch (IOException | JSchException e) {
					e.printStackTrace();
				}
				ssh.endConnection();

			}
		}
		return result;
	}
	
	public static Json getConfFromtap(String name, String file) {
		String username = "", password ="";

		Json result = Json.object();
		for(Json j : configuredTaps) {
			System.out.println(j);
			if(j.at("idname").asString().contentEquals(name)) {
				String ip = j.at("ip").asString();
				username = j.at("username").asString();
				password = j.at("password").asString();
				System.out.println("id "+username+" password "+password+" ip "+ip);
				SshUtils ssh = new SshUtils(username,password,ip,22/*,knownHostFilePath*/);
				try {
					switch (file) {
						case "reseau":
							ssh.getFile("/home/"+username+"/csl/tapReseau/conf.json",idsconf+"/taps/"+name+"/tapReseau.json");
							break;
						case "process":
							ssh.getFile("/home/"+username+"/csl/tapProcess/CSLConfigTAPProcess.json",idsconf+"/taps/"+name+"/tapProcess.json");
							break;
						case "suricataconf":
							ssh.getFile("/home/"+username+"/csl/configSuricata/suricata/suricata.yaml",idsconf+"/taps/"+name+"/suricata.yaml");
							break;
						case "genrules":
							ssh.getFile("/home/"+username+"/csl/configSuricata/suricata/rules/csl.rules",idsconf+"/taps/"+name+"/genrules.rules");
							break;
						case "baserules":
							ssh.getFile("/home/"+username+"/csl/configSuricata/suricata/rules/cslbase.rules",idsconf+"/taps/"+name+"/baserules.rules");
							break;
					}
				} catch (IOException | JSchException e) {
					e.printStackTrace();
				}
				ssh.endConnection();

			}
		}
		return result;
	}
	
	public static void sendConfToTap(String name, String file) {
		String username = "", password ="";

		for(Json j : configuredTaps) {
			if(j.at("idname").asString().contentEquals(name)) {
				String ip = j.at("ip").asString();
				username = j.at("username").asString();
				password = j.at("password").asString();
				SshUtils ssh = new SshUtils(username,password,ip,22/*,knownHostFilePath*/);
				try {
					switch(file) {
						case "reseau":
							ssh.sendFile(idsconf+"/taps/"+name+"/tapReseau.json","/home/"+username+"/csl/tapReseau/conf.json");
							break;
						case "process":
							ssh.sendFile(idsconf+"/taps/"+name+"/tapProcess.json","/home/"+username+"/csl/tapProcess/CSLConfigTAPProcess.json");
							break;
						case "suricataconf":
							ssh.sendFile(idsconf+"/taps/"+name+"/suricata.yaml","/home/"+username+"/csl/configSuricata/suricata/suricata.yaml");
							break;
						case "genrules":
							ssh.sendFile(idsconf+"/taps/"+name+"/genrules.rules","/home/"+username+"/csl/configSuricata/suricata/rules/csl.rules");
							break;
						case "baserules":
							ssh.sendFile(idsconf+"/taps/"+name+"/baserules.rules","/home/"+username+"/csl/configSuricata/suricata/rules/cslbase.rules");
							break;
					}
				} catch (IOException | JSchException e) {
					e.printStackTrace();
				}
				ssh.endConnection();

			}
		}
	}
	
	public static void sendIncludes(String name) {
		String username = "", password ="";

		for(Json j : configuredTaps) {
			if(j.at("idname").asString().contentEquals(name)) {
				String ip = j.at("ip").asString();
				username = j.at("username").asString();
				password = j.at("password").asString();
				// Declaration
				try {
					Json includes = j.at("includes");
					ArrayList<String> ruleFiles = new ArrayList<String>();
					Json includesRaw = readJsonFile(idsconf+"/taps/includesConfiguration.json");
					for(Json jj : includesRaw.asJsonList()) {
						for(Json jjj : includes.asJsonList()) {
							if(jjj.asInteger() == jj.at("id").asInteger()) {
								for(Json jjjj : jj.at("files").asJsonList()) {
									ruleFiles.add(jjjj.asString());
								}
							}
						}
					}
					SshUtils ssh = new SshUtils(username,password,ip,22/*,knownHostFilePath*/);
					ssh.remoteExec("sudo rm /home/"+username+"/configSuricata/suricata/rules/additionnalRules/*.rules");
					ssh.endConnection();
					String yamlFile = "";
					yamlFile += "%YAML 1.1\r\n---\r\n- csl.rules\r\n- cslbase.rules\r\n";
					for(String ruleFile : ruleFiles) {
						System.out.println("Sending "+ruleFile);
						yamlFile += "- /home/"+username+"/configSuricata/suricata/rules/additionnalRules/"+ruleFile+"\r\n";
						ssh = new SshUtils(username,password,ip,22/*,knownHostFilePath*/);
						ssh.sendFile(idsconf+"/taps/rules/"+ruleFile,"/home/"+username+"/configSuricata/suricata/rules/additionnalRules/"+ruleFile);
						ssh.endConnection();
					}
					writeToFile(yamlFile, idsconf+"/taps/"+name+"/includes.yaml");
					ssh = new SshUtils(username,password,ip,22/*,knownHostFilePath*/);
					ssh.sendFile(idsconf+"/taps/"+name+"/includes.yaml","/home/"+username+"/configSuricata/suricata/includes.yaml");
					ssh.endConnection();					
				} catch (IOException | JSchException  e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void setUsernamePassword(String name, String username, String password) {
		for(Json j : configuredTaps) {
			if(j.at("idname").asString().contentEquals(name)) {
				j.set("password",password);
				j.set("username",username);

			}
		}
	}
	public static void setNetworkName(String name, String networkName) {
		for(Json j : configuredTaps) {
			if(j.at("idname").asString().contentEquals(name)) {
				j.at("networkName",networkName);

			}
		}
	}
	public static Json startSuricata(String name) {
		String id = "", password ="";

		String ip = null;
		for(Json j : configuredTaps) {
			if(j.at("idname").asString().contentEquals(name)) {
				ip = j.at("ip").asString();
				id = j.at("username").asString();
				password = j.at("password").asString();
			}
		}
		SshUtils ssh = new SshUtils(id,password,ip,22/*,knownHostFilePath*/);	
		String command = START_SURICATA;
		String output = null;
		try {
			output = ssh.remoteExecNoWait(command);
		} catch (JSchException | IOException e) {
			e.printStackTrace();
		}
		ssh.endConnection();

		Json out = Json.object();
		out.at("result", output);
		return out;
	}
	
	public static Json stopSuricata(String name) {
		String id = "", password ="";

		String ip = null;
		for(Json j : configuredTaps) {
			if(j.at("idname").asString().contentEquals(name)) {
				ip = j.at("ip").asString();
				id = j.at("username").asString();
				password = j.at("password").asString();
			}
		}
		SshUtils ssh = new SshUtils(id,password,ip,22/*,knownHostFilePath*/);	
		String command = STOP_SURICATA;
		String output = null;
		try {
			output = ssh.remoteExec(command);
		} catch (JSchException | IOException e) {
			e.printStackTrace();
		}
		ssh.endConnection();

		Json out = Json.object();
		out.at("result", output);
		return out;
	}
	

	
	public static Json reloadRules(String name) {
		String id = "", password ="";

		String ip = null;
		for(Json j : configuredTaps) {
			if(j.at("idname").asString().contentEquals(name)) {
				ip = j.at("ip").asString();
				id = j.at("username").asString();
				password = j.at("password").asString();
			}
		}
		SshUtils ssh = new SshUtils(id,password,ip,22/*,knownHostFilePath*/);	
		String command = "sudo kill -USR2 `cat ~/configSuricata/suricataPID`";
		String output = null;
		try {
			output = ssh.remoteExec(command);
		} catch (JSchException | IOException e) {
			e.printStackTrace();
		}
		ssh.endConnection();

		Json out = Json.object();
		out.at("result", output);
		return out;
	}


	public TapsServices() {
		this.name="taps";
		this.configFileSectionName="ssh_service";
	}
	
	public TapsServices(String name, String configFileSectionName) {
		this.name=name;
		this.configFileSectionName=configFileSectionName;
	}
	
	

	@Override
	public String getConfigFileSectionName() {
		return configFileSectionName;
	}

	
	
	
	public String getTapName(Json j) {
		
		
		String n=JsonUtil.getStringFromJson(j, "name", "");
		return n;
	
		
	}
	
	
	public String tapNameHasError(Json j) {
		
		
		String n=JsonUtil.getStringFromJson(j, "name", "");
		
		if (n.isEmpty()) return "No tap name";
		
		String x=idsconf+"/taps/"+n;
		
		File file = new File(x);
		 
        if (!file.isDirectory()) return "No directory for tap "+n;
        
		return "";
		
	}
	
	
	public String missingParams(Json j,String ... params) {
		
		String e="";
		for (String s:params) {
			if (!j.has(s)) {
				if (!e.isEmpty()) s=s+",";
			}
		}
		
		if (!e.isEmpty()) e="Missing params:"+e;
		return e;
	}
	
	@Override
	public boolean init(Json config, String cslDir) {
		System.out.println("Initializing SSH taps commands ..");
		idsconf = CSLContext.instance.getCslConfDir();
		
		System.out.println(idsconf);
		Json conf;
		try {
			conf = readJsonFile(idsconf+"/taps/TapsConfiguration.json");
			if(conf.isArray())
				configuredTaps = (ArrayList<Json>) conf.asJsonList();
			else 
				configuredTaps =  new ArrayList<Json>();

			
		} catch (IOException e1) {
			System.err.println("No tap config found");
			configuredTaps =  new ArrayList<Json>();
		}
		knownHostFilePath = config.at("knowHostFilePath").asString();
		localIP = config.at("localIpAddr").asString();
		localPort = config.at("localPort").asString();

		addCmd("newTap", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				System.out.println("paramètres de newTap :"+params.toString());
				System.out.println("nom utilisé :"+params.at("name").asString());

				String error=(missingParams(params, "name"));
				if (!error.isEmpty()) return  Json.object().set("error",error);
				
				newTap(params.at("name").asString());
				Json write = Json.object();
				write.at("write",configuredTaps);
				try {
					writeToFile(write.at("write").toString(), idsconf+"/taps/TapsConfiguration.json");
				} catch (IOException e) {
					e.printStackTrace();
				}
				return Json.object();
			}
		}, new JsonCmdHelp()
				.setDesc("Creation of a new tap description")
				.setParam("name", "name of the tap (id) ", JsonCmdHelp.STR)
			//	.setResult("nothing", JsonCmdHelp.JSON)
				.setStatus(JsonCmdHelp.STATUS_OK)
				);
		
		addCmd("tapNumber", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				Json j = Json.object();
				j.at("number",configuredTaps.size());
				return j;
			}
		});	
		
		addCmd("deleteTap", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				deleteTap(params.at("name").asString());
				Json write = Json.object();
				write.at("write",configuredTaps);
				try {
					writeToFile(write.at("write").toString(), idsconf+"/taps/TapsConfiguration.json");
				} catch (IOException e) {
					e.printStackTrace();
				}
				return Json.object();
			}
		});	
		
		addCmd("setIp", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				
				setIp(params.at("name").asString(), params.at("ip").asString());
				Json write = Json.object();
				write.at("write",configuredTaps);
				try {
					writeToFile(write.at("write").toString(), idsconf+"/taps/TapsConfiguration.json");
				} catch (IOException e) {
					e.printStackTrace();
				}
				return Json.object();
			}
		});		
		
		addCmd("setUsernamePassword", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				
				setUsernamePassword(params.at("name").asString(), params.at("username").asString(),params.at("password").asString());
				Json write = Json.object();
				write.at("write",configuredTaps);
				try {
					writeToFile(write.at("write").toString(), idsconf+"/taps/TapsConfiguration.json");
				} catch (IOException e) {
					e.printStackTrace();
				}
				return Json.object();
			}
		});
		addCmd("setNetworkName", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				setNetworkName(params.at("name").asString(), params.at("networkName").asString());
				Json write = Json.object();
				write.at("write",configuredTaps);
				try {
					writeToFile(write.at("write").toString(), idsconf+"/taps/TapsConfiguration.json");
				} catch (IOException e) {
					e.printStackTrace();
				}
				return Json.object();
			}
		});
		addCmd("getConfFromTap", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				Json result = Json.object();
				switch(params.at("param").asString()) {
					case "reseau":
						result = getConfFromtap(params.at("name").asString(), "reseau");
						break;
					case "process":
						result = getConfFromtap(params.at("name").asString(), "process");
						break;	
					case "suricataconf":
						result = getConfFromtap(params.at("name").asString(), "suricataconf");
						break;	
					case "genrules":
						result = getConfFromtap(params.at("name").asString(), "genrules");
						break;							
					case "baserules":
						result = getConfFromtap(params.at("name").asString(), "baserules");
						break;							
				}
				return result;
			}
		});	
		
		addCmd("sendConfToTap", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				switch(params.at("param").asString()) {
					case "reseau":
						sendConfToTap(params.at("name").asString(), "reseau");
						break;
					case "process":
						sendConfToTap(params.at("name").asString(), "process");
						break;	
					case "suricataconf":
						sendConfToTap(params.at("name").asString(), "suricataconf");
						break;	
					case "genrules":
						sendConfToTap(params.at("name").asString(), "genrules");
						break;							
					case "baserules":
						sendConfToTap(params.at("name").asString(), "baserules");
						break;							
				}
				return Json.object();
			}
		});		
		
		addCmd("getTapConf", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				try {
					return readJsonFile(idsconf+"/taps/TapsConfiguration.json");
				} catch (IOException e) {
					e.printStackTrace();
				}
				return Json.object();
			}
		});		
		
		addCmd("getTapsConfiguration", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				try {
					return readJsonFile(idsconf+"/taps/TapsConfiguration.json");
				} catch (IOException e) {
					e.printStackTrace();
				}
				return Json.object();
			}
		});		
		
		addCmd("getTapState", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				Json j = Json.object();
				j.at("state","IDLE");
				return j;
			}
		});	
		
	
		
		addCmd("startTap", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				return startTaps(params.at("name").asString());
			}
		}, new JsonCmdHelp()
				.setDesc("Start tap")
				.setParam("name", "name of the tap (id) ", JsonCmdHelp.STR)
			//	.setResult("nothing", JsonCmdHelp.JSON)
				.setStatus(JsonCmdHelp.STATUS_OK)
				);		
		
		addCmd("stopTap", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				return stopTaps(params.at("name").asString());
			}
		});			
		
		
		addCmd("replay", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				String name = JsonUtil.getStringFromJson(params, "name", "???");
				String pcap = JsonUtil.getStringFromJson(params, "pcap_file", "???");
				
				return startReplay(name,pcap);
			}
		}, new JsonCmdHelp()
				.setDesc("Start replay of a pcap")
				.setParam("name", "name of the tap (id) ", JsonCmdHelp.STR)
				.setParam("pcap_file", "name of file to replay (must be in /csl) ", JsonCmdHelp.STR)
				
			//	.setResult("nothing", JsonCmdHelp.JSON)
				.setStatus(JsonCmdHelp.STATUS_OK)
				);	

		addCmd("startSuricata", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				return startSuricata(params.at("name").asString());
			}
		});		
		
		addCmd("stopSuricata", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				return stopSuricata(params.at("name").asString());
			}
		});		
		
		addCmd("reloadRules", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				return reloadRules(params.at("name").asString());
			}
		});	
		
		// Setter et getter de l'édition de Json graphique
		addCmd("getProcessJson", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				try {
					return readJsonFile(idsconf+"/taps/"+params.at("name").asString()+"/tapProcess.json");
				} catch (IOException e) {
					e.printStackTrace();
				}
				return Json.object();
			}
		});	
		addCmd("getNetworkJson", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				try {
					return readJsonFile(idsconf+"/taps/"+params.at("name").asString()+"/tapReseau.json");
				} catch (IOException e) {
					e.printStackTrace();
				}
				return Json.object();			}
		});	
		addCmd("setProcessJson", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				try {
					writeToFile(params.at("conf").toString(),idsconf+"/taps/"+params.at("name").asString()+"/tapProcess.json" );
				} catch (IOException e) {
					e.printStackTrace();
				}
				return Json.object();
			}
		});	
		addCmd("setNetworkJson", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				try {
					writeToFile(params.at("conf").toString(),idsconf+"/taps/"+params.at("name").asString()+"/tapReseau.json" );
				} catch (IOException e) {
					e.printStackTrace();
				}
				return Json.object();			}
		});	
		
		// Same but for suricata
		addCmd("getSuricataConf", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				Json j = Json.object();
				try {
					j.at("conf",readFile(idsconf+"/taps/"+params.at("name").asString()+"/suricata.yaml"));
				} catch (IOException e) {
					e.printStackTrace();
				}
				return j;
			}
		});	
		addCmd("getBaseRules", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				System.out.println("coucou");
				Json j = Json.object();
				try {
					j.at("conf",readFile(idsconf+"/taps/"+params.at("name").asString()+"/baserules.rules"));
				} catch (IOException e) {
					e.printStackTrace();
				}
				System.out.println(j);
				return j;
			}
		});	
		addCmd("getGenRules", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				Json j = Json.object();
				
				System.out.println("IDSCONF:"+idsconf);
				try {
					j.at("conf",readFile(idsconf+"/taps/"+params.at("name").asString()+"/genrules.rules"));
				} catch (IOException e) {
					e.printStackTrace();
				}
				return j;
			}
		});		
		addCmd("setSuricataConf", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				try {
					writeToFile(params.at("conf").asString(),idsconf+"/taps/"+params.at("name").asString()+"/suricata.yaml");
				} catch (IOException e) {
					e.printStackTrace();
				}
				return Json.object();
			}
		});	
		addCmd("setBaseRules", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				try {
					writeToFile(params.at("conf").asString(),idsconf+"/taps/"+params.at("name").asString()+"/baserules.rules" );
				} catch (IOException e) {
					e.printStackTrace();
				}
				return Json.object();			
			}
		});	
		addCmd("setGeneratedRules", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				try {
					writeToFile(params.at("conf").asString(),idsconf+"/taps/"+params.at("name").asString()+"/genrules.rules" );
				} catch (IOException e) {
					e.printStackTrace();
				}
				return Json.object();			}
		});	
		addCmd("getSuricataLogs", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				Json result = Json.object();
				result = getSuricataLogs(params.at("name").asString());
				String content = "";
				try {
					content = readFile(idsconf+"/taps/"+params.at("name").asString()+"/suricataLogs.txt");
				} catch (IOException e) {
					e.printStackTrace();
				}
				result.at("logs",content);
				return result;		
			}
		});		
		addCmd("clearSuricataLogs", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				clearLogs(params.at("name").asString());
				Json result = Json.object();
				try {
					writeToFile("",idsconf+"/taps/"+params.at("name").asString()+"/suricataLogs.txt" );
				} catch (IOException e) {
					e.printStackTrace();
				}
				result.at("logs","");
				return result;		
			}
		});	
		addCmd("sendIncludes", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				sendIncludes(params.at("name").asString());
				return Json.object();		
			}
		});		
		addCmd("test", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				try {
					return readJsonFile("./datafile/world.json");
				} catch (IOException e) {
				}
				return Json.object();
			}
		});			
		addCmd("setInclude", new IJsonCmd() {
			@Override
			public Json exec(Json params) {

				ArrayList<Integer> includeList = new ArrayList<>();
				for(Json j : params.at("include").asJsonList()) {
					if(j.at("checked").asBoolean()) {
						includeList.add(j.at("id").asInteger());
					}
				}
				for(Json good : configuredTaps) {
					if(good.at("idname").asString().contentEquals(params.at("name").asString())) {
						good.atDel("includes");
						good.at("includes",includeList);
					}
				}
				Json write = Json.object();
				write.at("write",configuredTaps);
				try {
					writeToFile(write.at("write").toString(), idsconf+"/taps/TapsConfiguration.json");
				} catch (IOException e) {
					e.printStackTrace();
				}
				return Json.object();		
			}
		});
		addCmd("getIncludes", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				try {
					Json includes = Json.object();
					Json taps = readJsonFile(idsconf+"/taps/TapsConfiguration.json");

					for(Json k : taps.asJsonList()) {
						Json includesRaw = Json.object();
						ArrayList<Json> includesRawClone = new ArrayList<Json>();							
						includesRaw = readJsonFile(idsconf+"/taps/includesConfiguration.json");
						for(Json j : includesRaw.asJsonList()) {
							for(Json r : k.at("includes").asJsonList()) {
								if(j.at("id").asInteger() == r.asInteger()) {
									j.at("checked",true );
								}
														
							}
							includesRawClone.add(j);
						}
						for(Json j : includesRawClone) {
							if(JsonUtil.getJson(j,"checked") == null)
								j.at("checked", false);
						}
						includes.at(k.at("idname").asString(),includesRawClone);
					}
					
					
					return includes;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				return Json.object();		
			}
		});			
		System.out.println("SSH commands operationnal");
		
		
		
		
		
		return true;
	}

	
	public String addCmd(String name, IJsonCmd j) {
		return apiCommands.registerCmd(name, j);
	}
	
	
	public String addCmd(String name, IJsonCmd j, IJsonCmdHelp jh) {
		return apiCommands.registerCmd(name, j,jh);
	}

	@Override
	public IApiCommands getApiCommands() {
		// TODO Auto-generated method stub
		apiCommands.setName(name);
		return apiCommands;
	}
	
	@Override
	public boolean terminate() {
		// TODO Auto-generated method stub
		return false;
	}
}
