package main.demo;

import com.csl.intercom.broker.CSLInterModuleCommunicationManager;
import com.csl.intercom.broker.MosquittoConfig;
import com.csl.intercom.jsoncmd.ApiCommands;
import com.xcsl.json.Json;


/*
 * 
 *   exec externe
 * 
 */
public class Test04InterProcessCommunicationB {

	
	ApiCommands api= new ApiCommands("essai");
	
	
	CSLInterModuleCommunicationManager imcm = new CSLInterModuleCommunicationManager("IDS2", new MosquittoConfig());
	
	public void init() {
		
		
		
		
	}
	
	
	
	public static void main(String[] args) {
		
		
		
		Test04InterProcessCommunicationB test= new Test04InterProcessCommunicationB();
		
		
		// Execute commande externe (module A)
		
		Json r=test.imcm.executeCommand("essai", Json.object().set("cmd", "test").set("params", Json.object().set("x", 10)));
		
		System.out.println("Result (extern):"+r);
		
		
	}
}
