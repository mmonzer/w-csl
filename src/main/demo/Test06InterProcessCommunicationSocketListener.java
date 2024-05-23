package main.demo;

import com.csl.intercom.broker.CSLInterModuleCommunicationManager;
import com.csl.intercom.broker.ISocketMsgListener;
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
public class Test06InterProcessCommunicationSocketListener {

	IApiCommands api= new ApiCommandsFactory().createApiCommands("essai");
	CSLInterModuleCommunicationManager imcm = new CSLInterModuleCommunicationManager("DB", new MosquittoConfig());
	
	public void init() {
		
	}
	
	
	
	public static void main(String[] args) {
		Test06InterProcessCommunicationSocketListener test= new Test06InterProcessCommunicationSocketListener();
		
		// Execute commande externe (module A)
		
		ISocketMsgListener is = new ISocketMsgListener() {
			
			@Override
			public void messageArrived(String websocketName,String jSocket) {
				// TODO Auto-generated method stub
				System.out.println("SOCKET  :"+websocketName);
				System.out.println("CONTENTS:"+jSocket);
				
			}
		};
		Json jparams= Json.object();
		jparams.set("user", "user1");
		jparams.set("op", "LST_DEVICES");
			
		test.imcm.registerSocketMsgListener(is);
	}
}
