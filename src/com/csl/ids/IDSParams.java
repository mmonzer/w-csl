package com.csl.ids;

import com.csl.core.CSLContext;
import com.xcsl.ids.IDSMainProcessor;
import com.xcsl.interfaces.IConsole;
import com.xcsl.json.Json;
import com.xcsl.json.JsonUtil;
import com.xcsl.util.IDSUtil;



public class IDSParams {


	public static final String MODE = "mode";
	public static final String PACKETS_DIR_FOR_LEARNING = "packets_dir_for_learning";
	public static final String PACKETS_DIR_FOR_DETECTION_OFFLINE = "packets_dir_for_detection_offline";
	public static final String PACKETS_DIR_FOR_RECORDING = "packets_dir_for_recording";

	public static final String IDS_CONF = "ids_conf";
	private static final String IDS_CONF_SEP = IDS_CONF+"/";

	//static public IDSParams instance= new IDSParams();

	/*
	 * 
	 * 	IDSMode  
	 * 		0: idle (server mode)
	 * 		1: record
	 * 		3: learn
	 * 		2: detect_online
	 * 		4: detect_offline	
	 * 
	 */

	//"help_mode":"0:idle, 1:record only, 2: detect online 3: learning 4: detect offline",

	public static int MODE_IDLE=0;
	public static int MODE_RECORD_ONLY=1;
	public static int MODE_DETECT_ONLINE=2;
	public static int MODE_LEARN=3;
	public static int MODE_DETECT_OFFLINE=4;

	public static String[] idsModeAsString= new String[] {"IDLE", "RECORDING", "ONLINE_DETECTION","LEARNING","OFF_LINE_DETECTION"};


	public static String SHOW_CONSOLE_ON_HMI="show_console_on_hmi_recording_page";
	public static String SHOW_ALERTS_ON_HMI="show_alerts_on_hmi_recording_page";

	public static String SHOW_LEARNED_MODEL_TABLE="show_learned_model_table";
	public static String SHOW_DEVICES_MODEL_TABLE="show_devices_model_table";
	public static String SHOW_CONSOLE_ON_HMI_LEARNING_PAGE="show_console_on_hmi_learning_page";

	public static String SHOW_ALERTS_ON_HMI_off_line="show_alerts_on_hmi_offline_detection_page";

	public static String TAPS_ID="taps_id";
	public static String TAPS_DIR="taps_dir";

	//IFileStoreService fileUtils= new FileStoreService();

	//	public boolean runOnLineRecordOrDetect=false;


	boolean on=false;

	boolean doNotUseCurrentIDSParamsFileName=false;

	Json jCurrentVariables = Json.object();

	private String dataDir=CSLContext.instance.getUserDir()+IDSUtil.fileSeparator+"idsdata";
	private String defaultDataDir=CSLContext.instance.getUserDir()+IDSUtil.fileSeparator+"idsdata";


	int idsMode=0;


	//String userDir="";

	//IDSRulesSystem irs=null;

	boolean logToFile=true;
	boolean sendToBrowser=true;
	boolean sendToConsole=false;




	private String subdir_learn="";
	private String subdir_test="";


	//String workingDir="";
	//String modelDir="";			 // where are the files describing the model
	//	String dataDir="";			 // where are the data for learning or offline detection
	//	String recordDir=""; //		 // where to record dadat
	//	String validationDataDir=""; // where to take data to validate the model right after learning

	/*	"packets_dir_for_detection_offline":"/Users/flausj/Documents/dev/zzidsdir/data/poc/0-comportement_normal",
	"packets_dir_for_detection_offline_info":"used for detection off line (mode 4) and mode 3 if validation after lerarning ",

	"packets_dir_for_recording":"/Users/flausj/Documents/dev/zzidsdir/data/poc/record",
	"packets_dir_for_recording_info":"used for mode record and detect on line with log (mode 1 and 2)",

	"packets_dir_for_learning":"/Users/flausj/Documents/dev/zzidsdir/data/poc/0-comportement_normal",
	"packets_dir_for_learning_info":"used for leraning  (mode 3)",
	"validation_after_learning":false,
	 */


	IDSDataSetManager datasetManager = new IDSDataSetManager(this);

	IConsole console= new IDSConsole(this);

	//IDSTapManager tapManager= null;

	String packets_dir_for_detection_offline="";
	String packets_dir_for_recording="";
	String packets_dir_for_learning="";

	String currentDataSetNameForDetectionOffLine=""; // subdir 
	String currentDataSetNameForRecording=""; // subdir 
	String currentDataSetNameForLearning=""; // subdir 

	private boolean killPreviousInstance;
	private String currentIDSParamsFileName="";

	//boolean showConsoleOnHMI=true;
	//boolean showAlertsOnHMI=true;

	private IDSMainProcessor idsMainProcessor;


	public IDSParams(IDSMainProcessor idsMainProcessor) {

		this.idsMainProcessor=idsMainProcessor;

		//this.dataDir=idsMainProcessor.getDefaultIdsDataDir();
		//this.defaultDataDir=idsMainProcessor.getDefaultIdsDataDir();

	}


	//	public String getIdsModelDir() {
	//		return idsMainProcessor.getIdsModelDir();
	//	}

	public IConsole getConsole() { return console; }

	public String toString() {

		String s="";
		s=s+"Root datadir :"+getDataDir();
		s=s+"\n"+("Mode :" + getIDSMode()
		+ " (0:idle, 1:record only, 2: detect online 3: learning 4: detect offline)");

		s=s+"\n"+("Data directory for detection offline :" + getPackets_dir_for_detection_offline());
		s=s+"\n"+("Data set for  detection offline      :" + currentDataSetNameForDetectionOffLine);

		s=s+"\n"+("Data directory for recording         :" + getPackets_dir_for_recording() );
		s=s+"\n"+("Data set for   for recording         :" + currentDataSetNameForRecording );

		s=s+"\n"+("Data directory for learning          :" + getPackets_dir_for_learning());
		s=s+"\n"+("Data set for learning                :" + currentDataSetNameForLearning);
		s=s+"\n";
		s=s+"currentIDSParamsFileName:"+currentIDSParamsFileName;
		s=s+"\n";
		//		s=s+"Taps ID :"+listTapIds;
		//		s=s+"\n";
		//		s=s+"Taps dir:"+tapsDir;
		//		s=s+"\n";

		//		s=s+"\n"+("Directory containing rules and models:" + getIdsModelDir());
		//		s=s+"\n"+(" Rules for detection :" + getRulesForDetectionFileName());
		//		s=s+"\n"+(" Rules for learning  :" + getRulesForLearningFileName());
		//		s=s+"\n"+(" System Configuration:" + getVariablesFileName());
		//
		//		s=s+"\n"+(" Learned rules       :" + getLearnedRulesFileName());
		//		s=s+"\n"+(" New learned rules   :" + getNewLearnedRulesFileName());
		return s;

	}


	public void loadCurrentVariables() {

		if (doNotUseCurrentIDSParamsFileName) return;
		//String dir=		getIdsModelDir();
		String fileName=		getCurrentIDSParamsFileName();
		jCurrentVariables=idsMainProcessor.readJsonFromModelDir("", fileName);
		//	getFileStoreServices().readJsonFromFile(dir, fileName);

		currentDataSetNameForDetectionOffLine=JsonUtil.getStringFromJson(jCurrentVariables,"currentDataSetNameForDetectionOffLine", "");
		currentDataSetNameForRecording=JsonUtil.getStringFromJson(jCurrentVariables,"currentDataSetNameForRecording", "");
		currentDataSetNameForLearning=JsonUtil.getStringFromJson(jCurrentVariables,"currentDataSetNameForLearning", "");

		boolean b=JsonUtil.getBooleanFromJson(jCurrentVariables,"sendToBrowser", sendToBrowser);
		setSendToBrowser(b);

		b=JsonUtil.getBooleanFromJson(jCurrentVariables,"sendToConsole", sendToBrowser);
		setSendToConsole(b);

		//icii
		//showConsoleOnHMI=JsonUtil.getBooleanFromJson(j,"showConsoleOnHMI", showConsoleOnHMI);
		//showAlertsOnHMI=JsonUtil.getBooleanFromJson(j,"showAlertsOnHMI", showAlertsOnHMI);

		String s=JsonUtil.getStringFromJson(jCurrentVariables,"idsMode", "");
		setIDSMode(s);

	}

	public void saveCurrentVariables() {



		if (doNotUseCurrentIDSParamsFileName) return;

		//String dir=		getIdsModelDir();
		String fileName=		getCurrentIDSParamsFileName();

		Json j= Json.object();

		j.set("currentDataSetNameForDetectionOffLine",currentDataSetNameForDetectionOffLine );
		j.set("currentDataSetNameForRecording", currentDataSetNameForRecording);
		j.set("currentDataSetNameForLearning", currentDataSetNameForLearning);
		j.set("sendToBrowser", sendToBrowser);
		j.set("sendToConsole", sendToConsole);
		j.set("idsMode", getIDSModeAsString());
		//j.set("showConsoleOnHMI", showConsoleOnHMI);
		//j.set("showAlertsOnHMI", showAlertsOnHMI);


		//fileUtils.saveJsonToFile(dir, fileName, j);
		idsMainProcessor.saveJsonInModelDir("", fileName, j);

	}


	private boolean testmode=false;
	private boolean testparam=false;
	//private Boolean relative_to_CSLConfig_dir=false;
	//private String dirOfConfigFile="";
	private boolean showReceivedObject;

	public boolean isTestMode() {
		return testmode;
	}

	public void setTestMode(boolean testmode) {
		this.testmode = testmode;
	}



	public boolean getTestParam() {
		return testparam;
	}

	public void setTestParam(boolean testparam) {
		this.testparam = testparam;
	}

	//	public int getTestParamAsInteger() {
	//
	//		String s=getTestParam().toUpperCase();
	//
	//
	//		for (int i=0; i<IDSParams.idsModeAsString.length;i++) {
	//			if (IDSParams.idsModeAsString[i].compareTo(s)==0) return i;
	//
	//		}
	//
	//		return 2; // default = detect on line
	//
	//
	//
	//	}




	//	public boolean isShowConsoleOnHMI() {
	//		return showConsoleOnHMI;
	//	}
	//
	//	public void setShowConsoleOnHMI(boolean showConsoleOnHMI) {
	//		this.showConsoleOnHMI = showConsoleOnHMI;
	//	}
	//
	//	public boolean isShowAlertsOnHMI() {
	//		return showAlertsOnHMI;
	//	}
	//
	//	public void setShowAlertsOnHMI(boolean showAlertsOnHMI) {
	//		this.showAlertsOnHMI = showAlertsOnHMI;
	//	}

	public boolean getParamFromCurrentVariablesAsBoolean(String name) {
		Json j=jCurrentVariables.get(name);
		if (j==null) return false;
		return j.asBoolean();
	}

	public String getParamFromCurrentVariablesAsString(String name) {
		Json j=jCurrentVariables.get(name);
		if (j==null) return "";
		return j.asString();
	}

	public void setParamInCurrentVariables(String name,boolean b) {
		jCurrentVariables.set(name,b);
		saveCurrentVariables();
	}
	public void setParamInCurrentVariables(String name,String s) {
		jCurrentVariables.set(name,s);
		saveCurrentVariables();
	}




	public int getIdsMode() {
		return idsMode;
	}


	public void setIdsMode(int idsMode) {
		this.idsMode = idsMode;
	}


	public Json getAsJson() {

		Json j = Json.object();

		j.set("on", on);
		j.set("IDSMode", getIdsMode());


		//		j.set("idsConfDir", getIdsModelDir());
		//		j.set("rulesForDetectionFileName",rulesForDetectionFileName);
		//		j.set("rulesForLearningFileName",rulesForLearningFileName);
		//		j.set("rulesForSuricataBaseFileName",rulesForSuricataBaseFileName);
		//		j.set("rulesForSuricataLearnedFileName",rulesForSuricataLearnedFileName);
		//		j.set("variablesFileName",variablesFileName);
		//
		//		j.set("learnedRulesFileName",learnedRulesFileName);
		//		j.set("newLearnedRulesFileName",newLearnedRulesFileName);
		j.set("dataDir",dataDir);

		j.set(PACKETS_DIR_FOR_DETECTION_OFFLINE,packets_dir_for_detection_offline);
		j.set(PACKETS_DIR_FOR_RECORDING,packets_dir_for_recording );
		j.set(PACKETS_DIR_FOR_LEARNING,packets_dir_for_learning );

		//j.set("syslearnUpgradeExistingModels",syslearnUpgradeExistingModels );

		j.set("subdir_learn",subdir_learn );
		j.set("subdir_learn", subdir_learn);


		return j;

	}



	public String getPackets_dir_for_detection_offline() {
		return packets_dir_for_detection_offline;
	}



	public String getPackets_dir_for_recording() {
		return packets_dir_for_recording;
	}



	public String getPackets_dir_for_learning() {
		return packets_dir_for_learning;
	}

	public String getFullPackets_dir_for_detection_offline() {
		//		if (currentDataSetNameForDetectionOffLine.isEmpty()) 
		//			currentDataSetNameForDetectionOffLine=datasetManager.findDataSetName(packets_dir_for_detection_offline);
		//		return packets_dir_for_detection_offline+IDSUtil.fileSeparator+currentDataSetNameForDetectionOffLine;
		int category=IDSDataSetManager.DETECTION_OFFLINE;

		return datasetManager.getDirOfCategory(category)+
				IDSUtil.fileSeparator+
				datasetManager.getCurrentdataSetOfCategory(category);

	}



	public String getFullPackets_dir_for_recording() {
		//		if (currentDataSetNameForRecording.isEmpty()) 
		//			currentDataSetNameForRecording=datasetManager.findDataSetName(packets_dir_for_recording);
		//		return packets_dir_for_recording+IDSUtil.fileSeparator+currentDataSetNameForRecording;
		int category=IDSDataSetManager.RECORDING;

		return datasetManager.getDirOfCategory(category)+
				IDSUtil.fileSeparator+
				datasetManager.getCurrentdataSetOfCategory(category);
	}



	public String getFullPackets_dir_for_learning() {
		//		if (currentDataSetNameForLearning.isEmpty()) 
		//			currentDataSetNameForLearning=datasetManager.findDataSetName(packets_dir_for_learning);
		//		return packets_dir_for_learning+IDSUtil.fileSeparator+currentDataSetNameForLearning;
		int category=IDSDataSetManager.LEARNING;

		String dir= datasetManager.getDirOfCategory(category)+
				IDSUtil.fileSeparator+
				datasetManager.getCurrentdataSetOfCategory(category);
		return dir;
	}


	public IDSDataSetManager getDatasetManager() {
		return datasetManager;
	}





	public boolean isKillPreviousInstance() {
		return killPreviousInstance;
	}


	public boolean isExistingDir(String d) {


		if (idsMainProcessor.getFileStoreServices().dirExists(d)) return true;
		System.out.println("Cannot find working dir "+d);

		return false;
	}

	private String clean(String s) {
		String z="../";
		while (s.indexOf(z)>=0) {
			int n= s.indexOf(z);
			String s1=s.substring(0,n);
			String s2=s.substring(n+z.length(),s.length());
			s=s1+s2;
		}
		return s;
	}


	public String getDataDir() {
		return dataDir;
	}


	public void setDataDir(String dataDir) {
		this.dataDir = dataDir;
	}

	public String buildFullPathInDataDir(String dir) {

		if (dir==null) dir ="";
		dir=dir.replace('\\','/');

		dir=clean(dir);

		if (dir.startsWith(".")) dir =dir.substring(1);
		if (dir.startsWith(IDSUtil.fileSeparator)) dir =dir.substring(1);

		return getDataDir()+IDSUtil.fileSeparator+dir;
	}

	//	public void initFromJson(Json j, 
	//			//String pworkingDir,
	//			String dataDir) {
	//		initFromJson(j,
	//				//pworkingDir, 
	//				dataDir, false);
	//	}

	public void initFromJson(Json j, 
			//String pworkingDir, 
			String dataDir, boolean testParam, boolean doNotUseCurrentIDSParamsFileName ) { ///*String paramsFile,*/ String pworkingDir,String dirOfConfigFile) {


		setTestParam(testParam);
		//String pworkingDir = cslRunningArgs.getworkingDir();

		on=JsonUtil.getBooleanFromJson(j, "ids_conf/on",false);

		this.doNotUseCurrentIDSParamsFileName=doNotUseCurrentIDSParamsFileName;

		//this.dirOfConfigFile=cslRunningArgs.getPathOfConfigFile(); //dirOfConfigFile;

		//userDir=System.getProperty("user.dir");
		if (!dataDir.isEmpty()) 
			setDataDir(dataDir);
		else {
			dataDir=defaultDataDir;
		}

		datasetManager = new IDSDataSetManager(this);


		//Json jParams= Json.object();
		//if (!paramsFile.isEmpty()) jParams=JsonUtil.readFileAsJson(paramsFile);

		//		if (pworkingDir.isEmpty()) {
		//			workingDir=JsonUtil.getStringFromJson(j, "ids_conf/rules_for_detection","rulesForDetection.txt");
		//		}

		/*String defaultDir="";
		if (pworkingDir.isEmpty()) {
			defaultDir=JsonUtil.getStringFromJson(j, "ids_conf/working_dir",".");
		}
		else {
			defaultDir=pworkingDir;
		}

		if (defaultDir.startsWith(".")) {
			defaultDir=userDir+defaultDir.substring(1);
		}*/

		//		if (on) {
		//		File dir= new File(IDSParams.instance.getWorkingDir());
		//		if (!dir.exists()) {
		//			if (!dir.isDirectory()) {
		//				System.out.println("Cannot find working dir "+dir.getAbsolutePath());
		//				System.exit(0);
		//			}
		//		}
		//		}

		this.logToFile= 
				JsonUtil.getBooleanFromJson(j,  "ids_conf/log_to_file", true) ; // if not read only in the table

		this.sendToBrowser=JsonUtil.getBooleanFromJson(j,  "ids_conf/send_to_browser", false) ;
		this.sendToConsole=JsonUtil.getBooleanFromJson(j,  "ids_conf/send_to_console", false) ;

		
		//this.relative_to_CSLConfig_dir=JsonUtil.getBooleanFromJson(j, "ids_conf/relative_to_CSLConfig_dir", false);

		//		String prefix ="";
		//		if  (relative_to_CSLConfig_dir) {
		//			prefix = cslRunningArgs.getPathOfConfigFile()+IDSUtil.fileSeparatorChar;
		//		}

		//String s=JsonUtil.getStringFromJson(j, "ids_conf/idsconf_dir","idsconf");
		//setIdsModelDir(fileUtils.buildFullPathInConfDir(
		//		JsonUtil.getStringFromJson(j, IDS_CONF_SEP+"idsconf_dir","idsconf")) );

		//dataDir=JsonUtil.getStringFromJson(j, "ids_conf/data_dir",defaultDir);
		//recordDir=JsonUtil.getStringFromJson(j, "ids_conf/record_dir",defaultDir);
		//		validationDataDir=JsonUtil.getStringFromJson(j, "ids_conf/validation_data_dir",defaultDir);		
		packets_dir_for_recording=buildFullPathInDataDir(
				JsonUtil.getStringFromJson(j, IDS_CONF_SEP+PACKETS_DIR_FOR_RECORDING, "./recorded_packets"));
		packets_dir_for_detection_offline=buildFullPathInDataDir(
				JsonUtil.getStringFromJson(j, IDS_CONF_SEP+PACKETS_DIR_FOR_DETECTION_OFFLINE, "./recorded_packets"));
		packets_dir_for_learning=buildFullPathInDataDir(
				JsonUtil.getStringFromJson(j, IDS_CONF_SEP+PACKETS_DIR_FOR_LEARNING, "./recorded_packets"));


		//		rulesForDetectionFileName=JsonUtil.getStringFromJson(j, IDS_CONF_SEP+RULES_FOR_DETECTION,"rulesForDetection.txt");
		//		rulesForLearningFileName=JsonUtil.getStringFromJson(j, IDS_CONF_SEP+RULES_FOR_LEARNING,"rulesForLearning.txt");
		//		rulesForSuricataBaseFileName=JsonUtil.getStringFromJson(j, IDS_CONF_SEP+"rules_for_suricata_base","rulesForSuricataBase.txt");
		//		rulesForSuricataLearnedFileName=JsonUtil.getStringFromJson(j, IDS_CONF_SEP+"rules_for_suricata_learned","rulesForSuricataLearned.txt");
		//
		//		variablesFileName=JsonUtil.getStringFromJson(j, IDS_CONF_SEP+"system_configuration","SystemConfiguration.json");
		//		learnedRulesFileName=JsonUtil.getStringFromJson(j, IDS_CONF_SEP+"learned_model","LearnedRules.json");
		//		newLearnedRulesFileName=JsonUtil.getStringFromJson(j, IDS_CONF_SEP+"new_learned_model","NewLearnedRules.json");
		//
		//		syslearnUpgradeExistingModels=JsonUtil.getBooleanFromJson(j, IDS_CONF_SEP+"syslearn_upgrade_existing_models",false);
		//		learnUpgradeExistingModel=JsonUtil.getBooleanFromJson(j, IDS_CONF_SEP+"learn_upgrade_existing_model",false);

		setIdsMode(JsonUtil.getIntFromJson(j, IDS_CONF_SEP+MODE,0));

		currentIDSParamsFileName=JsonUtil.getStringFromJson(j, IDS_CONF_SEP+"current_idsparams_file","CurrentIDSParams.json");

		subdir_learn=JsonUtil.getStringFromJson(j, IDS_CONF_SEP+"subdir_learn","");
		subdir_test=JsonUtil.getStringFromJson(j, IDS_CONF_SEP+"subdir_test","");

		killPreviousInstance = JsonUtil.getBooleanFromJson(j, IDS_CONF_SEP+"kill_previous_instance",true);

		//		Json listIDs=JsonUtil.getJson(j,  IDS_CONF_SEP+TAPS_ID);
		//		
		//		if (listIDs!=null) {
		//			if (listIDs.isArray()) {
		//			for (Json e:listIDs.asJsonList()) {
		//				if (e.isString())
		//					this.listTapIds.add(e.asString());
		//				}
		//			}
		//		}
		//		tapsDir=JsonUtil.getStringFromJson(j, IDS_CONF_SEP+TAPS_DIR,"taps");



		loadCurrentVariables();



		validDirectories();

		System.out.println(this);
	}




	public void validDirectories() {

		packets_dir_for_detection_offline=checkAndCreateDir(packets_dir_for_detection_offline);
		packets_dir_for_learning=checkAndCreateDir(packets_dir_for_learning);
		packets_dir_for_recording=checkAndCreateDir(packets_dir_for_recording);

	}

	public String checkAndCreateDir(String s) {


		return idsMainProcessor.getFileStoreServices().checkAndCreateDir(s);


	}

	public String getCurrentIDSParamsFileName() {
		return currentIDSParamsFileName;
	}




	public boolean isOn() {
		return on;
	}

	//	public String getWorkingDir() {
	//		// TODO Auto-generated method stub
	//		
	//		if (isDetectOffLineMode()) {
	//			if (!subdir_test.isEmpty()) return workingDir+IDSUtil.fileSeparatorChar+subdir_test;
	//		}
	//		if (isLearningMode()) {
	//			if (!subdir_learn.isEmpty()) return workingDir+IDSUtil.fileSeparatorChar+subdir_learn;
	//		}
	//		return workingDir;
	//	}


	//void getSystemConfig() {
	//		irs.getIdsVariables();
	//	}

	public int getIDSMode() {
		return getIdsMode();
	}

	public String getIDSModeAsString() {
		return getModeStr(getIdsMode());
	}

	private String getModeStr(int i) {		
		if ((i>=0)&&(i<idsModeAsString.length)) return idsModeAsString[i];
		return "UNDEF";
	}

	public void setIDSMode(String s) {

		for (int i=0;i<idsModeAsString.length;i++) {
			if (s.compareToIgnoreCase(idsModeAsString[i])==0) {
				setIDSMode(i);
				return;
			}
		}
		setIDSMode(0);
	}

	public void setIDSMode(int iDSMode) {
		if (iDSMode<0) iDSMode=0;
		if (iDSMode>4) iDSMode=0;

		setIdsMode( iDSMode);
		saveCurrentVariables();
	}


	public boolean isIdleMode() {
		return getIDSMode()==MODE_IDLE;
	}
	public IDSParams setIdleMode() {
		setIDSMode(MODE_IDLE);
		return this;
	}

	public boolean isRecordOnlyMode() {
		return getIDSMode()==MODE_RECORD_ONLY;
	}
	public IDSParams setRecordOnlyMode() {
		setIDSMode(MODE_RECORD_ONLY);
		return this;
	}

	public boolean isDetectOnLineMode() {
		return getIDSMode()==MODE_DETECT_ONLINE;
	}	
	public IDSParams setDetectOnLineMode() {
		setIDSMode(MODE_DETECT_ONLINE);
		return this;
	}


	public boolean isLearningMode() {
		return getIDSMode()==MODE_LEARN;
	}
	public IDSParams setLearningMode() {
		setIDSMode(MODE_LEARN);
		return this;
	}


	public boolean isDetectOffLineMode() {
		return getIDSMode()==MODE_DETECT_OFFLINE;
	}

	public IDSParams setDetectOffLineMode() {
		setIDSMode(MODE_DETECT_OFFLINE);
		return this;
	}









	public boolean isRunOnLineRecordOrDetect() {
		//return runOnLineRecordOrDetect;
		//System.out.println("JMFMODE="+getIDSMode());
		return isRecordOnlyMode()||isDetectOnLineMode();
	}



	public boolean isLogToFile() {
		return logToFile;
	}



	public void setLogToFile(boolean b) {
		this.logToFile = b;
		saveCurrentVariables();
	}



	public boolean isSendToBrowser() {
		return sendToBrowser;
	}



	public void setSendToBrowser(boolean b) {
		this.sendToBrowser = b;
		saveCurrentVariables();
	}



	public boolean isSendToConsole() {
		return sendToConsole;
	}



	public void setSendToConsole(boolean b) {
		this.sendToConsole = b;
		saveCurrentVariables();
	}



	public String getCurrentDataSetNameForDetectionOffLine() {
		return currentDataSetNameForDetectionOffLine;
	}



	public void setCurrentDataSetNameForDetectionOffLine(String currentDataSetNameForDetectionOffLine) {
		this.currentDataSetNameForDetectionOffLine = currentDataSetNameForDetectionOffLine;
		saveCurrentVariables();
	}



	public String getCurrentDataSetNameForRecording() {
		return currentDataSetNameForRecording;
	}



	public void setCurrentDataSetNameForRecording(String currentDataSetNameForRecording) {
		this.currentDataSetNameForRecording = currentDataSetNameForRecording;
		saveCurrentVariables();
	}



	public String getCurrentDataSetNameForLearning() {
		return currentDataSetNameForLearning;
	}



	public void setCurrentDataSetNameForLearning(String currentDataSetNameForLearning) {
		this.currentDataSetNameForLearning = currentDataSetNameForLearning;
		saveCurrentVariables();
	}

	public void setPackets_dir_for_detection_offline(String packets_dir_for_detection_offline) {
		this.packets_dir_for_detection_offline = packets_dir_for_detection_offline;
	}

	public void setPackets_dir_for_recording(String packets_dir_for_recording) {
		this.packets_dir_for_recording = packets_dir_for_recording;
	}

	public void setPackets_dir_for_learning(String packets_dir_for_learning) {
		this.packets_dir_for_learning = packets_dir_for_learning;
	}




	//	public void setRunOnLineRecordOrDetect(boolean runOnLineRecordOrDetect) {
	//		this.runOnLineRecordOrDetect = runOnLineRecordOrDetect;
	//	}


	


	public void setShowReceivedObject(boolean showReceivedObject) {
		// TODO Auto-generated method stub
		this.showReceivedObject=showReceivedObject;
	}

	//public boolean isSendToConsole() {
	//	return sendToConsole;
	//}
	//
	//public void setSendToConsole(boolean sendToConsole) {
	//	this.sendToConsole = sendToConsole;
	//}
	//
	//public boolean isSendToBrowser() {
	//	return sendToBrowser;
	//}
	//
	//public void setSendToBrowser(boolean sendToBrowser) {
	//	this.sendToBrowser = sendToBrowser;
	//}

	public boolean isShowReceivedObject() {
		return showReceivedObject;
	}


}
