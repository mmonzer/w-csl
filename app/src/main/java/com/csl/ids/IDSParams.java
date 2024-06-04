package com.csl.ids;

import com.csl.core.CSLContext;
import com.ucsl.interfaces.IConsole;
import com.ucsl.interfaces.IIDSMainProcessor;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import com.ucsl.util.IDSUtil;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IDSParams {

	public static final String MODE = "mode";
	public static final String PACKETS_DIR_FOR_LEARNING = "packets_dir_for_learning";
	public static final String PACKETS_DIR_FOR_DETECTION_OFFLINE = "packets_dir_for_detection_offline";
	public static final String PACKETS_DIR_FOR_RECORDING = "packets_dir_for_recording";

	public static final String IDS_CONF = "ids_conf";
	private static final String IDS_CONF_SEP = IDS_CONF+"/";

	public static int MODE_IDLE=0;
	public static int MODE_RECORD_ONLY=1;
	public static int MODE_DETECT_ONLINE=2;
	public static int MODE_LEARN=3;
	public static int MODE_DETECT_OFFLINE=4;

	public static String[] idsModeAsString= new String[] {"IDLE", "RECORDING", "ONLINE_DETECTION","LEARNING","OFF_LINE_DETECTION"};
    boolean on=false;

	boolean doNotUseCurrentIDSParamsFileName=false;

	Json jCurrentVariables = Json.object();
    private String dataDir=CSLContext.instance.getUserDir()+IDSUtil.fileSeparator+"idsdata";
	private final String defaultDataDir=CSLContext.instance.getUserDir()+IDSUtil.fileSeparator+"idsdata";

    int idsMode=0;

	boolean logToFile=true;
    boolean sendToBrowser=true;
    boolean sendToConsole=false;

	private String subdir_learn="";

    IDSDataSetManager datasetManager = new IDSDataSetManager(this);

    IConsole console= new IDSConsole(this);

    String packets_dir_for_detection_offline="";
    String packets_dir_for_recording="";
    String packets_dir_for_learning="";
    String currentDataSetNameForDetectionOffLine="";
    String currentDataSetNameForRecording="";
    String currentDataSetNameForLearning="";
    private String currentIDSParamsFileName="";

	private IIDSMainProcessor idsMainProcessor;
    private boolean showReceivedObject;
	private boolean testmode=false;
	private boolean testparam=false;

	public int getIdsMode() {return idsMode;}

	public void setIdsMode(int idsMode) {
		this.idsMode = idsMode;
	}

	public IDSParams(IIDSMainProcessor idsMainProcessor) {
		this.idsMainProcessor=idsMainProcessor;
    }

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
		String fileName=		getCurrentIDSParamsFileName();
		jCurrentVariables=idsMainProcessor.readJsonFromModelDir("", fileName);

		currentDataSetNameForDetectionOffLine=JsonUtil.getStringFromJson(jCurrentVariables,"currentDataSetNameForDetectionOffLine", "");
		currentDataSetNameForRecording=JsonUtil.getStringFromJson(jCurrentVariables,"currentDataSetNameForRecording", "");
		currentDataSetNameForLearning=JsonUtil.getStringFromJson(jCurrentVariables,"currentDataSetNameForLearning", "");

		boolean b=JsonUtil.getBooleanFromJson(jCurrentVariables,"sendToBrowser", sendToBrowser);
		setSendToBrowser(b);

		b=JsonUtil.getBooleanFromJson(jCurrentVariables,"sendToConsole", sendToBrowser);
		setSendToConsole(b);

		String s=JsonUtil.getStringFromJson(jCurrentVariables,"idsMode", "");
		setIDSMode(s);

	}

	public void saveCurrentVariables() {
		if (doNotUseCurrentIDSParamsFileName) return;
		String fileName=		getCurrentIDSParamsFileName();

		Json j= Json.object();

		j.set("currentDataSetNameForDetectionOffLine",currentDataSetNameForDetectionOffLine );
		j.set("currentDataSetNameForRecording", currentDataSetNameForRecording);
		j.set("currentDataSetNameForLearning", currentDataSetNameForLearning);
		j.set("sendToBrowser", sendToBrowser);
		j.set("sendToConsole", sendToConsole);
		j.set("idsMode", getIDSModeAsString());
		idsMainProcessor.saveJsonInModelDir("", fileName, j);
	}

	public boolean isTestMode() {
		return testmode;
	}

	public boolean getTestParam() {
		return testparam;
	}

	public void setTestParam(boolean testparam) {
		this.testparam = testparam;
	}

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

    public Json getAsJson() {
		Json j = Json.object();

		j.set("on", on);
		j.set("IDSMode", getIdsMode());

		j.set("dataDir",dataDir);

		j.set(PACKETS_DIR_FOR_DETECTION_OFFLINE,packets_dir_for_detection_offline);
		j.set(PACKETS_DIR_FOR_RECORDING,packets_dir_for_recording );
		j.set(PACKETS_DIR_FOR_LEARNING,packets_dir_for_learning );


		j.set("subdir_learn",subdir_learn );
		j.set("subdir_learn", subdir_learn);

		return j;

	}

    public String getFullPackets_dir_for_detection_offline() {
		int category=IDSDataSetManager.DETECTION_OFFLINE;

		return datasetManager.getDirOfCategory(category)+
				IDSUtil.fileSeparator+
				datasetManager.getCurrentdataSetOfCategory(category);
	}

	public String getFullPackets_dir_for_recording() {
		int category=IDSDataSetManager.RECORDING;

		return datasetManager.getDirOfCategory(category)+
				IDSUtil.fileSeparator+
				datasetManager.getCurrentdataSetOfCategory(category);
	}

	public String getFullPackets_dir_for_learning() {
		int category=IDSDataSetManager.LEARNING;

		String dir= datasetManager.getDirOfCategory(category)+
				IDSUtil.fileSeparator+
				datasetManager.getCurrentdataSetOfCategory(category);
		return dir;
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

    public String buildFullPathInDataDir(String dir) {

		if (dir==null) dir ="";
		dir=dir.replace('\\','/');

		dir=clean(dir);

		if (dir.startsWith(".")) dir =dir.substring(1);
		if (dir.startsWith(IDSUtil.fileSeparator)) dir =dir.substring(1);

		return getDataDir()+IDSUtil.fileSeparator+dir;
	}

	public void initFromJson(Json j, String dataDir, boolean testParam, boolean doNotUseCurrentIDSParamsFileName ) {

		setTestParam(testParam);
		on=JsonUtil.getBooleanFromJson(j, "ids_conf/on",false);
		this.doNotUseCurrentIDSParamsFileName=doNotUseCurrentIDSParamsFileName;

		if (!dataDir.isEmpty()) 
			setDataDir(dataDir);
		else {
			dataDir=defaultDataDir;
		}

		datasetManager = new IDSDataSetManager(this);

		this.logToFile= 
				JsonUtil.getBooleanFromJson(j,  "ids_conf/log_to_file", true) ; // if not read only in the table

		this.sendToBrowser=JsonUtil.getBooleanFromJson(j,  "ids_conf/send_to_browser", false) ;
		this.sendToConsole=JsonUtil.getBooleanFromJson(j,  "ids_conf/send_to_console", false) ;

		packets_dir_for_recording=buildFullPathInDataDir(
				JsonUtil.getStringFromJson(j, IDS_CONF_SEP+PACKETS_DIR_FOR_RECORDING, "./recorded_packets"));
		packets_dir_for_detection_offline=buildFullPathInDataDir(
				JsonUtil.getStringFromJson(j, IDS_CONF_SEP+PACKETS_DIR_FOR_DETECTION_OFFLINE, "./recorded_packets"));
		packets_dir_for_learning=buildFullPathInDataDir(
				JsonUtil.getStringFromJson(j, IDS_CONF_SEP+PACKETS_DIR_FOR_LEARNING, "./recorded_packets"));

		setIdsMode(JsonUtil.getIntFromJson(j, IDS_CONF_SEP+MODE,0));

		currentIDSParamsFileName=JsonUtil.getStringFromJson(j, IDS_CONF_SEP+"current_idsparams_file","CurrentIDSParams.json");

		subdir_learn=JsonUtil.getStringFromJson(j, IDS_CONF_SEP+"subdir_learn","");

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

	public boolean isRecordOnlyMode() {
		return getIDSMode()==MODE_RECORD_ONLY;
	}

	public boolean isDetectOnLineMode() {
		return getIDSMode()==MODE_DETECT_ONLINE;
	}

	public boolean isLearningMode() {
		return getIDSMode()==MODE_LEARN;
	}

	public boolean isDetectOffLineMode() {
		return getIDSMode()==MODE_DETECT_OFFLINE;
	}

	public boolean isRunOnLineRecordOrDetect() {
		return isRecordOnlyMode()||isDetectOnLineMode();
	}

    public void setSendToBrowser(boolean b) {
		this.sendToBrowser = b;
		saveCurrentVariables();
	}

    public void setSendToConsole(boolean b) {
		this.sendToConsole = b;
		saveCurrentVariables();
	}

    public void setCurrentDataSetNameForDetectionOffLine(String currentDataSetNameForDetectionOffLine) {
		this.currentDataSetNameForDetectionOffLine = currentDataSetNameForDetectionOffLine;
		saveCurrentVariables();
	}

    public void setCurrentDataSetNameForRecording(String currentDataSetNameForRecording) {
		this.currentDataSetNameForRecording = currentDataSetNameForRecording;
		saveCurrentVariables();
	}

    public void setCurrentDataSetNameForLearning(String currentDataSetNameForLearning) {
		this.currentDataSetNameForLearning = currentDataSetNameForLearning;
		saveCurrentVariables();
	}

}
