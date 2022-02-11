package main;

import com.csl.core.CSLContext;
import com.csl.devdb.CSLServiceDevicesDB;
import com.csl.intercom.broker.MosquittoConfig;
import com.csl.intercom.jsoncmd.JServiceLoader;
import com.csl.util.ProcessUtil;
import com.csl.web.database.CSLServiceJsonDataBase;
import com.xcsl.ids.IDSTrace;
import com.xcsl.json.Json;

import main.services.AlertsService;
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

	//	public String getWorkingDir(String[] args) {
	//		
	//		String OPTION_DIR = "datadir:";
	//		String pworkingDir = ""; // data dir for learn and detect offline, log dir for record and detect on-line
	//
	//		
	//		
	//		
	//		return pworkingDir;
	//	}


	//	public String getConfigFileName(String[] args) {
	//		
	//		
	//		CSLRunningParams params= new CSLRunningParams(args);
	//		
	//		if (params.hasError()) {
	//			System.out.println("Params errors:"+params.getError());
	//			System.exit(0);
	//		}
	//		String configFile="";
	//
	//		
	//		// LIRE LA CONFIG DE CONTEXT
	//				String path = System.getProperty("user.home");
	//
	//				if (configFile.isEmpty())
	//					configFile= System.getProperty("user.dir") + File.separator + "runconfig/CSLConfigIDS.json";
	//				
	//				
	//				//String paramsFile = ""; // "params.json";
	//
	//				File file = new File(configFile);
	//
	//				if (!file.exists()) {
	//					
	//					System.out.println("Cannot not find config file :"+file.getAbsolutePath());
	//					
	//					String fname=System.getProperty("user.dir") + File.separator + configFile;
	//					
	//					file = new File(fname);
	//					if (!file.exists()) {
	//						System.out.println("Cannot not find config file :"+file.getAbsolutePath());
	//						
	//						fname=System.getProperty("user.dir") + File.separator + "runconfig" + File.separator +configFile;
	//							
	//						file = new File(fname);
	//						if (!file.exists()) {
	//							System.out.println("Cannot find config file " + file.getAbsolutePath());
	//							System.exit(0);
	//						}
	//						else
	//							configFile=fname;
	//						
	//					}
	//					else {
	//						configFile= fname;
	//						
	//					}
	//					
	//					
	//				}
	//				
	//		return "";
	//	}
	//	



	public static void main(String[] args) {

		System.out.println("Starting CSL IDS version  " + CSLContext.VERSION);
		// Satart mosquitto 
		
		ProcessUtil.killProcess("mosquitto");
		
		
		
		if (START_MOSQUITTO) {
			ProcessUtil.killProcess("mosquitto");
			String output=ProcessUtil.execCmdInShell("/usr/local/sbin/", "./mosquitto", true );
			System.out.println(output);
		}
		

//		File f= new File("../../zzidsdir/data/test");//zzidsdir/data/test/");
//		
//		     // Populates the array with names of files and directories
//	        String[] pathnames = f.list();
//
//	        // For each pathname in the pathnames array
//	        for (String pathname : pathnames) {
//	            // Print the names of files and directories
//	            System.out.println(pathname);
//	        }

		
		/*if (CSLRunningArgs.instance.hasError()) {
			System.out.println("Error :"+CSLRunningArgs.instance.getError());
			System.exit(0);
		}
		//String pworkingDir=CSLRunningArgs.instance.getworkingDir();
		String configFile=CSLRunningArgs.instance.getConfigFile();

		System.out.println(CSLRunningArgs.instance.getPathOfConfigFile());*/
		
		//CSLContext.instance.setConfigFileName(configFile,CSLRunningArgs.instance.getUserDir());
		//CSLContext.context.setDataDir(CSLRunningArgs.instance.getDataDir());
		
		Json j =CSLContext.instance.getConfig();
		
		
		CSLContext.instance.init(new CSLRunningArgs().parseArgs(args).setHasIdsRunner(true));
		
		// Init idsrunner : should be set in cslcontext
		//IDSRunner idsRunner= new IDSRunner(j, CSLRunningArgs.instance);
		
		
		CSLContext.instance.setDebug(true);
		//CSLContext.instance.setTestMode(IDSRunner.instance.getIdsParams().isTestMode());
		
		
		boolean USE_BROKER=false; //true;
		JServiceLoader.setModuleName("IDS",new MosquittoConfig().setUseBroker(USE_BROKER));
		
		
		JServiceLoader.registerService(new CSLServiceDevicesDB(), j, true);
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
		CSLContext.instance.postInit();

		//JServiceLoader.startService(new CSLServiceDemo(), j, true);
		System.out.println();

		

		// set the

	
		CSLContext.instance.start();
		
		

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

