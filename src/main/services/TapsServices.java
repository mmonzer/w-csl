package main.services;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.csl.ids.Tap;
import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.status.IStatusProvider;
import com.csl.modules.ModuleIDS;
import com.google.gson.JsonObject;
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
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpPost;



import main.services.JsonApiResponse;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TapsServices extends Service {
	private static final String SCRIPTS_DIR = "~/csl/scripts";
	private static final String START_TAPS = "cd " + SCRIPTS_DIR + " && sudo ./launchTap.sh & exit";
	private static final String STOP_TAPS = "cd " + SCRIPTS_DIR + " && sudo ./killTaps.sh";

	private static final String REPLAY = "cd " + SCRIPTS_DIR + " && sudo ./replay.sh ";

	private static final String STOP_SURICATA = "cd " + SCRIPTS_DIR + " && sudo ./killSuricata.sh";
	private static final String START_SURICATA = "cd " + SCRIPTS_DIR + " && sudo ./launchSuricata.sh";
	private static final String CLEAR_SURICATA_LOG = "sudo rm /var/log/suricata/suricata.log";
	private static final String SURICATA_CONF_DIR = "/opt/csl/configSuricata";
	private static final String REMOVE_ADDITIONAL_RULES = "cd " + SCRIPTS_DIR + " && sudo ./removeAdditionnalRules.sh";
	private static final String RELOAD_RULES = "cd " + SCRIPTS_DIR + " && sudo ./reloadSuricataRules.sh";

	static ArrayList<Json> configuredTaps;
	static HashMap<String, Tap> taps = new HashMap<>();
	static String localIP;	
	static String localPort;
	static String knownHostFilePath;

	private final ScanApiHandler apiHandler;

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
		if (!taps.containsKey(name)) {
			return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
		}
		taps.get(name).sendCmd("/monitoring",
				Json.read("{\"cmd\":\"monitorStop\"}")).toJson();
		taps.get(name).sendCmd("/suricata",
				Json.read("{\"cmd\":\"suricataStop\"}")).toJson();
		return JsonApiResponse.result(Json.read("{\"msg\":\"Tap "+name+" was successfully stopped\"}")).toJson();
	}
	
	public static Json startTaps(String name) {
		if (!taps.containsKey(name)) {
			return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
		}
		taps.get(name).sendCmd("/monitoring",
				Json.read("{\"cmd\":\"monitorStart\"}")).toJson();
		taps.get(name).sendCmd("/suricata",
				Json.read("{\"cmd\":\"suricataStart\"}")).toJson();
		return JsonApiResponse.result(Json.read("{\"msg\":\"Tap "+name+" was successfully started\"}")).toJson();
	}

	public static Json startReplay(String name, String pcap) {
		String id = "", password ="";
		String ip = null;
		int port = 22;
		
		for(Json j : configuredTaps) {
			if(j.at("idname").asString().contentEquals(name)) {
				name=j.get("idname").asString();
				ip = j.at("ip").asString();
				id = j.at("username").asString();
				password = j.at("password").asString();
				try {
					port = j.at("port").asInteger();
				} catch (NullPointerException e) {
					System.out.println("Using default SSH port (22)");
				}
			}
		}
		System.out.println("Start script replay <"+pcap+"> on tap :"+name+" "+ip);
		SshUtils ssh = new SshUtils(id,password,ip,port/*,knownHostFilePath*/);
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
		int port = 22;
		for(Json j : configuredTaps) {
			if(j.at("idname").asString().contentEquals(name)) {
				ip = j.at("ip").asString();
				id = j.at("username").asString();
				password = j.at("password").asString();
				try {
					port = j.at("port").asInteger();
				} catch (NullPointerException e) {
					System.out.println("Using default SSH port (22)");
				}
			}
		}
		SshUtils ssh = new SshUtils(id,password,ip,port/*,knownHostFilePath*/);
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
		j.at("id",name);
		j.at("includes",new ArrayList<>());

		configuredTaps.add(j);
		
		Json basicConf = null;
		try {
			basicConf = readJsonFile(idsconf+"/taps/basicNetworkConf.json");
			basicConf.at("id",name);
			basicConf.at("csl_node_ip",localIP);
			basicConf.at("csl_node_port",localPort);
			writeToFile(basicConf.toString(), idsconf+"/taps/"+name+"/tapReseau.json");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			basicConf = readJsonFile(idsconf+"/taps/basicProcessConf.json");
			basicConf.at("id",name);
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

	public static void setPort(String name, int port) {
		for(Json j : configuredTaps) {

			System.out.println("tap="+j);
			if(j.at("idname").asString().contentEquals(name)) {
				j.set("port",port);
				System.out.println(j);
				System.out.println(configuredTaps);

			}
		}
		System.out.println(configuredTaps);

	}

	public static Json getSuricataLogs(String name) {
		String username = "", password ="";
		int port = 22;

		Json result = Json.object();
		for(Json j : configuredTaps) {
			if(j.at("idname").asString().contentEquals(name)) {
				String ip = j.at("ip").asString();
				username = j.at("username").asString();
				password = j.at("password").asString();
				try {
					port = j.at("port").asInteger();
				} catch (NullPointerException e) {
					System.out.println("Using default SSH port (22)");
				}
				System.out.println("id "+username+" password "+password+" ip "+ip+" port "+port);
				SshUtils ssh = new SshUtils(username,password,ip,port/*,knownHostFilePath*/);
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
		int port = 22;

		Json result = Json.object();
		for(Json j : configuredTaps) {
			System.out.println(j);
			if(j.at("idname").asString().contentEquals(name)) {
				String ip = j.at("ip").asString();
				username = j.at("username").asString();
				password = j.at("password").asString();
				try {
					port = j.at("port").asInteger();
				} catch (NullPointerException e) {
					System.out.println("Using default SSH port (22)");
				}
				System.out.println("id "+username+" password "+password+" ip "+ip);
				SshUtils ssh = new SshUtils(username,password,ip,port/*,knownHostFilePath*/);
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

	/**
	 * Gets the configuration of the only TAP
	 * @return the configuration of the TAP
	 */
	public static String getConfFromTap() {
		// TODO : change url and deal with answer
		HttpResponse<String> response = null;
		String url = "http://localhost:8888/config";
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.POST(HttpRequest.BodyPublishers.ofString("{\"cmd\":\"getConfig\"}"))
				.build();
		try {
			response = client.send(request, HttpResponse.BodyHandlers.ofString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response.body();
	}

	public static void sendConfToTap(String name, String file) {
		String username = "", password ="";
		int port = 22;

		for(Json j : configuredTaps) {
			if(j.at("idname").asString().contentEquals(name)) {
				String ip = j.at("ip").asString();
				username = j.at("username").asString();
				password = j.at("password").asString();
				try {
					port = j.at("port").asInteger();
				} catch (NullPointerException e) {
					System.out.println("Using default SSH port (22)");
				}
				SshUtils ssh = new SshUtils(username,password,ip,port/*,knownHostFilePath*/);
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
		int port = 22;

		for(Json j : configuredTaps) {
			if(j.at("idname").asString().contentEquals(name)) {
				String ip = j.at("ip").asString();
				username = j.at("username").asString();
				password = j.at("password").asString();
				try {
					port = j.at("port").asInteger();
				} catch (NullPointerException e) {
					System.out.println("Using default SSH port (22)");
				}
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
					SshUtils ssh = new SshUtils(username,password,ip,port/*,knownHostFilePath*/);
					// ssh.remoteExec("sudo rm /home/"+username+"/configSuricata/suricata/rules/additionnalRules/*.rules");
					ssh.remoteExec(REMOVE_ADDITIONAL_RULES);
					ssh.endConnection();
					String yamlFile = "";
					yamlFile += "%YAML 1.1\r\n---\r\n- csl.rules\r\n- cslbase.rules\r\n";
					for(String ruleFile : ruleFiles) {
						System.out.println("Sending "+ruleFile);
						yamlFile += "- /home/"+username+"/configSuricata/suricata/rules/additionnalRules/"+ruleFile+"\r\n";
						ssh = new SshUtils(username,password,ip,port/*,knownHostFilePath*/);
						ssh.sendFile(idsconf+"/taps/rules/"+ruleFile,"/home/"+username+"/configSuricata/suricata/rules/additionnalRules/"+ruleFile);
						ssh.endConnection();
					}
					writeToFile(yamlFile, idsconf+"/taps/"+name+"/includes.yaml");
					ssh = new SshUtils(username,password,ip,port/*,knownHostFilePath*/);
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

	/**
	 * Starts the Suricata of the given TAP name
	 * @param name name of the TAP
	 * @return the result of the starting
	 */
	public Json startSuricata(String name) {
		if (!taps.containsKey(name)) {
			return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
		}
		return taps.get(name).sendCmd("/suricata",
				Json.read("{\"cmd\":\"suricataStart\"}")).toJson();
	}

	/**
	 * Stops the Suricata of the given TAP name
	 * @param name name of the TAP
	 * @return the result of the stopping
	 */
	public Json stopSuricata(String name) {
		if (!taps.containsKey(name)) {
			return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
		}
		return taps.get(name).sendCmd("/suricata",
				Json.read("{\"cmd\":\"suricataStop\"}")).toJson();
	}

	/**
	 * Starts the Monitor of the given TAP name
	 * @param name name of the TAP
	 * @return the result of the starting
	 */
	public Json startMonitor(String name) {
		if (!taps.containsKey(name)) {
			return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
		}
		return taps.get(name).sendCmd("/monitoring",
				Json.read("{\"cmd\":\"monitorStart\"}")).toJson();
	}

	/**
	 * Stops the Monitor of the given TAP name
	 * @param name name of the TAP
	 * @return the result of the stopping
	 */
	public Json stopMonitor(String name) {
		if (!taps.containsKey(name)) {
			return JsonApiResponse.error("Tap's name does not correspond to a configured Tap").toJson();
		}
		return taps.get(name).sendCmd("/monitoring",
				Json.read("{\"cmd\":\"monitorStop\"}")).toJson();
	}

	private static Json reloadRulesParseOutput(String output) {
		if (output == null) {
			return Json.object();
		}
		Json out = Json.object();
		if (output.startsWith("{")) {
			Json result = Json.read(output);
			out.at("result", result);
			if (!result.at("return").asString().equals("OK")) {
				out.at("error",true);
			}
		} else {
			Json result = Json.object();
			result.at("return", "NOK");
			result.at("message", output);
			out.at("result", result);
			out.at("error", true);
		}
		return out;
	}

	public static Json reloadRules(String name) {
		String id = "", password ="";
		int port = 22;

		String ip = null;
		for(Json j : configuredTaps) {
			if(j.at("idname").asString().contentEquals(name)) {
				ip = j.at("ip").asString();
				id = j.at("username").asString();
				password = j.at("password").asString();
				try {
					port = j.at("port").asInteger();
				} catch (NullPointerException e) {
					System.out.println("Using default SSH port (22)");
				}
			}
		}
		SshUtils ssh = new SshUtils(id,password,ip,port/*,knownHostFilePath*/);
		String command = RELOAD_RULES;
		String output = null;
		try {
			output = ssh.remoteExec(command);
		} catch (JSchException | IOException e) {
			e.printStackTrace();
		}
		ssh.endConnection();

		return reloadRulesParseOutput(output);
	}

	/**
	 * Default constructor of the Suricata service.
	 */
	public TapsServices() {
		this("taps",
				"Service for managing the different CSL-Probes: adding, configuring, ...",
				"ssh_service");
	}

	/**
	 * Generic constructor of the Suricata service.
	 */
	public TapsServices(String name, String description, String configFileSectionName) {
		super(name, description, configFileSectionName);
		apiHandler = new ScanApiHandler("http://localhost:8888");
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

	/**
	 * Initialization of the TAPs commands
	 * @param config the configuration section of the configuration file
	 * @param cslDir the CSL directory
	 * @return true if the initialization happened with no problems, false otherwise.
	 */
	@Override
	public boolean init(Json config, String cslDir) {
		System.out.println("Initializing SSH taps commands ..");
		idsconf = CSLContext.instance.getCslConfDir();
		
		System.out.println(idsconf);
		Json conf;
		try {
			conf = readJsonFile(idsconf+"/taps/TapsConfiguration.json");
			if(conf.isArray()) {
				configuredTaps = (ArrayList<Json>) conf.asJsonList();
			} else {
				configuredTaps = new ArrayList<Json>();
			}
			for (Json j : configuredTaps) {
				taps.put(j.at("idname").asString(),
						new Tap(j.at("idname").asString(),
								j.at("id").asString(),
								j.at("ip").asString(),
								j.at("port").asInteger(),
								j.at("includes").asJsonList()
						)
				);
			}
		} catch (IOException e1) {
			System.err.println("No tap config found");
			configuredTaps =  new ArrayList<Json>();
		}
		knownHostFilePath = config.at("knowHostFilePath").asString();
		localIP = config.at("localIpAddr").asString();
		localPort = config.at("localPort").asString();

		addCmd("new_tap", new IJsonCmd() {
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

		addCmd("tap_number", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				Json j = Json.object();
				j.at("number",configuredTaps.size());
				return j;
			}
		});

		addCmd("delete_tap", new IJsonCmd() {
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

		addCmd("set_ip", new IJsonCmd() {
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

		addCmd("set_port", new IJsonCmd() {
			@Override
			public Json exec(Json params) {

				setPort(params.at("name").asString(), params.at("port").asInteger());
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

		addCmd("set_username_password", new IJsonCmd() {
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
		addCmd("set_network_name", new IJsonCmd() {
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
		addCmd("get_conf_from_tap", (Json params)-> Json.read(getConfFromTap()));
		//			@Override
//			public Json exec(Json params) {
//				Json result = Json.object();
//				switch(params.at("param").asString()) {
//					case "reseau":
//						result = getConfFromtap(params.at("name").asString(), "reseau");
//						break;
//					case "process":
//						result = getConfFromtap(params.at("name").asString(), "process");
//						break;
//					case "suricataconf":
//						result = getConfFromtap(params.at("name").asString(), "suricataconf");
//						break;
//					case "genrules":
//						result = getConfFromtap(params.at("name").asString(), "genrules");
//						break;
//					case "baserules":
//						result = getConfFromtap(params.at("name").asString(), "baserules");
//						break;
//				}
//				return result;
//			}
//		});

		addCmd("send_conf_to_tap", new IJsonCmd() {
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

		addCmd("get_tap_conf", new IJsonCmd() {
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

		addCmd("get_taps_configuration", new IJsonCmd() {
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

		addCmd("get_tap_state", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				Json j = Json.object();
				j.at("state","IDLE");
				return j;
			}
		});

		addCmd("start_tap", new IJsonCmd() {
					@Override
					public Json exec(Json params) {
						if (!params.has("name")) {
							return JsonApiResponse.error("Tap's name missing from params").toJson();
						}
						return startTaps(params.at("name").asString());
					}
				}, new JsonCmdHelp()
						.setDesc("Start the suricata of the given tap")
						.setParam("name", "name of the tap where Suricata must start", JsonCmdHelp.STR)
						//	.setResult("nothing", JsonCmdHelp.JSON)
						.setStatus(JsonCmdHelp.STATUS_OK)
		);

		addCmd("stop_tap", new IJsonCmd() {
					@Override
					public Json exec(Json params) {
						if (!params.has("name")) {
							return JsonApiResponse.error("Tap's name missing from params").toJson();
						}
						return stopTaps(params.at("name").asString());
					}
				}, new JsonCmdHelp()
						.setDesc("Stop the suricata of the given tap")
						.setParam("name", "name of the tap where Suricata must stop", JsonCmdHelp.STR)
						//	.setResult("nothing", JsonCmdHelp.JSON)
						.setStatus(JsonCmdHelp.STATUS_OK)
		);

		addCmd("restart_tap", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				if (!params.has("name")) {
					return JsonApiResponse.error("Tap's name missing from params").toJson();
				}
				stopTaps(params.at("name").asString());
				startTaps(params.at("name").asString());

				return Json.object();
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

		addCmd("start_suricata", new IJsonCmd() {
					@Override
					public Json exec(Json params) {
						if (!params.has("name")) {
							return JsonApiResponse.error("Tap's name missing from params").toJson();
						}
						return startSuricata(params.get("name").asString());
					}
				}, new JsonCmdHelp()
						.setDesc("Start/restart Suricata")
						.setParam("name", "name of the tap, where suricata should be started.", JsonCmdHelp.STR)
						.setResult("If the (re)starting was successful", JsonCmdHelp.JSON)
						.setStatus(JsonCmdHelp.STATUS_OK)
		);

		addCmd("stop_suricata", new IJsonCmd() {
					@Override
					public Json exec(Json params) {
						if (!params.has("name")) {
							return JsonApiResponse.error("Tap's name missing from params").toJson();
						}
						return stopSuricata(params.get("name").asString());
					}
				}, new JsonCmdHelp()
						.setDesc("Stop Suricata")
						.setParam("name", "name of the tap, where suricata should be stop. ", JsonCmdHelp.STR)
						.setResult("If the stopping was successful", JsonCmdHelp.JSON)
						.setStatus(JsonCmdHelp.STATUS_OK)
		);

		addCmd("start_monitor", new IJsonCmd() {
					@Override
					public Json exec(Json params) {
						if (!params.has("name")) {
							return JsonApiResponse.error("Tap's name missing from params").toJson();
						}
						return startMonitor(params.get("name").asString());
					}
				}, new JsonCmdHelp()
						.setDesc("Start/restart Monitor in the given tap")
						.setParam("name", "name of the tap, where monitor should be started.", JsonCmdHelp.STR)
						.setResult("If the (re)starting was successful", JsonCmdHelp.JSON)
						.setStatus(JsonCmdHelp.STATUS_OK)
		);

		addCmd("stop_suricata", new IJsonCmd() {
					@Override
					public Json exec(Json params) {
						if (!params.has("name")) {
							return JsonApiResponse.error("Tap's name missing from params").toJson();
						}
						return stopMonitor(params.get("name").asString());
					}
				}, new JsonCmdHelp()
						.setDesc("Stop Monitor in the given tap.")
						.setParam("name", "name of the tap, where monitor should be stop. ", JsonCmdHelp.STR)
						.setResult("If the stopping was successful", JsonCmdHelp.JSON)
						.setStatus(JsonCmdHelp.STATUS_OK)
		);

		addCmd("reload_all_taps_rules", new IJsonCmd() {
			@Override
			public Json exec(Json params) {

				return reloadAllTapsRules();
			}
		});

		addCmd("reload_rules",	new IJsonCmd() {
					@Override
					public Json exec(Json params) {

						return apiHandler.sendRequestToScanManager(HttpMethod.POST,"/suricata",
								Json.read("{\"cmd\":\"suricataReloadRules\"}")).toJson();
					}
				},
				new JsonCmdHelp().setDesc("Reloads the rules of Suricata")
						.setResult("The entity as returned by CSL-Probe", IJsonCmdHelp.JSON)
						.setStatus(IJsonCmdHelp.STATUS_OK)
		);

		// Setter et getter de l'édition de Json graphique
		addCmd("get_process_json", new IJsonCmd() {
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
		addCmd("get_network_json", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				try {
					return readJsonFile(idsconf+"/taps/"+params.at("name").asString()+"/tapReseau.json");
				} catch (IOException e) {
					e.printStackTrace();
				}
				return Json.object();			}
		});
		addCmd("set_process_json", new IJsonCmd() {
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
		addCmd("set_network_json", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				try {
					writeToFile(params.at("conf").toString(),idsconf+"/taps/"+params.at("name").asString()+"/tapReseau.json" );
				} catch (IOException e) {
					e.printStackTrace();
				}
				return Json.object();			}
		});

		addCmd("get_suricata_status", new IJsonCmd() {
					@Override
					public Json exec(Json params) {
						return apiHandler.sendRequestToScanManager(HttpMethod.POST,"/suricata",Json.read("{\"cmd\":\"suricataStatus\"}")).toJson();
					}
				},
				new JsonCmdHelp().setDesc("Get the status of Suricata")
						.setResult("The entity as returned by CSL-Probe", IJsonCmdHelp.JSON)
						.setStatus(IJsonCmdHelp.STATUS_OK)
		);
		addCmd("get_suricata_conf", new IJsonCmd() {
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

		addCmd("get_base_rules", new IJsonCmd() {
					@Override
					public Json exec(Json params) {
						return apiHandler.sendRequestToScanManager(HttpMethod.POST,"/suricata",
								Json.read("{\"cmd\":\"suricataGetBaseRules\"}")).toJson();
					}
				},
				new JsonCmdHelp().setDesc("Retrieve the base rules of Suricata")
						.setResult("An object with the sid as key and the rule as value", IJsonCmdHelp.JSON)
						.setStatus(IJsonCmdHelp.STATUS_OK)
		);

		addCmd("get_gen_rules", new IJsonCmd() {
					@Override
					public Json exec(Json params) {
						return apiHandler.sendRequestToScanManager(HttpMethod.POST,"/suricata",
								Json.read("{\"cmd\":\"suricataGetCustomRules\"}")).toJson();
					}
				},
				new JsonCmdHelp().setDesc("Retrieve the generated rules of Suricata")
						.setResult("An object with the sid as key and the rule as value", IJsonCmdHelp.JSON)
						.setStatus(IJsonCmdHelp.STATUS_OK)
		);

		addCmd("modify_gen_rules", new IJsonCmd() {
					@Override
					public Json exec(Json params) {
						return apiHandler.sendRequestToScanManager(HttpMethod.POST,"/suricata",
								Json.read("{\"cmd\":\"suricataModifyCustomRules\",\"params\":"+params+"}")).toJson();
					}
				},
				new JsonCmdHelp().setDesc("Modify the generated rules of Suricata")
						.setParam("rules","A list of strings, each string is a rule", IJsonCmdHelp.JSON)
						.setResult("An object with the sid as keys and the rules as values", IJsonCmdHelp.JSON)
						.setStatus(IJsonCmdHelp.STATUS_OK)
		);

		addCmd("modify_base_rules", new IJsonCmd() {
					@Override
					public Json exec(Json params) {
						return apiHandler.sendRequestToScanManager(HttpMethod.POST,"/suricata",
								Json.read("{\"cmd\":\"suricataModifyBaseRules\",\"params\":"+params+"}")).toJson();
					}
				},
				new JsonCmdHelp().setDesc("Modify the base rules of Suricata")
						.setParam("rules","A list of strings, each string is a rule", IJsonCmdHelp.JSON)
						.setResult("An object with the sid as keys and the rules as values", IJsonCmdHelp.JSON)
						.setStatus(IJsonCmdHelp.STATUS_OK)
		);

		addCmd("set_suricata_conf", new IJsonCmd() {
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
		addCmd("set_base_rules", new IJsonCmd() {
					@Override
					public Json exec(Json params) {
						return apiHandler.sendRequestToScanManager(HttpMethod.POST,"/suricata",
								Json.read("{\"cmd\":\"suricataAddBaseRules\",\"params\":" + params.toString()+"}")).toJson();
					}
				},
				new JsonCmdHelp().setDesc("Set the base rules of Suricata")
						.setParam("rules","A list of strings, each string is a rule", IJsonCmdHelp.JSON)
						.setResult("The entity as returned by CSL-Probe, with the new updated rules", IJsonCmdHelp.JSON)
						.setStatus(IJsonCmdHelp.STATUS_OK));
		addCmd("set_generated_rules", new IJsonCmd() {
					@Override
					public Json exec(Json params) {
						return apiHandler.sendRequestToScanManager(HttpMethod.POST,"/suricata",
								Json.read("{\"cmd\":\"suricataAddCustomRules\",\"params\":" + params.toString()+"}")).toJson();
					}
				},
				new JsonCmdHelp().setDesc("Set the generated rules of Suricata")
						.setParam("rules","A list of strings, each string is a rule", IJsonCmdHelp.JSON)
						.setResult("The entity as returned by CSL-Probe, with the new updated rules", IJsonCmdHelp.JSON)
						.setStatus(IJsonCmdHelp.STATUS_OK));

		/*
		 * Update suricata rules in all taps
		 */
		addCmd("update_all_taps_suricata_rules", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				Json response = null;
				for (Json tapConfig: configuredTaps) {
					response = apiHandler.sendRequestToScanManager(HttpMethod.POST,"/suricata",
							Json.read("{\"cmd\":\"suricataAddCustomRules\",\"params\":" + params.toString()+"}")).toJson();
				}
				return response;
			}
		});

		addCmd("get_suricata_logs", new IJsonCmd() {
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
		addCmd("clear_suricata_logs", new IJsonCmd() {
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
		addCmd("send_includes", new IJsonCmd() {
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
		addCmd("set_include", new IJsonCmd() {
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
		addCmd("get_includes", new IJsonCmd() {
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

		// Deprecated cmd
		// TODO : remove eventually
		{
			addCmd("newTap", new IJsonCmd() {
						@Override
						public Json exec(Json params) {
							System.out.println("paramètres de newTap :" + params.toString());
							System.out.println("nom utilisé :" + params.at("name").asString());

							String error = (missingParams(params, "name"));
							if (!error.isEmpty()) return Json.object().set("error", error);

							newTap(params.at("name").asString());
							Json write = Json.object();
							write.at("write", configuredTaps);
							try {
								writeToFile(write.at("write").toString(), idsconf + "/taps/TapsConfiguration.json");
							} catch (IOException e) {
								e.printStackTrace();
							}
							return Json.object();
						}
					}, new JsonCmdHelp()
							.setDesc("DEPRECATED : Creation of a new tap description")
							.setParam("name", "name of the tap (id) ", JsonCmdHelp.STR)
							//	.setResult("nothing", JsonCmdHelp.JSON)
							.setStatus(JsonCmdHelp.STATUS_OK)
			);

			addCmd("tapNumber", new IJsonCmd() {
				@Override
				public Json exec(Json params) {
					Json j = Json.object();
					j.at("number", configuredTaps.size());
					return j;
				}
			}, new JsonCmdHelp().setDesc("DEPRECATED"));

			addCmd("deleteTap", new IJsonCmd() {
				@Override
				public Json exec(Json params) {
					deleteTap(params.at("name").asString());
					Json write = Json.object();
					write.at("write", configuredTaps);
					try {
						writeToFile(write.at("write").toString(), idsconf + "/taps/TapsConfiguration.json");
					} catch (IOException e) {
						e.printStackTrace();
					}
					return Json.object();
				}
			}, new JsonCmdHelp().setDesc("DEPRECATED"));

			addCmd("setIp", new IJsonCmd() {
				@Override
				public Json exec(Json params) {

					setIp(params.at("name").asString(), params.at("ip").asString());
					Json write = Json.object();
					write.at("write", configuredTaps);
					try {
						writeToFile(write.at("write").toString(), idsconf + "/taps/TapsConfiguration.json");
					} catch (IOException e) {
						e.printStackTrace();
					}
					return Json.object();
				}
			}, new JsonCmdHelp().setDesc("DEPRECATED"));

			addCmd("setPort", new IJsonCmd() {
				@Override
				public Json exec(Json params) {

					setPort(params.at("name").asString(), params.at("port").asInteger());
					Json write = Json.object();
					write.at("write", configuredTaps);
					try {
						writeToFile(write.at("write").toString(), idsconf + "/taps/TapsConfiguration.json");
					} catch (IOException e) {
						e.printStackTrace();
					}
					return Json.object();
				}
			}, new JsonCmdHelp().setDesc("DEPRECATED"));

			addCmd("setUsernamePassword", new IJsonCmd() {
				@Override
				public Json exec(Json params) {

					setUsernamePassword(params.at("name").asString(), params.at("username").asString(), params.at("password").asString());
					Json write = Json.object();
					write.at("write", configuredTaps);
					try {
						writeToFile(write.at("write").toString(), idsconf + "/taps/TapsConfiguration.json");
					} catch (IOException e) {
						e.printStackTrace();
					}
					return Json.object();
				}
			}, new JsonCmdHelp().setDesc("DEPRECATED"));
			addCmd("setNetworkName", new IJsonCmd() {
				@Override
				public Json exec(Json params) {
					setNetworkName(params.at("name").asString(), params.at("networkName").asString());
					Json write = Json.object();
					write.at("write", configuredTaps);
					try {
						writeToFile(write.at("write").toString(), idsconf + "/taps/TapsConfiguration.json");
					} catch (IOException e) {
						e.printStackTrace();
					}
					return Json.object();
				}
			}, new JsonCmdHelp().setDesc("DEPRECATED"));
			addCmd("getConfFromTap", (Json params) -> Json.read(getConfFromTap()), new JsonCmdHelp().setDesc("DEPRECATED"));
			//			@Override
//			public Json exec(Json params) {
//				Json result = Json.object();
//				switch(params.at("param").asString()) {
//					case "reseau":
//						result = getConfFromtap(params.at("name").asString(), "reseau");
//						break;
//					case "process":
//						result = getConfFromtap(params.at("name").asString(), "process");
//						break;
//					case "suricataconf":
//						result = getConfFromtap(params.at("name").asString(), "suricataconf");
//						break;
//					case "genrules":
//						result = getConfFromtap(params.at("name").asString(), "genrules");
//						break;
//					case "baserules":
//						result = getConfFromtap(params.at("name").asString(), "baserules");
//						break;
//				}
//				return result;
//			}
//		});

			addCmd("sendConfToTap", new IJsonCmd() {
				@Override
				public Json exec(Json params) {
					switch (params.at("param").asString()) {
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
			}, new JsonCmdHelp().setDesc("DEPRECATED"));

			addCmd("getTapConf", new IJsonCmd() {
				@Override
				public Json exec(Json params) {
					try {
						return readJsonFile(idsconf + "/taps/TapsConfiguration.json");
					} catch (IOException e) {
						e.printStackTrace();
					}
					return Json.object();
				}
			}, new JsonCmdHelp().setDesc("DEPRECATED"));

			addCmd("getTapsConfiguration", new IJsonCmd() {
				@Override
				public Json exec(Json params) {
					try {
						return readJsonFile(idsconf + "/taps/TapsConfiguration.json");
					} catch (IOException e) {
						e.printStackTrace();
					}
					return Json.object();
				}
			}, new JsonCmdHelp().setDesc("DEPRECATED"));

			addCmd("getTapState", new IJsonCmd() {
				@Override
				public Json exec(Json params) {
					Json j = Json.object();
					j.at("state", "IDLE");
					return j;
				}
			}, new JsonCmdHelp().setDesc("DEPRECATED"));

			addCmd("startTap", new IJsonCmd() {
						@Override
						public Json exec(Json params) {
							return startTaps(params.at("name").asString());
						}
					}, new JsonCmdHelp()
							.setDesc("DEPRECATED : Start tap")
							.setParam("name", "name of the tap (id) ", JsonCmdHelp.STR)
							//	.setResult("nothing", JsonCmdHelp.JSON)
							.setStatus(JsonCmdHelp.STATUS_OK)
			);

			addCmd("stopTap", new IJsonCmd() {
				@Override
				public Json exec(Json params) {
					return stopTaps(params.at("name").asString());
				}
			}, new JsonCmdHelp().setDesc("DEPRECATED"));

			addCmd("restartTap", new IJsonCmd() {
				@Override
				public Json exec(Json params) {

					stopTaps(params.at("name").asString());
					startTaps(params.at("name").asString());

					return Json.object();
				}
			}, new JsonCmdHelp().setDesc("DEPRECATED"));

			addCmd("startSuricata", new IJsonCmd() {
						@Override
						public Json exec(Json params) {
							return apiHandler.sendRequestToScanManager(HttpMethod.POST, "/suricata",
									Json.read("{\"cmd\":\"suricataStart\"}")).toJson();
						}
					}, new JsonCmdHelp()
							.setDesc("DEPRECATED : Start/restart Suricata")
							.setResult("If the (re)starting was successful", JsonCmdHelp.JSON)
							.setStatus(JsonCmdHelp.STATUS_OK)
			);

			addCmd("stopSuricata", new IJsonCmd() {
				@Override
				public Json exec(Json params) {

					return apiHandler.sendRequestToScanManager(HttpMethod.POST, "/suricata",
							Json.read("{\"cmd\":\"suricataStop\"}")).toJson();
				}
			}, new JsonCmdHelp()
					.setDesc("DEPRECATED : Stop Suricata")
					.setResult("If the stopping was successful", JsonCmdHelp.JSON)
					.setStatus(JsonCmdHelp.STATUS_OK));

			addCmd("reloadAllTapsRules", new IJsonCmd() {
				@Override
				public Json exec(Json params) {

					List<Json> allTapsOutputs = new ArrayList<>();
					boolean gotError = false;

					for (Json j : configuredTaps) {
						String output = "";

						output = apiHandler.sendRequestToScanManager(HttpMethod.POST, "/suricata",
								Json.read("{\"cmd\":\"suricataReloadRules\"}")).toString();

						Json result = Json.object();
						result.at("idname", j.at("idname").asString());
						result.at("result", reloadRulesParseOutput(output));

						if (result.at("result").has("error")) {
							gotError = true;
						}

						allTapsOutputs.add(result);
					}

					Json out = Json.object();
					out.at("result", allTapsOutputs);
					if (gotError) {
						out.at("error", true);
					}
					return out;
				}
			}, new JsonCmdHelp().setDesc("DEPRECATED"));

			addCmd("reloadRules", new IJsonCmd() {
						@Override
						public Json exec(Json params) {

							return apiHandler.sendRequestToScanManager(HttpMethod.POST, "/suricata",
									Json.read("{\"cmd\":\"suricataReloadRules\"}")).toJson();
						}
					},
					new JsonCmdHelp().setDesc("DEPRECATED : Reloads the rules of Suricata")
							.setResult("The entity as returned by CSL-Probe", IJsonCmdHelp.JSON)
							.setStatus(IJsonCmdHelp.STATUS_OK)
			);

			// Setter et getter de l'édition de Json graphique
			addCmd("getProcessJson", new IJsonCmd() {
				@Override
				public Json exec(Json params) {
					try {
						return readJsonFile(idsconf + "/taps/" + params.at("name").asString() + "/tapProcess.json");
					} catch (IOException e) {
						e.printStackTrace();
					}
					return Json.object();
				}
			}, new JsonCmdHelp().setDesc("DEPRECATED"));
			addCmd("getNetworkJson", new IJsonCmd() {
				@Override
				public Json exec(Json params) {
					try {
						return readJsonFile(idsconf + "/taps/" + params.at("name").asString() + "/tapReseau.json");
					} catch (IOException e) {
						e.printStackTrace();
					}
					return Json.object();
				}
			}, new JsonCmdHelp().setDesc("DEPRECATED"));
			addCmd("setProcessJson", new IJsonCmd() {
				@Override
				public Json exec(Json params) {
					try {
						writeToFile(params.at("conf").toString(), idsconf + "/taps/" + params.at("name").asString() + "/tapProcess.json");
					} catch (IOException e) {
						e.printStackTrace();
					}
					return Json.object();
				}
			}, new JsonCmdHelp().setDesc("DEPRECATED"));
			addCmd("setNetworkJson", new IJsonCmd() {
				@Override
				public Json exec(Json params) {
					try {
						writeToFile(params.at("conf").toString(), idsconf + "/taps/" + params.at("name").asString() + "/tapReseau.json");
					} catch (IOException e) {
						e.printStackTrace();
					}
					return Json.object();
				}
			}, new JsonCmdHelp().setDesc("DEPRECATED"));

			addCmd("getSuricataStatus", new IJsonCmd() {
						@Override
						public Json exec(Json params) {
							return apiHandler.sendRequestToScanManager(HttpMethod.POST, "/suricata", Json.read("{\"cmd\":\"suricataStatus\"}")).toJson();
						}
					},
					new JsonCmdHelp().setDesc("DEPRECATED : Get the status of Suricata")
							.setResult("The entity as returned by CSL-Probe", IJsonCmdHelp.JSON)
							.setStatus(IJsonCmdHelp.STATUS_OK)
			);
			addCmd("getSuricataConf", new IJsonCmd() {
				@Override
				public Json exec(Json params) {
					Json j = Json.object();
					try {
						j.at("conf", readFile(idsconf + "/taps/" + params.at("name").asString() + "/suricata.yaml"));
					} catch (IOException e) {
						e.printStackTrace();
					}
					return j;
				}
			}, new JsonCmdHelp().setDesc("DEPRECATED"));

			addCmd("getBaseRules", new IJsonCmd() {
						@Override
						public Json exec(Json params) {
							return apiHandler.sendRequestToScanManager(HttpMethod.POST, "/suricata",
									Json.read("{\"cmd\":\"suricataGetBaseRules\"}")).toJson();
						}
					},
					new JsonCmdHelp().setDesc("DEPRECATED : Retrieve the base rules of Suricata")
							.setResult("An object with the sid as key and the rule as value", IJsonCmdHelp.JSON)
							.setStatus(IJsonCmdHelp.STATUS_OK)
			);

			addCmd("getGenRules", new IJsonCmd() {
						@Override
						public Json exec(Json params) {
							return apiHandler.sendRequestToScanManager(HttpMethod.POST, "/suricata",
									Json.read("{\"cmd\":\"suricataGetCustomRules\"}")).toJson();
						}
					},
					new JsonCmdHelp().setDesc("DEPRECATED : Retrieve the generated rules of Suricata")
							.setResult("An object with the sid as key and the rule as value", IJsonCmdHelp.JSON)
							.setStatus(IJsonCmdHelp.STATUS_OK)
			);

			addCmd("modifyGenRules", new IJsonCmd() {
						@Override
						public Json exec(Json params) {
							return apiHandler.sendRequestToScanManager(HttpMethod.POST, "/suricata",
									Json.read("{\"cmd\":\"suricataModifyCustomRules\",\"params\":" + params + "}")).toJson();
						}
					},
					new JsonCmdHelp().setDesc("DEPRECATED : Modify the generated rules of Suricata")
							.setParam("rules", "A list of strings, each string is a rule", IJsonCmdHelp.JSON)
							.setResult("An object with the sid as keys and the rules as values", IJsonCmdHelp.JSON)
							.setStatus(IJsonCmdHelp.STATUS_OK)
			);

			addCmd("modifyBaseRules", new IJsonCmd() {
						@Override
						public Json exec(Json params) {
							return apiHandler.sendRequestToScanManager(HttpMethod.POST, "/suricata",
									Json.read("{\"cmd\":\"suricataModifyBaseRules\",\"params\":" + params + "}")).toJson();
						}
					},
					new JsonCmdHelp().setDesc("DEPRECATED : Modify the base rules of Suricata")
							.setParam("rules", "A list of strings, each string is a rule", IJsonCmdHelp.JSON)
							.setResult("An object with the sid as keys and the rules as values", IJsonCmdHelp.JSON)
							.setStatus(IJsonCmdHelp.STATUS_OK)
			);

			addCmd("setSuricataConf", new IJsonCmd() {
				@Override
				public Json exec(Json params) {
					try {
						writeToFile(params.at("conf").asString(), idsconf + "/taps/" + params.at("name").asString() + "/suricata.yaml");
					} catch (IOException e) {
						e.printStackTrace();
					}
					return Json.object();
				}
			}, new JsonCmdHelp().setDesc("DEPRECATED"));
			addCmd("setBaseRules", new IJsonCmd() {
						@Override
						public Json exec(Json params) {
							return apiHandler.sendRequestToScanManager(HttpMethod.POST, "/suricata",
									Json.read("{\"cmd\":\"suricataAddBaseRules\",\"params\":" + params.toString() + "}")).toJson();
						}
					},
					new JsonCmdHelp().setDesc("DEPRECATED : Set the base rules of Suricata")
							.setParam("rules", "A list of strings, each string is a rule", IJsonCmdHelp.JSON)
							.setResult("The entity as returned by CSL-Probe, with the new updated rules", IJsonCmdHelp.JSON)
							.setStatus(IJsonCmdHelp.STATUS_OK));
			addCmd("setGeneratedRules", new IJsonCmd() {
						@Override
						public Json exec(Json params) {
							return apiHandler.sendRequestToScanManager(HttpMethod.POST, "/suricata",
									Json.read("{\"cmd\":\"suricataAddCustomRules\",\"params\":" + params.toString() + "}")).toJson();
						}
					},
					new JsonCmdHelp().setDesc("DEPRECATED : Set the generated rules of Suricata")
							.setParam("rules", "A list of strings, each string is a rule", IJsonCmdHelp.JSON)
							.setResult("The entity as returned by CSL-Probe, with the new updated rules", IJsonCmdHelp.JSON)
							.setStatus(IJsonCmdHelp.STATUS_OK));

			/*
			 * Update suricata rules in all taps
			 */
			addCmd("updateAllTapsSuricataRules", new IJsonCmd() {
				@Override
				public Json exec(Json params) {
					Json response = null;
					for (Json tapConfig : configuredTaps) {
						response = apiHandler.sendRequestToScanManager(HttpMethod.POST, "/suricata",
								Json.read("{\"cmd\":\"suricataAddCustomRules\",\"params\":" + params.toString() + "}")).toJson();
					}
					return response;
				}
			}, new JsonCmdHelp().setDesc("DEPRECATED"));

			addCmd("getSuricataLogs", new IJsonCmd() {
				@Override
				public Json exec(Json params) {
					Json result = Json.object();
					result = getSuricataLogs(params.at("name").asString());
					String content = "";
					try {
						content = readFile(idsconf + "/taps/" + params.at("name").asString() + "/suricataLogs.txt");
					} catch (IOException e) {
						e.printStackTrace();
					}
					result.at("logs", content);
					return result;
				}
			}, new JsonCmdHelp().setDesc("DEPRECATED"));
			addCmd("clearSuricataLogs", new IJsonCmd() {
				@Override
				public Json exec(Json params) {
					clearLogs(params.at("name").asString());
					Json result = Json.object();
					try {
						writeToFile("", idsconf + "/taps/" + params.at("name").asString() + "/suricataLogs.txt");
					} catch (IOException e) {
						e.printStackTrace();
					}
					result.at("logs", "");
					return result;
				}
			}, new JsonCmdHelp().setDesc("DEPRECATED"));
			addCmd("sendIncludes", new IJsonCmd() {
				@Override
				public Json exec(Json params) {
					sendIncludes(params.at("name").asString());
					return Json.object();
				}
			}, new JsonCmdHelp().setDesc("DEPRECATED"));
			addCmd("setInclude", new IJsonCmd() {
				@Override
				public Json exec(Json params) {

					ArrayList<Integer> includeList = new ArrayList<>();
					for (Json j : params.at("include").asJsonList()) {
						if (j.at("checked").asBoolean()) {
							includeList.add(j.at("id").asInteger());
						}
					}
					for (Json good : configuredTaps) {
						if (good.at("idname").asString().contentEquals(params.at("name").asString())) {
							good.atDel("includes");
							good.at("includes", includeList);
						}
					}
					Json write = Json.object();
					write.at("write", configuredTaps);
					try {
						writeToFile(write.at("write").toString(), idsconf + "/taps/TapsConfiguration.json");
					} catch (IOException e) {
						e.printStackTrace();
					}
					return Json.object();
				}
			}, new JsonCmdHelp().setDesc("DEPRECATED"));
			addCmd("getIncludes", new IJsonCmd() {
						@Override
						public Json exec(Json params) {
							try {
								Json includes = Json.object();
								Json taps = readJsonFile(idsconf + "/taps/TapsConfiguration.json");

								for (Json k : taps.asJsonList()) {
									Json includesRaw = Json.object();
									ArrayList<Json> includesRawClone = new ArrayList<Json>();
									includesRaw = readJsonFile(idsconf + "/taps/includesConfiguration.json");
									for (Json j : includesRaw.asJsonList()) {
										for (Json r : k.at("includes").asJsonList()) {
											if (j.at("id").asInteger() == r.asInteger()) {
												j.at("checked", true);
											}

										}
										includesRawClone.add(j);
									}
									for (Json j : includesRawClone) {
										if (JsonUtil.getJson(j, "checked") == null)
											j.at("checked", false);
									}
									includes.at(k.at("idname").asString(), includesRawClone);
								}


								return includes;
							} catch (IOException e1) {
								e1.printStackTrace();
							}
							return Json.object();
						}
					}, new JsonCmdHelp().setDesc("DEPRECATED")
			);
		}

		System.out.println("SSH commands operationnal");

		return true;
	}

	/**
	 * Sends the query to reload all taps rules (non-blocking).
	 * @return the output of every reload
     */
	private @NotNull Json reloadAllTapsRules() {
		List<Json> allTapsOutputs = new ArrayList<>();
		boolean gotError = false;

		for (Tap tap : taps.values()) {
			String output = tap.sendCmd("/suricata",Json.read("{\"cmd\":\"suricataReloadRulesNonBlocking\"}")).toString();

			Json result = Json.object();
			result.at("idname",tap.getName());
			result.at("result", reloadRulesParseOutput(output));

			if (result.at("result").has("error")) {
				gotError = true;
			}

			allTapsOutputs.add(result);
		}

		Json out = Json.object();
		out.at("result", allTapsOutputs);
		if (gotError) {
			out.at("error", true);
		}
		return out;
	}
}
