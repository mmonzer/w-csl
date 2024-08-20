package main.demo;

import com.csl.intercom.broker.CSLInterModuleCommunicationManager;
import com.csl.intercom.broker.MosquittoConfig;
import com.csl.intercom.jsoncmd.ApiCommands;
import com.csl.intercom.jsoncmd.ApiCommandsFactory;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.json.Json;


/*
 * 
 * 	lance un listener de commandes
 * 
 */
public class Test04InterProcessCommunicationA {
	ApiCommands api= new ApiCommandsFactory().createApiCommands("essai");
	
	CSLInterModuleCommunicationManager imcm = new CSLInterModuleCommunicationManager("IDS", new MosquittoConfig());
	
	public void init() {
		
		api.registerCmd("test", new IJsonCmd() {
			
			@Override
			public Json exec(Json params) {
				// TODO Auto-generated method stub
				int x=params.get("x").asInteger();
				Json result=Json.object().set("value",x/2);
				return result;
			}
		});
		
		imcm.registerAPI(api);
		
		imcm.start();
	}
	
	
	
	public static void main(String[] args) {
		Test04InterProcessCommunicationA test= new Test04InterProcessCommunicationA();

		test.init();
		
		Json r=test.imcm.executeCommand("essai", Json.object().set("cmd", "test").set("params", Json.object().set("x", 10)));
		System.out.println("Result (local)="+r);
		
		
	}
}
