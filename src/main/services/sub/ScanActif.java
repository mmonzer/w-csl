package main.services.sub;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.nmap4j_csl.Nmap4j;
import org.nmap4j_csl.core.nmap.NMapExecutionException;
import org.nmap4j_csl.core.nmap.NMapInitializationException;

import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;

public class ScanActif {
	String nmapLocation; 
	boolean debug;
	boolean verbose;
	String nmapScriptLocation; 
	ArrayList<String> networks;
	String ipForwardTestParameter = "";
	static Semaphore semaphore = new Semaphore(1);
	
	/**
	 * Constructeur de la classe ScanActif, la classe contient de méthodes permettant de scanner des sous réseau et des machines avec Nmap
	 * @param debug Active ou non le débug
	 * @param verbose Active ou non la verbose
	 * @param nmapLocation est le chemin absolu vers l'exécutable nmap
	 * @param ipForwardTestParameter Une adresse IP accessible sur le sous réseau de la machine testée (comme par exemple 8.8.8.8 si la machine est connectée à internet)
	 * @param nmapScriptLocation est le chemin absolu vers le dossier de scripts NSE nmap
	 */
	public ScanActif(boolean debug, boolean verbose, String nmapLocation, String ipForwardTestParameter, String nmapScriptLocation) {
		try {
			semaphore.acquire();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
        this.debug = debug; 
        this.verbose = verbose; 
		this.nmapLocation = nmapLocation;
		this.ipForwardTestParameter = ipForwardTestParameter;
		this.nmapScriptLocation = nmapScriptLocation;
		networks = new ArrayList<String>();


		print("------------------------------------",2);
		print("-------------Paramètres-------------",2);
		print("------------------------------------",2);
        print("Verbose : "+verbose,2);
        print("Débug : "+debug,2);
        print("Nmap location : "+nmapLocation,2);
        print("Nmap script location"+ipForwardTestParameter,2);
        print("Ip forward test parameter"+nmapScriptLocation,2);


		
	}
	
	/**
	 * Permet de récupérer les IP des machines accessibles sur le réseau
	 * @param networksList un Json contenant un tableau d'addresses IP dans le champ "list". 
	 * Les adresses doivent contenir un netmask au format /xx. 
	 * Exemple : {"list":["192.168.1.0/24"]}
	 * @return Un json contenant la liste des adresses IP étant accessibles pour chaque réseau scanné. 
	 * Un tableau Json est présent dans le champ "result", et chaque element de ce tableau est object json contenant un champ "network" contenant l'adresse du réseau scanné, 
	 * et un champ "machines" contenant un tableau de String avec les différentes IP détectées sur ce sous réseau
	 * Exemple : {"result":[{"machines":["192.168.1.1","192.168.1.18"],"network":"192.168.1.0/24"}]}
	 */
	public Json getIp(Json networksList) {
		ArrayList<Json> netList = (ArrayList<Json>) networksList.at("list").asJsonList();
		for(Json cur : netList) {
			this.networks.add(cur.asString());
		}
		ArrayList<Json> resultPart = new ArrayList<Json>();
		for(String cur : this.networks) {
			resultPart.add(scanNetwork(cur));
		}
		Json result = Json.object();
		result.at("result",resultPart);
		networks.clear();
		return result;
	}
	
	/**
	 * Permet de récupérer les IP des machines accessibles sur le réseau
	 * @param networksList une ArrayList de String contenant des adresses de réseau à scanner.
	 * Les adresses doit contenir un netmask au format /xx. 
	 * Exemple : 192.168.1.0/24  
	 * @return Un json contenant la liste des adresses IP étant accessibles pour chaque réseau scanné. 
	 * Un tableau Json est présent dans le champ "result", et chaque element de ce tableau est object json contenant un champ "network" contenant l'adresse du réseau scanné, 
	 * et un champ "machines" contenant un tableau de String avec les différentes IP détectées sur ce sous réseau
	 * Exemple : {"result":[{"machines":["192.168.1.1","192.168.1.18"],"network":"192.168.1.0/24"}]}
	 */	
	public Json getIp(ArrayList<String> networksList) {
		for(String cur : networksList) {
			this.networks.add(cur);
		}
		ArrayList<Json> resultPart = new ArrayList<Json>();
		for(String cur : this.networks) {
			resultPart.add(scanNetwork(cur));
		}
		Json result = Json.object();
		result.at("result",resultPart);
		networks.clear();

		return result;
	}	
	
	/**
	 * Permet de récupérer les IP des machines accessibles sur le réseau
	 * @param network Une String contenant une adresse d'un sous réseau à scanner 
	 * L'adresses doit contenir un netmask au format /xx. 
	 * Exemple : 192.168.1.0/24
	 * @return Un json contenant la liste des adresses IP étant accessibles pour chaque réseau scanné. 
	 * Un tableau Json est présent dans le champ "result", et chaque element de ce tableau est object json contenant un champ "network" contenant l'adresse du réseau scanné, 
	 * et un champ "machines" contenant un tableau de String avec les différentes IP détectées sur ce sous réseau
	 * Exemple : {"result":[{"machines":["192.168.1.1","192.168.1.18"],"network":"192.168.1.0/24"}]}
	 */	
	public Json getIp(String network) {
		this.networks.add(network);
		ArrayList<Json> resultPart = new ArrayList<Json>();
		for(String cur : this.networks) {
			resultPart.add(scanNetwork(cur));
		}
		Json result = Json.object();
		result.at("result",resultPart);
		networks.clear();
		return result;
	}	
	
	
	
	/**
	 * Scanne les sous-réseaux et renvoie un Json contenant un tableau Json d'ip
	 */
	private Json scanNetwork(String ip) {	
		Json result = Json.object();
		result.at("network",ip);
		
		ArrayList<String> ipList = new ArrayList<String>();
		
		String currentNetwork =ip;
		print("Scan de "+currentNetwork,2);
		try {	
			// Exécution de nmap
			Nmap4j nmap4j = new Nmap4j(nmapLocation);
			nmap4j.addFlags("-sP");
			nmap4j.includeHosts(currentNetwork);
			nmap4j.execute() ;
			String output ="Erreur",errors="Erreur";
			if( !nmap4j.hasError() ) {
				output = nmap4j.getOutput() ;
				errors = nmap4j.getExecutionResults().getErrors() ;
			}
			String rawOutput = XMLtoJSON(output);
			// Parsing de l'output json
			Json currentReponse = Json.object();
			currentReponse = Json.read(rawOutput);
			ArrayList<Json> hostList = (ArrayList<Json>) JsonUtil.getJson(currentReponse,"nmaprun/host").asJsonList();
			print("Hôtes trouvés :",2);
			for(Json currentHost : hostList) {
				Json newMachine = currentHost.get("address");
				if(newMachine.isArray()) {
					ArrayList<Json> addresses = (ArrayList<Json>) newMachine.asJsonList();
					for(Json j : addresses) {
						if(j.get("addrtype").asString().equals("ipv4")) {
							ipList.add(JsonUtil.getStringFromJson(j,"addr","Error"));
							print("    -"+JsonUtil.getStringFromJson(j,"addr","Error"),2);
						}
					}
				}
				else {
					ipList.add(JsonUtil.getStringFromJson(newMachine,"addr","Error"));
					print("    -"+JsonUtil.getStringFromJson(newMachine,"addr","Error"),2);

	
				}					
			}
		} catch (Exception e) {
			System.err.println("Erreur lors du Scan du réseau "+currentNetwork+ " (Résultats ignorés)");
			e.printStackTrace();
		}
		result.at("machines",ipList);
		return result;
		
	}
	
	
	/**
	 * Scanne les ip données, et ajoute les informations trouvées à la liste devices
	 * @param ipListJson un Json contenant un tableau dans le champ 'list'. Chaque element de ce tableau est une ip à scanner.
	 * @param devices est la liste des machines déjà existantes, les informations trouvées par ce scan seront ajoutées
	 * @return retourne un objet json contenant deux champs :
	 * devices : le champ devices passé en parametres, completé
	 * ip : tableau Json affichant toute les ip scannées pendant ce scan.
	 * Exemple : {"devices":[{"role":"none","softwares":[{"name":"http","cpe":"cpe:/a:python:python:2.7.16","portid":8080,"version":"Werkzeug httpd 1.0.1"}],"os_cpe":"cpe:/o:linux:linux_kernel:3","ip":"192.168.1.18","os_version":"3.X","name":"antodeb.home","os_name":"\"Linux 3.7 - 3.10\"","ports":[22,53,8080],"mac":"none","manufacturer":"Linux"}],"ip":["192.168.1.18"]}
	 */
	public Json getIpInfo(Json ipListJson, ArrayList<Json> devices) {
		ArrayList<Json> ipList = (ArrayList<Json>) ipListJson.at("list").asJsonList();
		ArrayList<String> machines = new ArrayList<String>();		
		Json result = Json.object();
		for(Json ip : ipList) {
			machines.add(ip.asString());
		}
		result.at("ip", machines);
		devices = scanMachines(machines, devices);
		result.at("devices", devices);
		return result;
	}
	/**
	 * Scanne les ip données, et ajoute les informations trouvées à la liste devices
	 * @param ipList Une liste d'IP à scanner sous forme de string. Chaque element est une ip à scanner.
	 * @param devices est la liste des machines déjà existantes, les informations trouvées par ce scan seront ajoutées
	 * @return retourne un objet json contenant deux champs :
	 * devices : le champ devices passé en parametres, completé
	 * ip : tableau Json affichant toute les ip scannées pendant ce scan.
	 * Exemple : {"devices":[{"role":"none","softwares":[{"name":"http","cpe":"cpe:/a:python:python:2.7.16","portid":8080,"version":"Werkzeug httpd 1.0.1"}],"os_cpe":"cpe:/o:linux:linux_kernel:3","ip":"192.168.1.18","os_version":"3.X","name":"antodeb.home","os_name":"\"Linux 3.7 - 3.10\"","ports":[22,53,8080],"mac":"none","manufacturer":"Linux"}],"ip":["192.168.1.18"]}
	 */
	public Json getIpInfo(ArrayList<String> ipList, ArrayList<Json> devices) {
		ArrayList<String> machines = ipList;
		Json result = Json.object();
		result.at("ip", machines);
		devices = scanMachines(machines, devices);
		result.at("devices", devices);
		return result;
	}	
	/**
	 * Scanne les ip données, et ajoute les informations trouvées à la liste devices
	 * @param ip Une ip à scanner.
	 * @param devices est la liste des machines déjà existantes, les informations trouvées par ce scan seront ajoutées
	 * @return retourne un objet json contenant deux champs :
	 * devices : le champ devices passé en parametres, completé
	 * ip : tableau Json affichant toute les ip scannées pendant ce scan.
	 * Exemple : {"devices":[{"role":"none","softwares":[{"name":"http","cpe":"cpe:/a:python:python:2.7.16","portid":8080,"version":"Werkzeug httpd 1.0.1"}],"os_cpe":"cpe:/o:linux:linux_kernel:3","ip":"192.168.1.18","os_version":"3.X","name":"antodeb.home","os_name":"\"Linux 3.7 - 3.10\"","ports":[22,53,8080],"mac":"none","manufacturer":"Linux"}],"ip":["192.168.1.18"]}
	 */
	public Json getIpInfo(String ip, ArrayList<Json> devices) {
		ArrayList<String> machines = new ArrayList<String>();
		machines.add(ip);
		Json result = Json.object();
		result.at("ip", machines);
		devices = scanMachines(machines, devices);
		result.at("devices", devices);
		return result;
	}	
	
	/**
	 * Scanne toutes les machines de la liste, une par une et renvoie une liste d'objects json les décrivant
	 * @return le tableau de JSON
	 */
	private ArrayList<Json> scanMachines(ArrayList<String> machines, ArrayList<Json> devices){
		for(String currentMachine : machines) {
			try {
				Json scannedResult = scanMachine(currentMachine);
				print("-----------------------",1);
				print("On a trouvé ces informations sur la machine"+currentMachine+" : ",1);
				print(scannedResult.toString(),1);
				print("-----------------------",1);
				devices = addAndMerge(scannedResult, devices);
			} catch (Exception e) {
				System.err.println("Erreur lors du traitement de la machine "+currentMachine+" (Résultat ignoré)");
				e.printStackTrace();
			}
		}	
		return devices;
	}
	

	/**
	 * Transforme une chaine XML en chaine JSON
	 * @param XMLs la chaine xml
	 * @return la chaine json
	 */
	private String XMLtoJSON(String XMLs) {
		String jsonPrettyPrintString ="";
        try {
            JSONObject xmlJSONObj = XML.toJSONObject(XMLs);
            jsonPrettyPrintString = xmlJSONObj.toString(4);
        } catch (JSONException je) {
            print(je.toString(),2);
        }	
        return jsonPrettyPrintString;
	}
	

	
	/**
	 * Récupère le meilleur OS (celui ayant la meilleure accuracy) 
	 * @param os Champ JSON spécifié dans nmap
	 * @return Le nom du meilleur OS
	 */
	private HashMap<String,String> checkOS(Json os) {
		HashMap<String,String> currentBestInfos = new HashMap<String,String>();
		String currentBestName;
		int currentBestAccuracy;
		Json osMatch = os.get("osmatch");
		if(osMatch.isArray()) {
			ArrayList<Json> osses = (ArrayList<Json>) osMatch.asJsonList();
			currentBestAccuracy = osses.get(0).get("accuracy").asInteger();
			currentBestName = osses.get(0).get("name").toString();
			currentBestInfos = getBestInfo(osses.get(0).get("osclass"));
			
			for(Json j : osses) {
				int current = j.get("accuracy").asInteger();
				if(current > currentBestAccuracy) {
					currentBestAccuracy = current;
					currentBestName = j.get("name").toString();
					currentBestInfos = getBestInfo(JsonUtil.getJson(j, "osclass"));
				}
			}
		}
		else {
			currentBestName = osMatch.get("name").toString();
			currentBestInfos = getBestInfo(JsonUtil.getJson(osMatch, "osclass"));			
		}
		currentBestInfos.put("name", currentBestName);
		return currentBestInfos;
	}
	
	/**
	 * Récupère le meilleur nom pour le meilleur OS
	 * @param j Le champ json dans lequel choisir 
	 * @return le meilleur nom (fonctionne avec checkOS(Json os)
	 */
	private HashMap<String,String> getBestInfo(Json j) {
		HashMap<String,String> result = new HashMap<String,String>();
		String currentBestVersion = "", currentBestCpe = "", currentBestVendor = "";
		int currentBestAccuracy;
		if(j.isArray()){
			ArrayList<Json> j2 = (ArrayList<Json>) j.asJsonList();
			currentBestAccuracy = j2.get(0).get("accuracy").asInteger();
			currentBestVersion = JsonUtil.getStringFromJson(j2.get(0), "osgen","none");
			currentBestCpe = JsonUtil.getStringFromJson(j2.get(0), "cpe","none");
			currentBestVendor = JsonUtil.getStringFromJson(j2.get(0), "vendor","none");
			
			for(Json j3 : j2) {
				int current = j3.get("accuracy").asInteger();
				if(current > currentBestAccuracy) {
					currentBestAccuracy = current;
					currentBestVersion = JsonUtil.getStringFromJson(j3, "osgen","none");
					currentBestCpe = JsonUtil.getStringFromJson(j3, "cpe","none");
					currentBestVendor = JsonUtil.getStringFromJson(j3, "vendor","none");
				}
			}
		}
		else {
			currentBestVersion = JsonUtil.getStringFromJson(j, "osgen","none");
			currentBestCpe = JsonUtil.getStringFromJson(j, "cpe","none");
			currentBestVendor = JsonUtil.getStringFromJson(j, "vendor","none");
		}
		result.put("version",currentBestVersion);
		result.put("cpe",currentBestCpe);
		result.put("vendor",currentBestVendor);		
		print("C'est "+currentBestVersion,1);
		print("-----------------------",1);

		return result;
	}
	
	
	/**
	 * Renvoie un object json composé des trois champs donnés en parametres
	 * @param name noms
	 * @param version version
	 * @param port port
	 * @return l'object json associé
	 */
	private Json newJsonService(String name, String version, int port, Json cpe) {
		Json result = Json.object();
		result.at("name", name);
		result.at("version", version);
		result.at("portid", port);
		result.at("cpe", cpe);
	
		return result;
	}
	
	/**
	 * Scanne une machine et renvoie un object json la décrivant 
	 * @param ip l'ip de la machine
	 * @return l'object JSON
	 */
	private Json scanMachine(String ip) {
		Json result = Json.object();
		try {
			// -O -> OS probe
			// sudo nmap -sn 192.168.1.0/24 --script ip-forwarding --script-args='target=www.google.com'
			// nmap4j.addFlags(" --script ip-forwarding.nse --script-args='target=www.google.com' ");
			//nmap4j.addFlags(" --script /usr/share/nmap/scripts/ip-forwarding.nse --script-args='target=www.google.com' ");
			//nmap4j.includeHosts("195.83.81.0/24");		
			
			// Exécution de nmap
			Nmap4j nmap4j = new Nmap4j(nmapLocation);
			print("Scan de "+ip,2);
			nmap4j.includeHosts(ip);
			nmap4j.addFlags(" -A --osscan-guess --osscan-limit");
			nmap4j.execute() ;
			String output ="Erreur";
			if( !nmap4j.hasError() ) {
				output = nmap4j.getOutput() ;
			}
			else {
				print("Exécution impossible",2);
				print(nmap4j.getExecutionResults().getErrors(),2) ;
			}
			String rawOutput = XMLtoJSON(output);
			print("-----------------------",1);
			print("Output nmap pour "+ip+" : ",1);
			print(rawOutput,1);
			print("-----------------------",1);

			// Parsing de la réponse nmap
			Json currentReponse = Json.object();
			currentReponse = Json.read(rawOutput);

			
			try {
				result.at("name", JsonUtil.getStringFromJson(currentReponse,"nmaprun/host/hostnames/hostname/name","none"));
			} catch (Exception e) {
				System.err.println("Nom de la machine introuvable");
			}
			boolean ipFound = false, macFound = false;
			try {
				Json check = currentReponse.get("nmaprun").get("host").get("address");
				if(check.isArray()) {
					ArrayList<Json> addresses = (ArrayList<Json>) currentReponse.get("nmaprun").get("host").get("address").asJsonList();
					for(Json j : addresses) {
						if(j.get("addrtype").asString().equals("ipv4")) {
							result.at("ip", JsonUtil.getStringFromJson(j,"addr",ip));
							ipFound = true;
						}
						else if(j.get("addrtype").asString().equals("mac")){
							result.at("mac", JsonUtil.getStringFromJson(j,"addr","none"));
							macFound = true;

		
						}
					}
				}
				else {
					result.at("ip", JsonUtil.getStringFromJson(currentReponse,"nmaprun/host/address/addr","none"));
	
				}
				if(!ipFound) {
					result.at("ip", ip);
				}
				if(!macFound) {
					result.at("mac", "none");
				}				
			} catch (Exception e) {
				if(!ipFound) {
					result.at("ip", ip);
				}
				if(!macFound) {
					result.at("mac", "none");
				}
				System.err.println("Adresses IP ou MAC de la machine introuvable");
			}	
			
			if(hasIpForwarding(ip)) {
				result.at("role", "router");
			}
			else {
				result.at("role", "none");

			}

			try {
				Json currentPorts =   JsonUtil.getJson(currentReponse, "nmaprun/host/ports");
				ArrayList<Integer> portsNew = new ArrayList<Integer>();
				ArrayList<Json> servicesNew = new ArrayList<Json>();			
				if(currentPorts.get("port").isArray()) {
					ArrayList<Json> ports = (ArrayList<Json>) currentPorts.get("port").asJsonList();
					for(Json current : ports) {
						servicesNew.add(newJsonService(JsonUtil.getStringFromJson(current,"service/name","none"),
								JsonUtil.getStringFromJson(current,"service/product","")+" "+JsonUtil.getStringFromJson(current,"service/version","none"),
								JsonUtil.getIntFromJson(current,"portid",-1),
								JsonUtil.getJson(current,"service/cpe") 
						));
						portsNew.add(JsonUtil.getIntFromJson(current,"portid",-1));
					}
				}
				else {
					Json port2 = currentPorts.get("port");
					servicesNew.add(newJsonService(JsonUtil.getStringFromJson(port2,"service/name","none"),
							JsonUtil.getStringFromJson(port2,"service/product","")+" "+JsonUtil.getStringFromJson(port2,"service/version","none"),
							JsonUtil.getIntFromJson(port2,"portid",-1),
							JsonUtil.getJson(port2,"service/cpe") 
					));
					portsNew.add(JsonUtil.getIntFromJson(port2,"portid",-1));
				}
				result.at("softwares",servicesNew);
				result.at("ports", portsNew);
			} catch (Exception e) {
				System.err.println("Impossible de trouver des ports et services sur la machine");
			}
			
			try {
				HashMap<String,String> os = checkOS(JsonUtil.getJson(currentReponse,"nmaprun/host/os"));
				result.at("os_name", os.get("name"));
				result.at("os_version", os.get("version"));
				result.at("os_cpe",os.get("cpe"));
				result.at("manufacturer",os.get("vendor"));

			} catch (Exception e) {
				System.err.println("Impossible des informations sur l'OS");
			}
		} catch (NMapInitializationException e) {
			e.printStackTrace();
		} catch (NMapExecutionException e) {
			e.printStackTrace();
		}

		return result;
	}
	
	/**
	 * Vérifie si la machine de l'ip passée en paramètres a l'ip forwarding d'activé 
	 * @param ip l'ip à vérifier
	 * @return true si il a l'ip forwarding, false sinon
	 */
	private boolean hasIpForwarding(String ip) {
		print("On tente de savoir si "+ip+" a l'ip forwarding avec ce paramètre : "+ipForwardTestParameter,1);
		try {
			// -O -> OS probe
			// sudo nmap -sn 192.168.1.0/24 --script ip-forwarding --script-args='target=www.google.com'
			// nmap4j.addFlags(" --script ip-forwarding.nse --script-args='target=www.google.com' ");
			//nmap4j.addFlags(" --script /usr/share/nmap/scripts/ip-forwarding.nse --script-args='target=www.google.com' ");
			//nmap4j.includeHosts("195.83.81.0/24");		
			
			// Exécution de nmap
			Nmap4j nmap4j = new Nmap4j(nmapLocation);
			nmap4j.includeHosts(ip);
			//nmap4j.addFlags(" -vv -sn --script="+nmapScriptLocation+"ip-forwarding --script-args='"+ipForwardTestParameter+"' ");
			//////////////////
			nmap4j.addFlags(" --script-trace --script ip-forwarding.nse --script-args='target=www.google.com' ");


			
			//////////////////
			nmap4j.execute() ;
			String output ="Erreur";
			if( !nmap4j.hasError() ) {
				output = nmap4j.getOutput() ;
			}
			else {
				print("Exécution impossible",2);
				print(nmap4j.getExecutionResults().getErrors(),2) ;
			}
			String rawOutput = XMLtoJSON(output);
			print("Réponse nmap : ",1);
			print(output,1);
			// Parsing de la réponse nmap
			Json currentReponse = Json.object();
			currentReponse = Json.read(rawOutput);
			
			print("Réponse nmap : ",1);
			print(currentReponse.toString(),1);
			print("-----------------------",1);

			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	

	
	/**
	 * Ajoute dans 'devices' la machine passée en parametre, sauf si elle existe déjà. Si elle existe, on ajoute simplement les informations manquantes ou différentes. 
	 * On considère qu'une machine existe déjà si son adresse mac et son adresse IP apparaissent déjà dans la liste des machines.
	 * @param scannedResult La machine à ajouter.
	 * @param devices une array list contenant les machines déjà ajoutées.
	 */
	private ArrayList<Json> addAndMerge(Json scannedResult, ArrayList<Json> devices) {
		boolean flag = true;

		ArrayList<Json> devicesMerged = new ArrayList<Json>();
		
		for(Json j : devices) {

			// Si l'ip et la mac apparaissent déjà
			if(j.at("ip").asString().equals(scannedResult.at("ip").asString()) && j.at("mac").asString().equals(scannedResult.at("mac").asString())) {
				print("La machine d'ip "+scannedResult.at("ip").asString()+" et mac "+scannedResult.at("mac").asString()+" existe déjà, merge des informations ..",1);

				flag = false;
				
				// MERGE
				
				// Merge des ports
				ArrayList<Json> toAddp = (ArrayList<Json>) scannedResult.at("ports").asJsonList();
				ArrayList<Json> existingp = (ArrayList<Json>) j.at("ports").asJsonList();
				for(Json current : toAddp) {
					if(!existingp.contains(current)) {
						existingp.add(current);
					}
				}
				j.atDel("ports");
				j.at("ports",existingp);
				
				// Merge des services
				ArrayList<Json> toAdds = (ArrayList<Json>) scannedResult.at("softwares").asJsonList();
				ArrayList<Json> existings = (ArrayList<Json>) j.at("softwares").asJsonList();
				for(Json current : toAdds) {
					if(!existings.contains(current)) {
						existings.add(current);
					}
				}
				j.atDel("softwares");
				j.at("softwares",existings);				
				
				// merge des noms d'OS
				j = merge(j,"os_name",scannedResult.at("os_name").asString(), scannedResult);

				
				// merge des versions d'OS
				j = merge(j,"os_version",scannedResult.at("os_version").asString(), scannedResult);

				
				
				devicesMerged.add(j);
			}
			else {
				devicesMerged.add(j);
			}
		}
		if(flag) {
			devicesMerged.add(scannedResult);
		}
		devices = devicesMerged;
		return devices;
	}
	
	/**
	 * Ajoute au champ property du Json j le champ toAdd. ScannedResult est le Json complet de la réponse (toAdd est contenu dans scannedResult)
	 * @param j Le champ Json de la réponse du logiciel
	 * @param property le champ à manipuler dans j
	 * @param toAdd la valeur à ajouter dans property
	 * @param scannedResult le json complet de la réponse nmap
	 * @return j modifié avec la nouvelle valeur
	 */
	private Json merge(Json j, String property, String toAdd, Json scannedResult) {
		Json existingVersion = j.at(property);
		if(existingVersion.isArray()) {
			ArrayList<Json> list = new ArrayList<Json>();
			list = (ArrayList<Json>) existingVersion.asJsonList();
			if(!list.contains(scannedResult.at(property))) {
				list.add(scannedResult.at(property));
				j.delAt(property);
				j.at(property,list);						
			}
		}
		else {
			if(!toAdd.equals(existingVersion.toString().replaceAll("\"", ""))) {
				ArrayList<String> version = new ArrayList<String>();
				version.add(toAdd);
				version.add(existingVersion.toString());
				j.delAt(property);
				j.at(property,version);
			}
		}	
		return j;
	}
	
	private void print(String m, int type) {
		if(debug && type == 1) {
			System.out.println(m);
		} else if(verbose && type == 2) {
			System.out.println(m);
		}
	}

}
