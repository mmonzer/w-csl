package com.csl.ids;

import com.csl.core.CSLContext;
import com.csl.intercom.dbapi.DbapiHandler;
import com.csl.interfaces.IIDSRunner;
import com.csl.modules.ModuleIDS;
import com.csl.web.websockets.CSLWebSocket;
import com.ucsl.interfaces.*;
import com.ucsl.json.Json;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class IDSRunner implements IIDSRunner {

	private boolean DEBUG=false;

	private static final String LEARN_TAG = "learn";
	private static final String DETECT_OFFLINE_TAG = "detect_offline";
	private static final String DETECT_ONLINE_TAG = "detect_online";

	@Getter
	IDSParams idsParams=new IDSParams(CSLContext.instance.getIDSMainProcessor());

	boolean verbose = false;

	@Setter
    @Getter
    boolean canceling=false;

	@Setter
    @Getter
    boolean runningExec=false;

	private ScheduledExecutorService scheduler=null;

	ModuleIDS ids=null;

	Runnable task = new Runnable() {
		public void run() {
			String more="";
			if (ids!=null) more = "  ModuleIDS:"+ids.runningState();
		}
	};

	private final Logger logger =  LoggerFactory.getLogger(IDSRunner.class);

	public IDSRunner(IDSParams idsParams, ModuleIDS ids) {
		this.idsParams=idsParams;
		
		this.ids=ids;
		if (DEBUG) {
			scheduler
			= Executors.newSingleThreadScheduledExecutor();

			int delay = 1000;
			scheduler.scheduleAtFixedRate(task, 0, delay, TimeUnit.MILLISECONDS);

		}

	}

	public void stop() {
		if (scheduler!=null) scheduler.shutdownNow();
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

    public void updateModuleIdsOutputFlags() {
		boolean b=getIdsParams().isSendToBrowser();
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

	public   void println(String target,String line) {
		if (getIdsParams().isSendToBrowser()) {
			Json j = Json.object();
			j.set("line", line);
			j.set("console_id",target);
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
		CSLWebSocket.broadcastMessageJson(CSLWebSocket.WEB_SOCKET_CONSOLE,j );
	}

	public   void execLearning() {
		boolean learnNetwork = true;
		boolean learnSysModel = true;

		IILearningProcessor logAnalyzer =CSLContext.instance.getIDSMainProcessor()
				.getLogAnalyzer(idsParams.getFullPackets_dir_for_learning(), learnNetwork, learnSysModel);

		logAnalyzer.setDirToProcess(idsParams.getFullPackets_dir_for_learning());

		logAnalyzer.setVerbose(verbose);

		logAnalyzer.setCancelChecker(new ICancelChecker() {

			@Override
			public boolean hasBeenCanceled() {
				return isCanceling();
			}
		});

		boolean okrules = logAnalyzer.doLearning();

		if (isCanceling()) {
			idsParams.setIDSMode(IDSParams.MODE_IDLE);
			return;
		}

		if (!okrules) {
			println(LEARN_TAG,"Rules errors");
			for (String e : logAnalyzer.getPacketPreprocessingRulesErrors()) {
				logger.error(e);
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
		println(LEARN_TAG,"RESULT:");
		println(LEARN_TAG,""+logAnalyzer.saveLearnedRules());

		println(LEARN_TAG,"========================================================================");
		println(LEARN_TAG,"========================================================================");
		println(LEARN_TAG,"   END OF LEARNING ");
		println(LEARN_TAG,"========================================================================");
		println(LEARN_TAG,"========================================================================");

		idsParams.setIDSMode(IDSParams.MODE_IDLE);

		refreshHmi();
	}

	public   void execIdleMode() {
		System.out.println("Exec idle");

		ids.setModeIdle();
		refreshHmi();
	}

	public   void execRecordOnly() {
		updateModuleIdsOutputFlags();

		ids.reOpenLogFiles(getIdsParams().getFullPackets_dir_for_recording());

		ids.setModeRecord();
		refreshHmi();
		println(DETECT_ONLINE_TAG,""+getIdsParams().getAsJson());

		while (idsParams.isRunOnLineRecordOrDetect()&!isCanceling()) {
			try {
				Thread.sleep(10); // 10 ms delay
			} catch (InterruptedException e) {
				e.printStackTrace();
			}  
		}

		ids.setModeIdle();
		idsParams.setIDSMode(IDSParams.MODE_IDLE);
		refreshHmi();

	}

	public   void execDetectOnLine() {
		CSLContext.instance.getIDSMainProcessor().init();

		updateModuleIdsOutputFlags();

		ids.reOpenLogFiles(getIdsParams().getFullPackets_dir_for_recording());
		ids.setModeDetect();
		refreshHmi();


		IIDSMainProcessor idsMainProcessor = CSLContext.instance.getIDSMainProcessor();
		
		if (idsMainProcessor.getErrors().size() >0) {
			println(DETECT_ONLINE_TAG,""+"Invalid rules, stopping 275");

			for (String s : idsMainProcessor.getErrors())
				println(DETECT_ONLINE_TAG,""+s);
			for (String s : idsMainProcessor.getErrors())
				System.out.println(s);
			System.exit(0);
		}

		println(DETECT_ONLINE_TAG, idsMainProcessor.getIdsRulesSetAsString());

		println(DETECT_ONLINE_TAG,""+idsMainProcessor.getIDSVariables());
		println(DETECT_ONLINE_TAG,""+idsMainProcessor.getProcessVariables());
		println(DETECT_ONLINE_TAG,""+idsMainProcessor.getCurrentLearnedModel());

		println(DETECT_ONLINE_TAG,""+"========================================");
		println(DETECT_ONLINE_TAG,""+"========================================");
		println(DETECT_ONLINE_TAG,""+"=        Starting IDS online           =");
		println(DETECT_ONLINE_TAG,""+"========================================");
		println(DETECT_ONLINE_TAG,""+"========================================");



		int n=0;
		while (idsParams.isRunOnLineRecordOrDetect()&&!isCanceling()) {
				Thread.yield();
				System.out.println("xxXXIDS mode:"+getIDSModeAsString());
				n++;
		}

		ids.setModeIdle();
		CSLContext.instance.getIDSMainProcessor().saveLearnedModelAsNewLearnedModel();
		
		idsParams.setIDSMode(IDSParams.MODE_IDLE);
		refreshHmi();
	}

	public   void execDetectOffLine() {
		ids.setModeIdle();  // packets are provided by the LogReplay

		ids.setModeDetect();
		System.out.println(idsParams.getIDSModeAsString());
		refreshHmi();

		IIDSMainProcessor idsMainProcessor= CSLContext.instance.getIDSMainProcessor();

		IOffLineDetectionProcessor offLineDetectionProcessor=idsMainProcessor.getOffLineDetectionProcessor(idsParams.getFullPackets_dir_for_detection_offline());
		
		offLineDetectionProcessor.setVerbose(verbose);
		
		if (offLineDetectionProcessor.getIDSRulesErrors().size() > 0) {
			System.out.println("Invalid rules, stopping 358");

			for (String s : offLineDetectionProcessor.getIDSRulesErrors())
				System.out.println(s);

			return;
		}
		println(DETECT_OFFLINE_TAG,""+offLineDetectionProcessor.getProcessVariables());
		println(DETECT_OFFLINE_TAG,""+offLineDetectionProcessor.getLearnedRules());


		offLineDetectionProcessor.setCancelChecker(new ICancelChecker() {

			@Override
			public boolean hasBeenCanceled() {
				return isCanceling();
			}
		});
		offLineDetectionProcessor.setOnFinished(new Runnable() {

			@Override
			public void run() {
				offLineDetectionProcessor.saveNewLearnedRules();
				
				if (verbose)
					println(DETECT_OFFLINE_TAG,""+"Variables at the end of the test:");
				System.out.println("End of test");

				ids.setModeIdle();
				idsParams.setIDSMode(IDSParams.MODE_IDLE);
				refreshHmi();

			}

		});

		System.out.println("Detection");
		offLineDetectionProcessor.doDetection();
	}

	public void start() {
		if (idsParams.isOn()) {
			System.out.println("");
			System.out.println("Configuration");
			System.out.println("=============");
			System.out.println("Mode :" + idsParams.getIDSMode()
			+ " (0:idle, 1:record only, 2: detect online 3: learning 4: detect offline)");

			System.out.println("Data directory for detection offline :" + idsParams.getPackets_dir_for_detection_offline());
			System.out.println("Data directory for recording         :" + idsParams.getPackets_dir_for_recording() );
			System.out.println("Data directory for learning          :" + idsParams.getPackets_dir_for_learning());

			System.out.println(CSLContext.instance.getIDSMainProcessor().getDirAndFileNamesInfo());
			System.out.println("");

			updateModuleIdsOutputFlags();


			if (idsParams.isTestMode()) {
				System.out.println("=================================================================================");
				System.out.println("RUNNING TEST MODE:"+idsParams.getTestParam());
				System.out.println("=================================================================================\n\n");
			}

			switchModeTo(idsParams.getIDSMode());



		}
	}

	public IIDSLearnedRules getLearnedRules() {
		return  CSLContext.instance.getIDSMainProcessor().getCurrentLearnedModel();
	}
}


