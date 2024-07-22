package main.services;

import com.csl.core.CSLContext;
import com.csl.intercom.jsoncmd.JsonCmdHelp;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import main.extensions.CveUtils;
import main.extensions.Utils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CveServices extends Service {
	/**
	 * Default constructor of the CVE service.
	 */
	public CveServices() {
		this("cve",
				"cve description","cve_service");
	}

	/**
	 * Generic constructor of the CVE service.
	 */
	public CveServices(String name, String description, String configFileSectionName) {
		super(name, description,configFileSectionName);
	}

	/**
	 * Initialization of the CveServices commands.
	 * It can be request the list of CVE depending on the parameter:
	 *  - "any" : sends all the CVE of every machine
	 *  - <uuid> : sends the CVE related to the given machine
	 * @param jConfig the configuration section of the configuration file
	 * @param cslDir the CSL directory
	 * @return true if the initialization happened with no problems, false otherwise.
	 */
	@Override
	public boolean init(Json jConfig, String cslDir) {
		addCmd("getCve", new IJsonCmd() {
			
			@Override
			public Json exec(Json params) {
				String parameter = JsonUtil.getStringFromJson(params, "mode","any");
				if(parameter.contentEquals("any")) {
					Json result = Json.object();
					ArrayList<Json> resultArray = new ArrayList<Json>();
					
					Json cve =Utils.readObjectFromDatabase("cve");
					HashMap<String, Object> test = (HashMap<String, Object>) cve.asMap();
					Set<String> bitest = test.keySet();
					for(String tritest : bitest) {
						Json current = cve.at(tritest);
						Map<String, Object> test2 = current.asMap();
						Set<String> keySet = test2.keySet();
						for(String s : keySet) {
							Json tmp = current.at(s);
							for(Json j : tmp.asJsonList()) {
								Json jj= Json.object();
								jj.set("ip", JsonUtil.getStringFromJson(j,"ip",tritest));
								jj.set("id", JsonUtil.getStringFromJson(j,"id", ""));
								jj.set("cwe", JsonUtil.getStringFromJson(j,"cwe", ""));
								
								jj.set("cvss", JsonUtil.getStringFromJson(j,"cvss", ""));
								if (j.has("access")) jj.set("access", j.get("access"));
								if (j.has("impact")) jj.set("impact", j.get("impact"));
								if (j.has("cvss-vector")) jj.set("cvss-vector", j.get("cvss-vector"));
								if (j.has("Published")) jj.set("Published", j.get("Published"));
								if (j.has("summary")) jj.set("summary", j.get("summary"));

								resultArray.add(jj);							}
							
						}
					}
					result.at("test",resultArray);
					return result.at("test");						
				}
				else {
					Json cve = Utils.readObjectFromDatabase("cve");
					
					ArrayList<Json> total = new ArrayList<Json>();
					Json toParse = cve.at(parameter);
					if(toParse != null) {
						Map<String, Object> test = toParse.asMap();
						Set<String> keySet = test.keySet();
						for(String s : keySet) {
							Json tmp = toParse.at(s);
							for(Json j : tmp.asJsonList()) {
							
							}
							
						}
						Json toreturn = Json.object();
						toreturn.at("result",total);
						return toreturn.at("result");					
					}
					else {
						return Json.object();
					}
						
				}

			}
		},
				new JsonCmdHelp()
				.setDesc("return the list of cve ")
				.setResult("list of cve", JsonCmdHelp.JSON)
				.setStatus(JsonCmdHelp.STATUS_OK)
				);
		
		
		
		addCmd("stats", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				System.out.println("start exec ");
				System.out.println("Exec JCmd test_cmd :"+params);
				System.out.println("Fin exec");
				Json j=Json.object();
				j.set("all", 70);
				Json jvalues=Json.array();
				jvalues.add(Json.object().set("level_name","low").set("count", 5));
				jvalues.add(Json.object().set("level_name","medium").set("count", 20));
				jvalues.add(Json.object().set("level_name","high").set("count", 10));
				jvalues.add(Json.object().set("level_name","critical").set("count", 3));
				j.set("bylevels", jvalues);
				
				

				return j;
			}
		},
				new JsonCmdHelp()
				.setDesc("return the number of active cve ")
				.setResult("{all:n, bylevels:[n1, n2, n3, n4]}", JsonCmdHelp.JSON)
				.setStatus(JsonCmdHelp.STATUS_TODO)
				);
		
		
		addCmd("status", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				System.out.println("start exec ");
				System.out.println("Exec JCmd test_cmd :"+params);
				System.out.println("Fin exec");
				Json j=Json.object();
				j.set("isUpToDate", true);
				long t=CSLContext.instance.getTimeSystemCurrent()-1000*60*60;
				j.set("lastUpdateTime",t);
				
				

				return j;
			}
		},
				new JsonCmdHelp()
				.setDesc("return the status of cve update ")
				.setResult("{     isUpToDate: true,  lastUpdateTime: 1636088584326" + 
						"}", JsonCmdHelp.JSON)
				.setStatus(JsonCmdHelp.STATUS_TODO)
				);
		
		/*
		 * Permet à l'IHM de demander la mise à jour de la base de CVE en fonction de devices.json
		 */
		addCmd("updateCve", new IJsonCmd() {
			
			@Override
			public Json exec(Json params) {
				try {
					CveUtils.updateCveBase();
				} catch (ParseException e) {
					e.printStackTrace();
				}
				return Json.object();
			}
		});
		
		addCmd("testsave", new IJsonCmd() {
			
			@Override
			public Json exec(Json params) {
				
				return Utils.writeObjectToDatabase(params.get("name").asString(),params.get("contents"),"0" );
			}
		});
		return true;
	}
}