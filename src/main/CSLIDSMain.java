package main;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.csl.core.CSLContext;
import com.csl.devdb.CSLServiceDevicesDB;
import com.csl.intercom.broker.MosquittoConfig;
import com.csl.intercom.jsoncmd.ApiGetHelp;
import com.csl.intercom.jsoncmd.JServiceLoader;
import com.xcsl.miniserver.ApiHttpServer;
import com.csl.util.ProcessUtil;
import com.csl.web.database.CSLServiceJsonDataBase;
import com.xcsl.ids.IDSTrace;
import com.xcsl.json.Json;

import main.services.AlertsService;
import main.services.CSLServiceDemo;
import main.services.CSLServiceIDS;
import main.services.CpeServices;
import main.services.CveServices;
import main.services.MonitorService;
import main.util.CSLRunningArgs;

public class CSLIDSMain {
	
	static boolean START_MOSQUITTO=false;
	/*
	 * 
	 * 
	 * 
	TODO
	
	- separer CSLModulesRunner
	
	- tester
		chgt log
		chgt database dir
		
		choix données
		
		test leran ou detect
		
	 * 
	 * 
	 */
	
	
	// JDK 11

	public void cleanRunningProcess() {
		// ProcessUtil.listJavaProcesses();

		// ProcessUtil.killProcess("main.TestLearningAndDetect");

		// String dir ="/Users/flausj/Documents/dev";
		// String jarFile="Logview.jar";
		// ProcessUtil.startJarIfNotRunning(".", "Logview.jar", false);

	}

	


	public static void main(String[] args) {

		System.out.println("Starting CSL IDS version  " + CSLContext.VERSION);
	
		
		Json j =CSLContext.instance.getConfig();
		
		
		CSLContext.instance.init(new CSLRunningArgs().parseArgs(args).setHasIdsRunner(true));
		
		// Init idsrunner : should be set in cslcontext
		//IDSRunner idsRunner= new IDSRunner(j, CSLRunningArgs.instance);
		
		
		CSLContext.instance.setDebug(true);
		
		
		boolean USE_BROKER=false; //true;
		JServiceLoader.setModuleName("IDS",new MosquittoConfig().setUseBroker(USE_BROKER));
		
		
		JServiceLoader.registerService(new CSLServiceDemo(), j, true);
		//CSLServiceIDS cslServiceIDS= new CSLServiceIDS();
		JServiceLoader.registerService(new AlertsService(), j, true);
		JServiceLoader.registerService(new MonitorService(), j, true);
		JServiceLoader.registerService(new CSLServiceIDS(), j, true);
		
		JServiceLoader.registerService(new CSLServiceJsonDataBase(), j, true);

		
	//	JServiceLoader.registerService(new GraphServices(), j, true);
		JServiceLoader.registerService(new CpeServices(), j, true);
		JServiceLoader.registerService(new CveServices(), j, true);
	//	JServiceLoader.registerService(new NmapServices(), j, true);
	//	JServiceLoader.registerService(new TapsServices(), j, true);
	
		
		// Init Databaseserver, httpserver, udpserver, ...
		
		System.out.println(CSLContext.instance);
		CSLContext.instance.postInit();

		//JServiceLoader.startService(new CSLServiceDemo(), j, true);
		System.out.println();

		

		// set the

	
		CSLContext.instance.start();
		
		
		
		// Mini http server included in JAVA (for api)
		ApiHttpServer apiHttpServer = new ApiHttpServer().createServer(
				new InetSocketAddress(9000), 
				JServiceLoader.getApiCommandsList(),
				new ApiGetHelp());
		
		//JsonCommandsNmap.init(j.get("nmap_service"));

		
		
		boolean test=true;
		
		IDSTrace.log(IDSTrace.GENERAL, "test");
		
		System.out.println();
		
		//IDSRunner.instance.getIdsParams().initFromJson(j, CSLRunningArgs.instance.getDataDir()
		//		); // pworkingDir);

		//System.out.println("User dir :"+CSLContext.instance.getUserDir());
		//System.out.println("Config dir (model,rules ...):"+IDSRunner.instance.getIdsParams().getIdsModelDir());
		

		
		if (!test) {

			//IDSRunner.instance.start(j, CSLRunningArgs.instance); //pworkingDir, configFile);
			
			//cslServiceIDS.setIDSRunner(idsRunner);
			
			CSLContext.instance.getIdsRunner().start();
			
		}
		else {
//			IDSParams idsParams= new IDSParams();
//			//		
//			idsParams.initFromJson(j,  CSLRunningArgs.instance.getDataDir(),
//					CSLRunningArgs.instance.getTestParam()); //pworkingDir,CSLRunningArgs.instance.getPathOfConfigFile());
//			
//
//			RulesEditor r= new RulesEditor(idsParams) ;//"idsconf", "RulesForDetection.txt", "SystemConfiguration.json", IDSRulesSystem.IDS_RULES);
////
//			Json js=r.loadDetectionRules();
//			System.out.println(js);
//			String s=js.get("text").asString();
//			System.out.println(s);
////
//			Json result=r.compileRules(js.get("text").asString());
//			System.out.println(result);
//
//			System.out.println("SYSTEM CONFIG");
//			js=r.loadSystemConfiguration();
//			System.out.println(js);
//			s=js.get("text").asString();
//			System.out.println(s);
//
//			result=r.compileSystemConfiguration(s,true);
//			System.out.println(result);
//
//
		//	System.exit(0);

		}
	}

}

