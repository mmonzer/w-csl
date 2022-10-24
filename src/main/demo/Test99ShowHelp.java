package main.demo;

import com.csl.intercom.broker.CSLInterModuleCommunicationManager;
import com.csl.intercom.broker.MosquittoConfig;
import com.csl.intercom.jsoncmd.ApiCommands;
import com.csl.intercom.jsoncmd.ApiCommandsFactory;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;


/*
 * 
 *   exec externe
 * 
 */
public class Test99ShowHelp {

	
	//IApiCommands api= new ApiCommands("essai");
	IApiCommands api= new ApiCommandsFactory().createApiCommands("essai");

	
	CSLInterModuleCommunicationManager imcm = new CSLInterModuleCommunicationManager("DB", new MosquittoConfig());
	
	public void init() {
		
		
		
		
	}
	
	
	
	public static void main(String[] args) {
		
		
		
		Test99ShowHelp test= new Test99ShowHelp();
		
		
		// Execute commande externe (module A)
		
		// Execute commande externe (module A)
		
				Json jparams= Json.object();
				jparams.set("user", "user1");
				//jparams.set("op", "LST_DEVICES");
					
				Json r=test.imcm.executeCommand("devdb", Json.object().set("cmd", "help").set("params", jparams));
				System.out.println(JsonUtil.prettyPrint(r));
				
				r=test.imcm.executeCommand("ids", Json.object().set("cmd", "help").set("params", jparams));
				System.out.println(JsonUtil.prettyPrint(r));
				
				r=test.imcm.executeCommand("cve", Json.object().set("cmd", "help").set("params", jparams));
				System.out.println(JsonUtil.prettyPrint(r));
				
				r=test.imcm.executeCommand("cpe", Json.object().set("cmd", "help").set("params", jparams));
				System.out.println(JsonUtil.prettyPrint(r));
			
				r=test.imcm.executeCommand("alerts", Json.object().set("cmd", "help").set("params", jparams));
				System.out.println(JsonUtil.prettyPrint(r));
			
				System.exit(0);
	}
}
