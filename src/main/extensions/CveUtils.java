package main.extensions;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.xcsl.json.Json;
import com.xcsl.json.JsonUtil;

public class CveUtils {
	static Json devices;

	/**
	 * Lors de l'appel, le fichier devices.json est scanné, tout les CPE s'y
	 * trouvant sont récupérés, et les CVE associées sont trouvées. Ces CVE sont
	 * ensuite mises dans le fichier cve.json
	 * 
	 * @param ip   l'IP de l'API cveSearch
	 * @param port le port de l'API cve seach
	 * @throws ParseException
	 */
	public static void updateCveBase() throws ParseException {
		System.out.println("Updating CVE database ..");
		devices = Utils.readObjectFromDatabase("devices");

		Utils.writeObjectToDatabase("cve", Json.object(), "cve");

		Json toReturn = Json.object();
		ArrayList<Json> devicesList = (ArrayList<Json>) devices.at("devices").asJsonList();
		HashMap<String, ArrayList<Json>> map = new HashMap<String, ArrayList<Json>>();

		// On alimente la hashmap "map" avec les CPE et les machines ou services auquels
		// elles sont associées
		// Key : CPE, Value : Hashmap avec les machines associées
		for (Json current : devicesList) {
			Json currentCouple = Json.object();
			currentCouple.at("ip", current.at("ip"));
			currentCouple.at("id", current.at("uuid"));

		
			Json props=JsonUtil.getJson(current,"props");
			if (props==null) props=Json.object();
			// Récupération des CPE de l'OS et des services
			String os_cpe = JsonUtil.getStringFromJson(props, "os_cpe",""); //  , jse)getJson(current.at("props"), "os_cpe");
			System.out.println(current);
			System.out.println(os_cpe);
			boolean tracked= JsonUtil.getBooleanFromJson(props, "os_cpe_tracked", false);
			if (tracked) {
				if (map.containsKey(StringUtils.strip(os_cpe, "\""))) {
					HashMap<String, ArrayList<Json>> clone = (HashMap<String, ArrayList<Json>>) map.clone();
					ArrayList<Json> list = clone.get(StringUtils.strip(os_cpe, "\""));
					list.add(currentCouple);
					map.put(StringUtils.strip(os_cpe, "\""), list);
				} else {
					ArrayList<Json> list = new ArrayList<Json>();
					list.add(currentCouple);
					map.put(StringUtils.strip(os_cpe, "\""), list);
				}
			}
			if (current.get("props").get("softwares") != null) {
				ArrayList<Json> softwares = (ArrayList<Json>) current.get("props").at("softwares").asJsonList();
				for (Json currentSoft : softwares) {
					Json cpe = JsonUtil.getJson(currentSoft, "cpe");
					if (cpe != null && cpe != Json.read("null") && JsonUtil.getJson(currentSoft, "cpe_tracked") != null
							&& JsonUtil.getJson(currentSoft, "cpe_tracked").asBoolean())
						addCpe(map, cpe, currentCouple);
				}
			}
		}

		// Récupérations des clefs de la hashmap et loading de la Base des CVE
		Set<String> keys = ((HashMap<String, ArrayList<Json>>) map.clone()).keySet();
		HashMap<String, Json> cveMap = new HashMap<String, Json>();

		System.out.println("Those CPE will be used");
		ArrayList<String> keysString = new ArrayList<String>();
		for (String s : keys) {
			if (s != null && !s.contentEquals("null")) {
				System.out.println(s);
				keysString.add(s);
			}
		}
		CveSearch cvesearch = new CveSearch(keysString);

		// Création d'une nouvelle hashmap qui contient les paires id / ArrayList<cve>
		for (String s : keys) {
			if (s != null && !s.contentEquals("null")) {
				ArrayList<Json> list = cvesearch.getFullCve(s);
				ArrayList<Json> couples = map.get(s);
				for (Json j : couples) {
					ArrayList<Json> newList = new ArrayList<Json>();
					String curIp = StringUtils.strip(j.at("ip").toString(), "\"");
					String curId = StringUtils.strip(j.at("id").toString(), "\"");
					for (Json l : list) {
						l = Json.read(StringUtils.strip(l.toString(), "\""));
						l.at("impact", getImpactVector(l.at("vectorString").asString()));
						l.at("access", getAccessVector(l.at("vectorString").asString()));
						l.at("ip", curIp);
						newList.add(l);
					}

					if (!cveMap.containsKey(curId)) {
						Json jj = Json.object();
						cveMap.put(curId, jj);
					}
					cveMap.get(curId).at(s, newList);
					setPropNumber(curIp, s, newList.size());
				}
			}
		}

		// Ajout dans le Json CVE.json
		Set<String> finalKeys = cveMap.keySet();
		toReturn.at("test", cveMap);
		Utils.writeObjectToDatabase("cve", toReturn.at("test"), "cve");
		System.out.println("CVE update complete");
	}

	private static void setPropNumber(String ip, String cpe, int number) {
		System.out.println("J'essaie de changer le nombre pour " + ip + " et " + cpe + " value " + number);

		Json nul = Json.object();
		nul.at("nul", number);
		String[] splitedCpe = cpe.split(":");
		if (splitedCpe[1].contentEquals("/o")) {
			// C'est un OS
			// ip path value
			Utils.setDeviceProp(ip, "props.os_cve_count", nul.at("nul"));
		} else {
			// C'est un soft
			Json softwares;
			for (Json j : devices.at("devices").asJsonList()) {
				System.out.println(j);
				if (StringUtils.strip(j.at("ip").asString(), "\"").contentEquals(ip)) {
					softwares = j.at("props").at("softwares");
					int i = 0;
					if (softwares != null) {
						for (Json tmp : softwares.asJsonList()) {
							if (!tmp.at("cpe").isArray()) {
								if (StringUtils.strip(tmp.at("cpe").toString(), "\"").contentEquals(cpe)) {
									System.out.println("Je change le nombre pour " + ip + " path " + "props.softwares."
											+ i + ".cve_count" + " value " + nul.at("nul"));
									Utils.setDeviceProp(ip, "props.softwares." + i + ".cve_count", nul.at("nul"));
								}
							} else {
								if (StringUtils.strip(tmp.at("cpe").asJsonList().get(0).toString(), "\"")
										.contentEquals(cpe)) {
									System.out.println("Je change le nombre pour " + ip + " path " + "props.softwares."
											+ i + ".cve_count" + " value " + nul.at("nul"));
									Utils.setDeviceProp(ip, "props.softwares." + i + ".cve_count", nul.at("nul"));
								}
							}

							i++;
						}
					}
				}
			}
		}
	}

	private static Json getAccessVector(String vector) {
		vector = StringUtils.strip(vector, "\"");
		String[] vectorSplited = vector.split("/");
		Json result = Json.object();
		for (int i = 1; i < vectorSplited.length; i++) {
			String cur = vectorSplited[i];
			String[] curSplited = cur.split(":");
			String key = "", value = "";
			switch (curSplited[0]) {
			case "AV":
				key = "accessVector";
				switch (curSplited[1]) {
				case "L":
					value = "Local";
					break;
				case "A":
					value = "Adjacent network";
					break;
				case "N":
					value = "Network";
					break;
				}
				break;
			case "AC":
				key = "accessComplexity";
				switch (curSplited[1]) {
				case "H":
					value = "High";
					break;
				case "M":
					value = "Medium";
					break;
				case "L":
					value = "Low";
					break;
				}
				break;
			case "Au":
				key = "authentification";
				switch (curSplited[1]) {
				case "M":
					value = "Multiple";
					break;
				case "S":
					value = "Single";
					break;
				case "N":
					value = "None";
					break;
				default:
					value = "N/A";
				}

				break;
			}
			result.at(key, value);
		}
		return result;
	}

	private static Json getImpactVector(String vector) {
		vector = StringUtils.strip(vector, "\"");
		String[] vectorSplited = vector.split("/");
		Json result = Json.object();
		for (int i = 1; i < vectorSplited.length; i++) {
			String cur = vectorSplited[i];
			String[] curSplited = cur.split(":");
			String key = "", value = "";
			switch (curSplited[0]) {
			case "C":
				key = "confidentiality";
				break;
			case "I":
				key = "integrity";
				break;
			case "A":
				key = "availability";
				break;
			}
			switch (curSplited[1]) {
			case "N":
				value = "None";
				break;
			case "P":
				value = "Partial";
				break;
			case "C":
				value = "Complete";
				break;
			case "L":
				value = "Low";
				break;
			case "H":
				value = "High";
				break;
			default:
				value = "N/A";
			}
			result.at(key, value);
		}

		return result;
	}

	/**
	 * Fonction privée utilisée par updateCveBase(), permet de récupérer les cpe
	 * contenu dans un champ Json
	 * 
	 * @param listCpe la liste de CPE a remplir
	 * @param champ   le champ Json a lire
	 */
	private static void addCpe(HashMap<String, ArrayList<Json>> map, Json champ, Json currentCouple) {
		if (champ.isArray()) {
			ArrayList<Json> cpes = new ArrayList<Json>();
			cpes = (ArrayList<Json>) champ.asJsonList();
			for (Json biCurrent : cpes) {
				if (map.containsKey(StringUtils.strip(biCurrent.toString(), "\""))) {
					HashMap<String, ArrayList<Json>> clone = (HashMap<String, ArrayList<Json>>) map.clone();
					ArrayList<Json> list = clone.get(StringUtils.strip(biCurrent.toString(), "\""));
					list.add(currentCouple);
					map.put(StringUtils.strip(biCurrent.toString(), "\""), list);
				} else {
					ArrayList<Json> list = new ArrayList<Json>();
					list.add(currentCouple);
					map.put(StringUtils.strip(biCurrent.toString(), "\""), list);
				}
			}
		} else {
			if (map.containsKey(StringUtils.strip(champ.toString(), "\""))) {
				ArrayList<Json> list = map.get(StringUtils.strip(champ.toString(), "\""));
				list.add(currentCouple);
				map.put(StringUtils.strip(champ.toString(), "\""), list);
			} else {
				ArrayList<Json> list = new ArrayList<Json>();
				list.add(currentCouple);
				map.put(StringUtils.strip(champ.toString(), "\""), list);
			}
		}
	}
}