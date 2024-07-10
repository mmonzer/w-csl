package main.services;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import lombok.Getter;
import org.xml.sax.SAXException;

import com.csl.intercom.jsoncmd.ApiCommandsFactory;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.interfaces.ICSLService;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.interfaces.IJsonCmdHelp;
import com.ucsl.json.Json;

import main.extensions.CpeSearch;

<<<<<<< HEAD
public class CpeServices extends Service {
	/**
	 * Default constructor of the CPE service.
	 */
	public CpeServices() {
		this("cpe",
				"cpe description",
		"cpe_service");
	}

	/**
	 * Generic constructor of the CPE service.
	 */
	public CpeServices(String name, String description, String configFileSectionName) {
		super(name, description,configFileSectionName);
	}

	/**
	 * Initialization of the CPE service commands
	 * @param jConfig the configuration section of the configuration file
	 * @param cslDir the CSL directory
	 * @return true if the initialization happened with no problems, false otherwise.
	 */
=======
public class CpeServices implements ICSLService {
	
	String name="cpe";
	@Getter
	IApiCommands apiCommands= new ApiCommandsFactory().createApiCommands(name);

	@Getter
	String configFileSectionName="cpe_service";
	
	public CpeServices() {
		this.name="cpe";
		this.configFileSectionName="cpe_service";
	}
	
	public CpeServices(String name, String configFileSectionName) {
		this.name=name;
		this.configFileSectionName=configFileSectionName;
	}

	public String addCmd(String name, IJsonCmd j) {
		return apiCommands.registerCmd(name, j);
	}

	public String addCmd(String name, IJsonCmd j, IJsonCmdHelp jh) {
		return apiCommands.registerCmd(name, j,jh);
	}

>>>>>>> origin/feature/refactor_code
	@Override
	public boolean init(Json jConfig, String cslDir) {
		System.out.println("Initialising CPE functions .."+jConfig);
		CpeSearch c = new CpeSearch();
		try {
			String path = jConfig.get("dictionaryPath").asString();
			
			String[] splited = path.split("\\.");
 			switch(splited[1]) {
			case "json":
				c.readDictionnaryFromJson(path);
				break;
			case "xml":
				c.readDictionnaryFromXML(path);
				break;
			default :
				System.err.println("Unknown extention "+path.split(".")[1]+", trying xml");
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {

			e.printStackTrace();
		}
		System.out.println("CPE functions opérational");

		
		addCmd("getPrefix", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				Json result = Json.object();
				return result.at("result",c.getPrefix());
			}
		});
		addCmd("getVendor", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				Json result = Json.object();
				return result.at("result",c.getVendor(params.get("prefix").asString()));
			}
		});		
		addCmd("getProduct", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				Json result = Json.object();
				return result.at("result",c.getProduct(params.get("prefix").asString(),params.get("vendor").asString()));
			}
		});	
		addCmd("getVersion", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				Json result = Json.object();
				return result.at("result",c.getVersion(params.get("prefix").asString(),params.get("vendor").asString(),params.get("product").asString()));
			}
		});			
		return true;
	}
<<<<<<< HEAD
=======

	@Override
	public boolean terminate() {
		// TODO Auto-generated method stub
		return false;
	}
	
>>>>>>> origin/feature/refactor_code
}
