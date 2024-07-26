package main.services;

import com.csl.core.CSLContext;
import com.csl.core.Config;
import com.jcraft.jsch.JSchException;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import main.extensions.ScanActif;
import main.extensions.SshUtils;
import main.extensions.Utils;
import org.apache.commons.net.util.SubnetUtils;

import java.io.*;
import java.util.ArrayList;

public class NmapServices extends Service {
	static String idsconf;
	static boolean debugMode = false;
	static boolean logMode = false;
	static String debugPath = "";
	static String logPath = "";

	private static final String GET_TAP_NETWORK = "ip -o -f inet addr show $(cat ~/nmapInterface.txt) | awk '/scope global/ {print $4}'";

	/**
	 * Default constructor of the Nmap service.
	 */
	public NmapServices() {
		this("nmap",
				"nmap description",
				"nmap_service");
	}

	/**
	 * Generic constructor of the Nmap service.
	 */
	public NmapServices(String name, String description, String configFileSectionName) {
		super(name,description,configFileSectionName);
	}

	static public void lauchNmap(Json params, Json jConfig) {
		System.out.println("launchNmap:"+params);
		System.out.println("launchNmap:"+jConfig);

		Json resultat = Json.object();
		ScanActif sa = new ScanActif(false, true,params.at("list").asJsonList().get(0).asString(), debugMode, logMode, debugPath, logPath);

		Json result = sa.getIp(params);
		result = result.at("result");
		ArrayList<Json> cur = (ArrayList<Json>) result.asJsonList();
		Json machines = Json.object();
		machines.at("list",cur.get(0).at("machines"));
		System.out.println(machines);
		System.out.println(jConfig);

		machines.at("tap",params.at("list").asJsonList().get(0));
		scanDevice(machines, jConfig);

	}

	static public void lauchNmap(Json params, Config.CSLNmapService config) {
//		System.out.println("launchNmap:"+params);
//		System.out.println("launchNmap:"+config);

		Json resultat = Json.object();
		ScanActif sa = new ScanActif(false, true,params.at("list").asJsonList().get(0).asString(), debugMode, logMode, debugPath, logPath);

		Json result = sa.getIp(params);
		result = result.at("result");
		ArrayList<Json> cur = (ArrayList<Json>) result.asJsonList();
		Json machines = Json.object();
		machines.at("list",cur.get(0).at("machines"));
		System.out.println(machines);
//		System.out.println(jConfig);

		machines.at("tap",params.at("list").asJsonList().get(0));
		scanDevice(machines, config);

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

	static public Json scanDevice(Json params, Json jConfig) {
		System.out.println("launchNmap:"+params);
		System.out.println("launchNmap:"+jConfig);


		ScanActif sa = new ScanActif(false, true, params.at("tap").asString(), debugMode, logMode, debugPath, logPath);
		Json result = sa.getIpInfo (params, new ArrayList<Json>());


		// Insertion dans la base de données
		ArrayList<Json> resultList = (ArrayList<Json>) result.at("devices").asJsonList();
		for(Json j : resultList) {

			String ip = j.at("ip").asString();
			j.delAt("ip");
			Json mac = j.at("mac");
			j.delAt("mac");
			System.out.println("insertion de "+ ip);
			System.out.println(j);
			j.at("related_tap" , params.at("tap"));
			Utils.addDevice(ip, j);
			Utils.setDeviceProp(ip, "macs" , mac);

			j.at("lastScan",System.currentTimeMillis());
			Utils.setDeviceProp(ip, "props" , j);

		}
		return result;
	}

	static public Json scanDevice(Json params, Config.CSLNmapService config) {
		System.out.println("launchNmap:"+params);
//		System.out.println("launchNmap:"+jConfig);


		ScanActif sa = new ScanActif(false, true, params.at("tap").asString(), debugMode, logMode, debugPath, logPath);
		Json result = sa.getIpInfo (params, new ArrayList<Json>());


		// Insertion dans la base de données
		ArrayList<Json> resultList = (ArrayList<Json>) result.at("devices").asJsonList();
		for(Json j : resultList) {

			String ip = j.at("ip").asString();
			j.delAt("ip");
			Json mac = j.at("mac");
			j.delAt("mac");
			System.out.println("insertion de "+ ip);
			System.out.println(j);
			j.at("related_tap" , params.at("tap"));
			Utils.addDevice(ip, j);
			Utils.setDeviceProp(ip, "macs" , mac);

			j.at("lastScan",System.currentTimeMillis());
			Utils.setDeviceProp(ip, "props" , j);

		}
		return result;
	}

	/**
	 * Initialization of the Nmap commands
	 * @param jConfig the configuration section of the configuration file
	 * @param cslDir the CSL directory
	 * @return true if the initialization happened with no problems, false otherwise.
	 */
	@Override
	public boolean init(Json jConfig, String cslDir) {
		Config.CSLNmapService config = Config.instance.NmapService;
		System.out.println("--- Initialisation des services Nmap ---");
//		NmapServices.debugMode = jConfig.at("debug_mode").asBoolean();
		NmapServices.debugMode = config.getDebugMode();
//		NmapServices.logMode = jConfig.at("log_mode").asBoolean();
		NmapServices.logMode = config.getLogMode();
//		NmapServices.logPath = jConfig.at("log_dir").asString();
		NmapServices.logPath = config.getLogDir();
//		NmapServices.debugPath = jConfig.at("debug_dir").asString();
		NmapServices.debugPath = config.getDebugDir();

		// Fonction mise ici en attendant d'avoir une vrai fonction backend permettant de faire la meme chose (compter le nombre de liens en tout)
		////////////////////////////////////////////////////////////////////////////////////////////////////
		addCmd("getLinksNumber", new IJsonCmd() {
			
			@Override
			public Json exec(Json params) {
				Json j =null;
				try {
					j = readJsonFile("./datafile/devices.json");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Json result = Json.object();
				if(j.at("links") != null) {
					result.at("number",j.at("links").asJsonList().size());
				}
				else {
					result.at("number",0);
				}

				return result;
			}
		});	
		/////////////////////////////////////////////////////////////////////////////////////////////////////
		/*
		 * Permet de lancer nmap sur le sous réseau donné et de remplir le fichier devices.json en fonction du résultat de nmap.
		 * Paramètre : un json de la forme {"list":["<ip1>"]}
		 */
		addCmd("launchNmap", new IJsonCmd() {
			
			@Override
			public Json exec(Json params) {
//				lauchNmap(params,jConfig);
				lauchNmap(params,config);
				return Json.object();
			}
		});
		
		addCmd("scanDevice", new IJsonCmd() {
			
			@Override
			public Json exec(Json params) {
				String ip = params.at("ip").asString();
				String tapName = JsonUtil.getStringFromJson(params, "props/related_tap", null);
				
				
				// Recherche d'un tap compatible si jamais le champ related_tap est vide
				// On interroge chaque tap pour récupérer l'adresse IP de leur interface reseau exterieur (pas forcement celle avec laquelle ils communiquent avec csl, d'ou la demande)
				// On récupère l'ip et le netmask et on vérifie par rapport à l'ip à scanner.
				// Si c'est compatible, on prend celui là, sinon le scan est impossible
				if(tapName == null) {
					boolean test = false;
					idsconf = CSLContext.instance.getCslConfDir(); //getIdsRunner().getIdsParams().getIdsModelDir();
					Json taps = Json.object();
					try {
						taps = readJsonFile(idsconf+"/taps/TapsConfiguration.json");
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					for(Json j : taps.asJsonList()) {
						boolean accessible = true;
						String result = "";
						try {
							int port = 22;
							try {
								port = j.at("port").asInteger();
							} catch (NullPointerException e) {
								System.out.println("Using default SSH port (22)");
							}
							SshUtils ssh = new SshUtils(j.at("username").asString(),j.at("password").asString(),j.at("ip").asString(),port );
							// result = ssh.remoteExec("ip -o -f inet addr show $(cat /home/"+j.at("username").asString()+"/nmapInterface.txt) | awk '/scope global/ {print $4}'");
							 result = ssh.remoteExec(GET_TAP_NETWORK);
						} catch (IOException e) {
							accessible = false;
							e.printStackTrace();
						} catch (JSchException e) {
							accessible = false;
							System.out.println("Le tap "+j.at("ip").asString()+" est inacessible pour le moment");
						}
						if(accessible) {
							result = result.substring(0, result.length()-2);							
							SubnetUtils subnet = new SubnetUtils(result);
							test = subnet.getInfo().isInRange(ip);	
						}			
						if(test) {
							tapName = j.at("idname").asString();
							break;
						}
					}
					if(!test) {
						System.out.println("Pas de tap trouvé, requête impossible");
						return Json.object().at("error", "Requête impossible, pas de tap trouvé");
					}
					else {
						System.out.println("Tap trouvé pour "+ip+", il s'agit de "+tapName);
					}
				}
				
				Json param = Json.object();
				ArrayList<String> tmp = new ArrayList<String>();
				tmp.add(ip);
				param.at("list",tmp);
				param.at("tap", tapName);
				System.out.println("Scanning device");
				System.out.println(param);
				return	scanDevice(param,jConfig);

			}
		});
		return true;
	}
}
