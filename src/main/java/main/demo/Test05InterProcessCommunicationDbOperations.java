package main.demo;

import com.csl.intercom.broker.CSLInterModuleCommunicationManager;
import com.csl.intercom.broker.MosquittoConfig;
import com.csl.intercom.jsoncmd.ApiCommands;
import com.csl.intercom.jsoncmd.ApiCommandsFactory;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.json.Json;


/*
 * 
 *   exec externe
 * 
 */
public class Test05InterProcessCommunicationDbOperations {

	ApiCommands api= new ApiCommandsFactory().createApiCommands("essai");
	CSLInterModuleCommunicationManager imcm = new CSLInterModuleCommunicationManager("DB", new MosquittoConfig());
	
	public void init() {
	}
	
	public static void main(String[] args) {

		Test05InterProcessCommunicationDbOperations test= new Test05InterProcessCommunicationDbOperations();
		
		
		// Execute commande externe (module A)
		
		Json jparams= Json.object();
		jparams.set("user", "user1");
		jparams.set("op", "LST_DEVICES");
			
		Json r=test.imcm.executeExternalCommand("devdb", Json.object().set("cmd", "op").set("params", jparams));
		System.out.println(r);

	}
}
