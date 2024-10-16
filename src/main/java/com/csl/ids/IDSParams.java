package com.csl.ids;

import com.csl.core.CSLContext;
import com.ucsl.json.Json;
import com.wcsl.ids.IDSMainProcessor;
import lombok.Getter;
import lombok.Setter;

import java.io.File;

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
    private String dataDir=CSLContext.instance.getUserDir()+ File.separator+"idsdata";
	private final String defaultDataDir=CSLContext.instance.getUserDir()+File.separator+"idsdata";

    int idsMode=0;

	boolean logToFile=true;
    boolean sendToBrowser=true;
    boolean sendToConsole=false;

	private String subdir_learn="";

    IDSDataSetManager datasetManager = new IDSDataSetManager(this);

    IDSConsole console= new IDSConsole(this);

    String packets_dir_for_detection_offline="";
    String packets_dir_for_recording="";
    String packets_dir_for_learning="";
    String currentDataSetNameForDetectionOffLine="";
    String currentDataSetNameForRecording="";
    String currentDataSetNameForLearning="";
    private String currentIDSParamsFileName="";

	private IDSMainProcessor idsMainProcessor;
    private boolean showReceivedObject;
	private boolean testmode=false;
	private boolean testparam=false;

	public int getIdsMode() {return idsMode;}

	public IDSParams(IDSMainProcessor idsMainProcessor) {
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
