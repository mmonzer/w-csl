package com.csl.ids;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.csl.core.CSLContext;
import com.csl.interfaces.IIDSRunner;
import com.csl.modules.ModuleIDS;
import com.csl.web.websockets.CSLWebSocket;
import com.xcsl.ids.IDSContext;
import com.xcsl.ids.IDSMainProcessor;
import com.xcsl.ids.IDSTrace;
import com.xcsl.interfaces.IAlertManager;
import com.xcsl.interfaces.IAlertSender;
import com.xcsl.interfaces.ICSLLogger;
import com.xcsl.interfaces.ICancelChecker;
import com.xcsl.interfaces.ILearningProcessor;
import com.xcsl.interfaces.IOffLineDetectionProcessor;
import com.xcsl.json.Json;
import com.xcsl.learning.IDSLearnedRules;

public class IDSRunner implements IIDSRunner {

	private boolean DEBUG=false;

	private static final String LEARN_TAG = "learn";
	private static final String DETECT_OFFLINE_TAG = "detect_offline";
	private static final String DETECT_ONLINE_TAG = "detect_online";

	//public static IDSRunner instance = new IDSRunner();




	IDSParams idsParams=new IDSParams(CSLContext.instance.getIDSMainProcessor());

	boolean verbose = false;

	//boolean sendToBrowser = true;
	//boolean sendToConsole = false;


	boolean showReceivedObject =false;
	boolean canceling=false;
	boolean runningExec=false;

	private ScheduledExecutorService scheduler=null;

	ModuleIDS ids=null;

	Runnable task = new Runnable() {


		public void run() {
			String more="";
			//if (CSLContext.instance.getModuleContext("module_ids")!=null) {
			//ModuleIDS ids = (ModuleIDS) CSLContext.instance.getModuleContext("module_ids").getModule();
			if (ids!=null) more = "  ModuleIDS:"+ids.runningState();

			//System.out.println("IDSRunner:"+idsParams.getIDSModeAsString()+more);

		}
	};

	private ICSLLogger logger;

	//	private CSLRunningArgs cslRunningArgs;
	//
	//private Json jConfig;

	//public IDSRunner(Json jconfig, CSLRunningArgs cslRunningArgs, ModuleIDS ids, ICSLLogger logger) {
	public IDSRunner(IDSParams idsParams, ModuleIDS ids, ICSLLogger logger) {


		//this.jConfig=jconfig;
		this.idsParams=idsParams;
		
		this.ids=ids;
		this.logger=logger;
		//this.cslRunningArgs=cslRunningArgs;

		//		initFromJson(jconfig, cslRunningArgs);

		if (DEBUG) {


			scheduler
			= Executors.newSingleThreadScheduledExecutor();


			int delay = 1000;
			scheduler.scheduleAtFixedRate(task, 0, delay, TimeUnit.MILLISECONDS);

		}

		//CSLContext.instance.setIdsRunner(this);

	}


	long t_previous=0;
	public void debugState() {
		if (!DEBUG) return;
		long t= System.currentTimeMillis();
		if ((t-t_previous)<500) return ;
		t_previous=t;

		//ModuleIDS ids = (ModuleIDS) CSLContext.instance.getModuleContext("module_ids").getModule();
		//System.out.println("IDSRunner:"+idsParams.getIDSModeAsString()+"  ModuwleIDS:"+ids.runningState());


	}


	public void stop() {
		if (scheduler!=null) scheduler.shutdownNow();
	}

	//public String getUserDir() {
	//	return CSLContext.instance.getUserDir();
	//}







	public void showIDSState() {
		//ModuleIDS ids = (ModuleIDS) CSLContext.instance.getModuleContext("module_ids").getModule();
		System.out.println(ids.runningState());



	}

	public void switchModeTo(int iDSMode) {

		//arrete ce qui fonctionne (sauf idle) et attend IDLE

		updateModuleIdsOutputFlags();


		if (isRunningExec()) {
			setCanceling(true);
			while (!getIdsParams().isIdleMode()) {
				try {
					System.out.println("WAIT FOR IDLE");
					Thread.sleep(10); // 10 ms delay
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}  
			}
			setCanceling(false);
		}

		System.out.println("SWITCH TO MODE:"+iDSMode+" FROM "+getIDSMode());
		idsParams.setIDSMode(iDSMode);

		setRunningExec(true);
		if (idsParams.isIdleMode()) {
			execIdleMode();
		}
		else if (idsParams.isLearningMode()) {
			execLearning();
		}
		else if (idsParams.isRecordOnlyMode()) {
			execRecordOnly();
		}
		else if (idsParams.isDetectOffLineMode()) {
			execDetectOffLine();
		}
		else if (idsParams.isDetectOnLineMode()) {
			execDetectOnLine();
		}
		setRunningExec(false);
		//System.out.println("END OF ONLINE RUN");

	}

	public void switchModeToIdle() {
		switchModeTo(IDSParams.MODE_IDLE);
	}
	public void switchModeToRecording() {
		switchModeTo(IDSParams.MODE_RECORD_ONLY);
	}
	public void switchModeToDetectOnline() {
		switchModeTo(IDSParams.MODE_DETECT_ONLINE);
	}
	public void switchModeToDetectOffline() {
		switchModeTo(IDSParams.MODE_DETECT_OFFLINE);
	}
	public void switchModeToLearn() {
		switchModeTo(IDSParams.MODE_LEARN);
	}


	public boolean isCanceling() {
		return this.canceling;
	}

	public void setCanceling(boolean b) {
		this.canceling=b;
	}



	public boolean isRunningExec() {
		return runningExec;
	}


	public void setRunningExec(boolean runningExec) {
		this.runningExec = runningExec;
	}


	public void updateModuleIdsOutputFlags() {
		boolean b=getIdsParams().isSendToBrowser();
		//ModuleIDS ids = (ModuleIDS) CSLContext.instance.getModuleContext("module_ids").getModule();
		ids.setSendToBrowser(b);

		b=getIdsParams().isSendToConsole();
		ids.setSendToConsole(b);

	}

	public void setIdsSendToBrowser(boolean b) {
		getIdsParams().setSendToBrowser(b);
		updateModuleIdsOutputFlags();
	}

	public boolean isIdsSendToBrowser() {
		return getIdsParams().isSendToBrowser();
	}

	public void setIdsSendToConsole(boolean b) {
		getIdsParams().setSendToConsole(b);	
		updateModuleIdsOutputFlags();
	}

	public boolean isIdsSendToConsole() {
		return getIdsParams().isSendToConsole();
	}

	public void setIDSMode(int mode) {

		idsParams.setIDSMode(mode);
	}

	public int getIDSMode() {

		return idsParams.getIDSMode();
	}


	public String getIDSModeAsString() {

		return idsParams.getIDSModeAsString();
	}


	public IDSParams getIdsParams() {
		return idsParams;
	}




	public boolean isShowReceivedObject() {
		return showReceivedObject;
	}

	public void setShowReceivedObject(boolean showReceivedObject) {
		this.showReceivedObject = showReceivedObject;
	}

	public   String readInput(String prompt) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		System.out.print(prompt);
		// Reading data using readLine
		String s = "";
		try {
			s = reader.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
		return s;

	}

	public   String readInput(String prompt, String[] list) {

		List<String> listUpper = new ArrayList<String>();
		for (String s : list)
			listUpper.add(s.toUpperCase().trim());

		String s = "";
		while (!listUpper.contains(s)) {
			s = readInput(prompt);
			s = s.trim().toUpperCase();
		}

		return s;
	}

	public   String readInput(String prompt, String list) {

		String[] tokens = list.split(";");
		return readInput(prompt, tokens);
	}

	public   void outDisplay(Json jj) {
		if (getIdsParams().isSendToBrowser()) {

			Json j = Json.object();
			j.set("line", jj.toString());
			j.set("console_id", "#1");
			//CSLWebSocketForConsole.broadcastMessageJson("log", j);
			CSLWebSocket.broadcastMessageJson(CSLWebSocket.WEB_SOCKET_CONSOLE,j );
		}
		if (getIdsParams().isSendToConsole()) 
			System.out.println(jj);

	}

	public   void println(String target,String line) {
		if (getIdsParams().isSendToBrowser()) {

			Json j = Json.object();
			j.set("line", line);
			j.set("console_id",target);
			//			CSLWebSocketForConsole.broadcastMessageJson("log", j);
			CSLWebSocket.broadcastMessageJson(CSLWebSocket.WEB_SOCKET_CONSOLE,j );
		}
		if (getIdsParams().isSendToConsole()) 
			System.out.println(line);

	}


	public   void refreshHmi() {

		Json j = Json.object();
		j.set("line", "");
		j.set("refresh_hmi", true);
		j.set("console_id","any");
		//			CSLWebSocketForConsole.broadcastMessageJson("log", j);
		CSLWebSocket.broadcastMessageJson(CSLWebSocket.WEB_SOCKET_CONSOLE,j );


	}


	

	public   void execLearning() {
		boolean learnNetwork = true;
		boolean learnSysModel = true;
		
		
		//CSLContext.instance.getIDSMainProcessor().init();
		
		ILearningProcessor logAnalyzer =CSLContext.instance.getIDSMainProcessor()
				.getLogAnalyzer(idsParams.getFullPackets_dir_for_learning(), learnNetwork, learnSysModel);
		
		//LogAnalyzer logAnalyzer = new LogAnalyzer(idsParams.getIdsMainProcessor(), learnNetwork, learnSysModel);

		logAnalyzer.setDirToProcess(idsParams.getFullPackets_dir_for_learning());

//		logAnalyzer.setIdsModelDir(idsParams.getIdsModelDir());
//		logAnalyzer.setRulesFileNameForDetection(idsParams.getRulesForDetectionFileName());
//		logAnalyzer.setRulesFileNameForLearning(idsParams.getRulesForLearningFileName()); // rulesForLearningFileName);
//		logAnalyzer.setVariablesFileName(idsParams.getVariablesFileName());
//		logAnalyzer.setLearnedRulesFileName(idsParams.getLearnedRulesFileName()); // learnedRulesFileName);
		// //"LEARNED_RULES99.json");

		logAnalyzer.setVerbose(verbose);
//		logAnalyzer.setOutput(new IConsole() {
//			@Override
//			public void print(String tag,String s) {
//				println(tag,s);
//
//			}
//		});

		logAnalyzer.setCancelChecker(new ICancelChecker() {

			@Override
			public boolean hasBeenCanceled() {
				return isCanceling();
			}
		});
		//logAnalyzer.setSyslearnUpgradeExistingModels(idsParams.isSyslearnUpgradeExistingModels());


		boolean okrules = logAnalyzer.doLearning();

		if (isCanceling()) {
			idsParams.setIDSMode(IDSParams.MODE_IDLE);
			return;
		}

		if (!okrules) {
			println(LEARN_TAG,"Rules errors");
			for (String e : logAnalyzer.getPacketPreprocessingRulesErrors()) {
				logger.printError(e);
			}

			idsParams.setIDSMode(IDSParams.MODE_IDLE);

			return;
		}

		if (learnNetwork) {
			
			CSLContext.instance.getIDSMainProcessor().renameLearnedRulesWithTimeStamp();
			
			logAnalyzer.saveLearnedRules();
		}
		println(LEARN_TAG,"Stored Learned rules:");
		println(LEARN_TAG,"=====================");

		// logAnalyzer.readFiles();
		// IDSLearnedRules idsLearningRules = new IDSLearnedRules(new IDSVariables());
		// idsLearningRules.readFromFile("idsdata","LEARNED_RULES99.json");

		println(LEARN_TAG,"RESULT:");
		println(LEARN_TAG,""+logAnalyzer.getIdsLearnedRules());

		println(LEARN_TAG,"========================================================================");
		println(LEARN_TAG,"========================================================================");
		println(LEARN_TAG,"   END OF LEARNING ");
		println(LEARN_TAG,"========================================================================");
		println(LEARN_TAG,"========================================================================");

		idsParams.setIDSMode(IDSParams.MODE_IDLE);

		refreshHmi();

		return;

	}


//	public Json getLearnedRulesAsJson() {
//		//loadRules(getRulesForLearningFileName(), getVariablesFileName());
//
//		String dir= idsParams.getIdsModelDir();
//		String fileName= idsParams.getLearnedRulesFileName();
//
//		return FileUtils.readJsonFromFile(dir, fileName);
//	};




	public   void execIdleMode() {
		//ModuleIDS ids = (ModuleIDS) CSLContext.instance.getModuleContext("module_ids").getModule();
		IDSContext idsContext = CSLContext.instance.getIDSMainProcessor().getIDSContext();


		System.out.println("Exec idle");

		ids.setModeIdle();
		refreshHmi();



		//	println(DETECT_ONLINE_TAG,""+getIdsParams().getAsJson());
		//	println(DETECT_ONLINE_TAG,""+getIdsParams().getAsJson());

		//		while (idsParams.isIdleMode()) {
		//			try {
		//				Thread.sleep(10); // 10 ms delay
		//			} catch (InterruptedException e) {
		//				// TODO Auto-generated catch block
		//				e.printStackTrace();
		//			}  
		//		}


		return;

	}

	public   void execRecordOnly() {
		//ModuleIDS ids = (ModuleIDS) CSLContext.instance.getModuleContext("module_ids").getModule();
		IDSContext idsContext = CSLContext.instance.getIDSMainProcessor().getIDSContext();


		//initIdsContext(idsContext,verbose);
		updateModuleIdsOutputFlags();


		ids.reOpenLogFiles(getIdsParams().getFullPackets_dir_for_recording());

		ids.setModeRecord();
		refreshHmi();



		println(DETECT_ONLINE_TAG,""+getIdsParams().getAsJson());

		while (idsParams.isRunOnLineRecordOrDetect()&!isCanceling()) {
			try {
				Thread.sleep(10); // 10 ms delay
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  
		}

		ids.setModeIdle();
		idsParams.setIDSMode(IDSParams.MODE_IDLE);
		refreshHmi();

		return;

	}

	public   void execDetectOnLine() {
		//ModuleIDS ids = (ModuleIDS) CSLContext.instance.getModuleContext("module_ids").getModule();
		
		
		//IDSContext idsContext = CSLContext.instance.getIDSMainProcessor().getIDSContext();

		//prendre iContext ds main processor
		CSLContext.instance.getIDSMainProcessor().init();
		

//		initIdsContext(idsContext,verbose);
		updateModuleIdsOutputFlags();


		ids.reOpenLogFiles(getIdsParams().getFullPackets_dir_for_recording());
		ids.setModeDetect();
		refreshHmi();


		IDSMainProcessor idsMainProcessor = CSLContext.instance.getIDSMainProcessor();
		
		if (idsMainProcessor.getPacketPreprocessingRulesErrors().size() >0) {
		
			println(DETECT_ONLINE_TAG,""+"Invalid rules, stopping 275");

			for (String s : idsMainProcessor.getPacketPreprocessingRulesErrors())
				println(DETECT_ONLINE_TAG,""+s);
			for (String s : idsMainProcessor.getPacketPreprocessingRulesErrors())
				System.out.println(s);
			System.exit(0);
		}

		println(DETECT_ONLINE_TAG, idsMainProcessor.getIdsRulesSet().toStringAsTree());
		System.out.println(idsMainProcessor.getIdsRulesSet().toStringAsTree());

		println(DETECT_ONLINE_TAG,""+idsMainProcessor.getIDSVariables());
		println(DETECT_ONLINE_TAG,""+idsMainProcessor.getProcessVariables());
		println(DETECT_ONLINE_TAG,""+idsMainProcessor.getLearnedRules());

	//	IAlertManager alertViewer = idsContext.getCslAlertManager(); // new CSLAlertManager("IDS");

		println(DETECT_ONLINE_TAG,""+"========================================");
		println(DETECT_ONLINE_TAG,""+"========================================");
		println(DETECT_ONLINE_TAG,""+"=        Starting IDS online           =");
		println(DETECT_ONLINE_TAG,""+"========================================");
		println(DETECT_ONLINE_TAG,""+"========================================");

		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

		int duration = 30; // mn



		IDSTrace.setFlagOff(IDSTrace.UDP_TRACE);

		//		scheduler.scheduleAtFixedRate(new Runnable() {
		//
		//			@Override
		//			public void run() {
		//				// TODO Auto-generated method stub
		//				long n = CSLContext.context.getTimeFromStartingTime();
		//				long w = 60 * duration - (n / 1000);
		//
		//				if (w < 0) {
		//					idsContext.saveLearnedModel(idsParams.getNewLearnedRulesFileName());
		//					if (verbose)
		//						System.out.println("Variables at the end of the test:");
		//					if (verbose)
		//						System.out.println(
		//								CSLContext.context.getGlobalVariablesTable().toPrettyString("  "));
		//					System.out.println("End of test");
		//					idsParams.setIDSMode(IDSParams.MODE_IDLE);
		//					return;
		//
		//				} else {
		//					System.out.println("running... (ending in " + ((w + 30) / 60) + " mn) :"+getIDSModeAsString());
		//				}
		//
		//			}
		//
		//		}, 0, 1, TimeUnit.MINUTES);



		while (idsParams.isRunOnLineRecordOrDetect()&!isCanceling()) {
			try {
				Thread.sleep(10); // 10 ms delay
				//		System.out.println(getIDSModeAsString());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  
		}

		ids.setModeIdle();
		//idsContext.saveLearnedModel(idsParams.getNewLearnedRulesFileName());
		CSLContext.instance.getIDSMainProcessor().saveLearnedModelAsNewLearnedModel();
		
		idsParams.setIDSMode(IDSParams.MODE_IDLE);
		refreshHmi();

		return;
	}


	/*public Json getCurrentAlertsList() {


		ModuleIDS ids = (ModuleIDS) CSLContext.instance.getModuleContext("module_ids").getModule();
		IDSContext idsContext = ids.getIdsContext();

		ICSLAlertManager alertViewer = idsContext.getCslAlertManager(); // new CSLAlertManager("IDS");

		return alertViewer.getListOfCurrentAlertsAsJson();


	}*/

	public Json execOpAlert(Json params) {


		//ModuleIDS ids = (ModuleIDS) CSLContext.instance.getModuleContext("module_ids").getModule();
		IDSContext idsContext = CSLContext.instance.getIDSMainProcessor().getIDSContext();


		IAlertSender alertSender = CSLContext.instance.getIDSMainProcessor().getAlertManager(); // new CSLAlertManager("IDS");

		if (alertSender instanceof IAlertManager )
			return ((IAlertManager)alertSender).execOpAlert(params); 
		else {
			return Json.object().set("error", "Not possible to execOpAlert");
		}

	}

	public   void execDetectOffLine() {
		//ModuleIDS ids = (ModuleIDS) CSLContext.instance.getModuleContext("module_ids").getModule();
		//IDSContext idsContext = CSLContext.instance.getIDSMainProcessor().getIDSContext();

		//idem

		//initIdsContext(idsContext,verbose);

		ids.setModeIdle();  // packets are provided by the LogReplay


		ids.setModeDetect();
		System.out.println(idsParams.getIDSModeAsString());
		refreshHmi();




	//	IAlertSender alertSender = CSLContext.instance.getIDSMainProcessor().getAlertManager(); // new CSLAlertManager("IDS");

	//	IAlertSender alertViewer =  CSLContext.instance.getIDSMainProcessor().getAlertManager(); // new CSLAlertManager("IDS");



		IDSMainProcessor idsMainProcessor= CSLContext.instance.getIDSMainProcessor();
		
		//LogReplay logReplay = new LogReplay(idsParams.getFullPackets_dir_for_detection_offline(), idsMainProcessor);
		
		IOffLineDetectionProcessor offLineDetectionProcessor=idsMainProcessor.getOffLineDetectionProcessor(idsParams.getFullPackets_dir_for_detection_offline());
		
		offLineDetectionProcessor.setVerbose(verbose);
		
		if (offLineDetectionProcessor.getIDSRulesErrors().size() > 0) {
			System.out.println("Invalid rules, stopping 358");

			for (String s : offLineDetectionProcessor.getIDSRulesErrors())
				System.out.println(s);

			return;
			//System.exit(0);
		}

		println(DETECT_OFFLINE_TAG,""+offLineDetectionProcessor.getIdsRulesSet().toStringAsTree());

		println(DETECT_OFFLINE_TAG,""+offLineDetectionProcessor.getIdsVariables());
		println(DETECT_OFFLINE_TAG,""+offLineDetectionProcessor.getProcessVariables());
		println(DETECT_OFFLINE_TAG,""+offLineDetectionProcessor.getLearnedRules());

		
		//logReplay.set

		offLineDetectionProcessor.setCancelChecker(new ICancelChecker() {

			@Override
			public boolean hasBeenCanceled() {
				return isCanceling();
			}
		});
		offLineDetectionProcessor.setOnFinished(new Runnable() {

			@Override
			public void run() {

				//idsContext.saveLearnedModel(idsParams.getNewLearnedRulesFileName());
				offLineDetectionProcessor.saveNewLearnedRules();
				
				if (verbose)
					println(DETECT_OFFLINE_TAG,""+"Variables at the end of the test:");
				//if (verbose)
				//	System.out
				//	.println(CSLContext.instance.getGlobalVariablesTable().toPrettyString("  "));
				System.out.println("End of test");

				ids.setModeIdle();
				idsParams.setIDSMode(IDSParams.MODE_IDLE);
				refreshHmi();

				return;
			}

		});

		System.out.println("Detection");
		offLineDetectionProcessor.doDetection();
		
		//CSLContext.instance.getIDSMainProcessor().setCancelChecker

	}



	//	public void initFromJson(Json j, /*String pworkingDir,*/ CSLRunningArgs cslRunningArgs/*, String configFile*/	) {
	//		
	//		idsParams = new IDSParams();
	//		
	//		idsParams.initFromJson(j, cslRunningArgs.getDataDir(), cslRunningArgs.getTestParam()); // pworkingDir);
	//		
	//		
	//		System.out.println(idsParams.getAsJson());
	//	}


	/*
	 * 
	 * 	Json j : configfile
	 * 
	 *  String[] args 		: args of the main module
	 *  String pworkingDir 	: default data dir
	 *  
	 */
	//public void start(Json j, /*String pworkingDir,*/ CSLRunningArgs cslRunningArgs/*, String configFile*/) {
	public void start() {

		//String configFile=cslRunningArgs.getConfigFile();



		if (idsParams.isOn()) {

			// for test
			// ========
			final ScheduledExecutorService scheduler1 = Executors.newScheduledThreadPool(1);

			final Runnable beeper = new Runnable() {
				public void run() {
					String s = "line " + System.currentTimeMillis();
					Json j = Json.object();
					j.set("line", s);
					//					CSLWebSocketForConsole.broadcastMessageJson("log", j);
					CSLWebSocket.broadcastMessageJson(CSLWebSocket.WEB_SOCKET_CONSOLE,j );
					// System.out.println(s);
				}
			};
			// final ScheduledFuture<?> beeperHandle =
			// scheduler1.scheduleAtFixedRate(beeper, 1000, 1000,
			// java.util.concurrent.TimeUnit.MILLISECONDS);


			//			if (idsParams.isKillPreviousInstance()) {
			//				ProcessUtil.killProcess("main.CSLIDSMain");
			//				// String dir ="/Users/flausj/Documents/dev";
			//				// String jarFile="Logview.jar";
			//				//ProcessUtil.startJarIfNotRunning(".", "Logview.jar", false);
			//			}





			// DSTrace.setFlagOn(IDSTrace.UDP_TRACE);

			IDSTrace.log(IDSTrace.GENERAL, "test");


			System.out.println("");
			System.out.println("Configuration");
			System.out.println("=============");
			//ystem.out.println("\nIDS Config directory:" + idsParams.getIdsModelDir());

			//System.out.print("Exec params file:" + cslRunningArgs.getConfigFile() + "\n");

			// System.out.print("Data source :"+mode+"\n");
			System.out.println("Mode :" + idsParams.getIDSMode()
			+ " (0:idle, 1:record only, 2: detect online 3: learning 4: detect offline)");

			System.out.println("Data directory for detection offline :" + idsParams.getPackets_dir_for_detection_offline());
			System.out.println("Data directory for recording         :" + idsParams.getPackets_dir_for_recording() );
			System.out.println("Data directory for learning          :" + idsParams.getPackets_dir_for_learning());

//			System.out.println("Directory containing rules and models:" + idsParams.getIdsModelDir());
//			System.out.println(" Rules for detection :" + idsParams.getRulesForDetectionFileName());
//			System.out.println(" Rules for learning  :" + idsParams.getRulesForLearningFileName());
//			System.out.println(" System Configuration:" + idsParams.getVariablesFileName());
//
//			System.out.println(" Learned rules       :" + idsParams.getLearnedRulesFileName());
//			System.out.println(" New learned rules   :" + idsParams.getNewLearnedRulesFileName());

			
			System.out.println(CSLContext.instance.getIDSMainProcessor().getDirAndFileNamesInfo());
			System.out.println("");
			System.out.println(IDSTrace.paramsToString());
			System.out.println("");



			// do the init to get errors
			//ModuleIDS ids = (ModuleIDS) CSLContext.instance.getModuleContext("module_ids").getModule();
			IDSContext idsContext = CSLContext.instance.getIDSMainProcessor().getIDSContext();

			//initIdsContext(idsContext,verbose);
			updateModuleIdsOutputFlags();


			if (idsParams.isTestMode()) {
				System.out.println("=================================================================================");
				System.out.println("RUNNING TEST MODE:"+idsParams.getTestParam());
				System.out.println("=================================================================================\n\n");

				//idsParams.setIDSMode(idsParams.getTestParam());
			}

			switchModeTo(idsParams.getIDSMode());



		}
	}


	public IDSLearnedRules getLearnedRules() {

		return  CSLContext.instance.getIDSMainProcessor().getIDSContext().getLearnedRules();

		
	}




}


