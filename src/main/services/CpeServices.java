package main.services;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.csl.intercom.jsoncmd.ApiCommands;
import com.csl.intercom.jsoncmd.ApiCommandsFactory;
import com.xcsl.interfaces.IApiCommands;
import com.xcsl.interfaces.ICSLService;
import com.xcsl.interfaces.IJsonCmd;
import com.xcsl.interfaces.IJsonCmdHelp;
import com.xcsl.json.Json;

import main.extensions.CpeSearch;

public class CpeServices implements ICSLService {
//	ApiCommands apiCommands= new ApiCommands("");
	
	IApiCommands apiCommands= new ApiCommandsFactory().createApiCommands("");
	
	String name="cpe";
	String configFileSectionName="cpe_service";
	
	public CpeServices() {
		this.name="cpe";
		this.configFileSectionName="cpe_service";
	}
	
	public CpeServices(String name, String configFileSectionName) {
		this.name=name;
		this.configFileSectionName=configFileSectionName;
	}
	
	

	@Override
	public String getConfigFileSectionName() {
		return configFileSectionName;
	}

	
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
