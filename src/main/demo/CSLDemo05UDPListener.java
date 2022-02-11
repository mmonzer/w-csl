package main.demo;

import java.io.File;

import com.csl.core.CSLContext;
import com.xcsl.interfaces.ICSLFlowListener;
import com.xcsl.json.Json;

import main.util.CSLRunningArgs;
import main.util.JsonCommands;




public class CSLDemo05UDPListener {

	static boolean verbose=false;



	
	public static void main(String[] args) {


		ICSLFlowListener listener=new ICSLFlowListener() {
			@Override
			public String getName() {
				// TODO Auto-generated method stub
				return "L2";
			}
			@Override
			public int newElementOnQueue(Json jj) {
		
				System.out.println("listener#1:"+jj);
				return ICSLFlowListener.DO_NOTHING;
			}

		};
		
		ICSLFlowListener listener2=new ICSLFlowListener() {
			@Override
			public String getName() {
				// TODO Auto-generated method stub
				return "L2";
			}
			@Override
			public int newElementOnQueue(Json jj) {
				
				System.out.println("listener#2:"+jj);
				return ICSLFlowListener.DO_NOTHING;
			}

		};
		
		
	
		
		
		
		System.out.println("Starting CSL IDS version "+CSLContext.VERSION);
		String configFile=System.getProperty("user.dir")+File.separator+"runconfig/CSLConfigIDS.json";
			
		
		CSLContext.instance.init(new CSLRunningArgs().parseArgs(args));
		
		
		CSLContext.instance.getCslUDPServer().addListener(1,listener); 
		CSLContext.instance.getCslUDPServer().addListener(1,listener2); 
	
	
		
		
		
		//démarrer si on
		//CSLContext.instance.initWebsocketListener();
		
		CSLContext.instance.setDebug(true);
		//CSLServer.setExternalWebSiteRoot("/Users/flausj/Documents/devx/demo/cslgraph");

		CSLContext.instance.getCslUDPServer().start();
		CSLContext.instance.getCslUDPServer().start();
		
		CSLContext.instance.startExec();
	
		
		JsonCommands.init();
		

		
		
		
	}
}
