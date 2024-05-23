package com.wcsl.ids;

import java.util.List;
import java.util.Map;

import com.csl.alert.CSLAlertFactory;
import com.csl.alert.CSLAlertManager;
import com.csl.core.CSLContext;
import com.csl.defaultclasses.FileLogFactory;
import com.csl.defaultclasses.FileStoreService;
import com.ucsl.interfaces.IAlertDescriptor;
import com.ucsl.interfaces.IAlertFactory;
import com.ucsl.interfaces.IAlertLevel;
import com.ucsl.interfaces.IAlertManager;
import com.ucsl.interfaces.IAlertSender;
import com.ucsl.interfaces.ICSLLogger;
import com.ucsl.interfaces.IConsole;
import com.ucsl.interfaces.IFileLogFactory;
import com.ucsl.interfaces.IFileStoreService;
import com.ucsl.interfaces.IIDSLearnedRules;
import com.ucsl.interfaces.IIDSMainProcessor;
import com.ucsl.interfaces.IIDSMainProcessorParams;
import com.ucsl.interfaces.IILearningProcessor;
import com.ucsl.interfaces.IOffLineDetectionProcessor;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import com.ucsl.util.DefaultLogger;
import com.ucsl.util.IDSUtil;
import lombok.Getter;
import lombok.Setter;


public class IDSMainProcessor implements IIDSMainProcessor {
	
	private IFileStoreService fileStoreServices;

	private IIDSMainProcessorParams idsMainProcessorParams;

	private Json config;

	private IFileLogFactory fileLogFactory;

	private IAlertManager alertManager;

	private IAlertFactory alertFactory;

	private long currentTime;

	
	//=======================================================================================================================
	// Logger
	//
	@Setter
    @Getter
    static private ICSLLogger logger= new DefaultLogger();
	static public ICSLLogger cslLogger() {
		return logger;
	}

    //=======================================================================================================================
	public IDSMainProcessor(
			Json jConfig,
			String cslConfDir
			) 
			{
	
		
		this.fileStoreServices= new FileStoreService(cslConfDir);
		
		
		this.config=jConfig;

		this.fileLogFactory=new FileLogFactory();

		this.alertManager=getDefaultAlertManager();


		this.idsMainProcessorParams= new IDSMainProcessorParams(this, jConfig);

		this.alertFactory= new CSLAlertFactory();

	}
	
	public IAlertManager getDefaultAlertManager() {

		Json j= Json.object();
		return new CSLAlertManager(this,j);
	}
	
	@Override
	public void setAlertFactory(IAlertFactory alertFactory) {
		// TODO Auto-generated method stub
		this.alertFactory=alertFactory;
	}
	
	@Override
	public IAlertFactory getAlertFactory() {
		// TODO Auto-generated method stub
		return alertFactory;
	}

	
	
	@Override
	public void init() {
		// TODO Auto-generated method stub
		
	}

	//==================================================================================
	// Adavanced version
	@Override
	public void processPacket(Json jj) {
	}
	@Override
	public void processVariables(Json jj) {
	}
	@Override
	public void execSysStateRules(long systemCurrentTimeMillis) {
	}
	//==================================================================================
	

	@Override
	public void processSuricataEvent(Json event) {
		// TODO Auto-generated method stub
		updateTime(event);
		generateAlertFRomSuricataEvent(event);
	}
	
	
	@Override
	public void setFileLogFactory(IFileLogFactory fileLogFactory) {
		this.fileLogFactory=fileLogFactory;
		
	}
	@Override
	public void setFileStoreServices(IFileStoreService fileUtils) {
		// TODO Auto-generated method stub
		this.fileStoreServices=fileUtils;
		
	}
	@Override
	public void setAlertManager(IAlertManager cslAlertManager) {
		// TODO Auto-generated method stub
		this.alertManager=cslAlertManager;
	}
	
	
	@Override
	public void saveJsonInModelDir(String dir,String fileName,Json j) {
		if (!dir.isEmpty())
			dir =idsMainProcessorParams.getIdsModelDir()+IDSUtil.fileSeparator+dir;
		getFileStoreServices().saveJsonToFile(dir, fileName,j);
	}

	@Override
	public Json readJsonFromModelDir(String dir, String fileName) {
		if (!dir.isEmpty())
			dir =idsMainProcessorParams.getIdsModelDir()+IDSUtil.fileSeparator+dir;
		return getFileStoreServices().readJsonFromFile(dir, fileName);
	}
	
	@Override
	public IFileStoreService getFileStoreServices() {
		// TODO Auto-generated method stub
		return fileStoreServices;
	}
	@Override
	public void setConsole(IConsole console) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public IAlertSender getAlertManager() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public IIDSMainProcessorParams getIdsMainProcessorParams() {
		
		return idsMainProcessorParams;
		}
	
	// process an event (from suricata for example)
	private void generateAlertFRomSuricataEvent(Json evtsInfo) {
		
		// test pour eve event
		
		boolean verbose=true;
		if (verbose) System.out.println("IDS Processing Info  "+evtsInfo);
		String code="#suricata_alert";
		String msg="#undef";

		if (evtsInfo.has("alert")) {

			Json j=evtsInfo.get("alert");
			if (j.has("signature")) {

				String s=j.get("signature").asString();
				if (s.startsWith("#")) {
					int p=s.indexOf(" ");
					if (p<0) {
						msg=s;
					}
					else {
						code=s.substring(1,p);
						msg=s.substring(p+1,s.length());
					}
				}
				else msg=s;
			}

			if (j.has("category")) {
				evtsInfo.set("category", j.get("category").asString());
			}
			else 
				evtsInfo.set("category", "#undef");


			if (j.has("severity")) {
				evtsInfo.set("severity", j.get("severity").asString());
			}
			else 
				evtsInfo.set("severity", "0");


			evtsInfo.set("msg", msg);
			evtsInfo.set("code", code);

			if (verbose) System.out.println("Suricita alert code <"+code+">:"+ msg);
			if (verbose) System.out.println(JsonUtil.prettyPrint(evtsInfo));

			//if (evtsInfo.has("msg"))
			Json base_info = Json.object();
			base_info.set("timestamp", evtsInfo.at("timestamp"));
			base_info.set("flow_id", evtsInfo.at("flow_id"));
			base_info.set("in_iface", evtsInfo.at("in_iface"));
			base_info.set("event_type", evtsInfo.at("event_type"));
			base_info.set("src_ip", evtsInfo.at("src_ip"));
			base_info.set("src_port", evtsInfo.at("src_port"));
			base_info.set("dest_ip", evtsInfo.at("dest_ip"));
			base_info.set("dest_port", evtsInfo.at("dest_port"));
			base_info.set("proto", evtsInfo.at("proto"));
			//ajouter erxtra info for suricata ds alert
			
			IAlertDescriptor  alert=
					getAlertFactory().createAlertDescriptor(
							IAlertLevel.INFO.getLevelAsInt(),
							msg,
							getIDSCurrentTimeMillis()
							)
					.setProp("category",evtsInfo.get("category").asString())
					.setProp("severity", evtsInfo.get("severity").asString())
					.setMetaInfo("suricata_info", getEveInfo(j))
					.setMetaInfo("base_info", base_info);

			CSLContext.instance.getCSLAlertManager().sendAlert(alert);
		}
		else {

			System.out.println("Suricata EVE (not an alert)"+evtsInfo);
		}

	}
	
	private Json getEveInfo(Json jj) {
		
		Json result=Json.object();
		for (Map.Entry<String, Json> e : jj.asJsonMap().entrySet()) {
			String key=e.getKey();
			if ((key.compareTo("timestamp")!=0)
				&& (key.compareTo("type")!=0))
				
			result.set(key, e.getValue());
			
		}
		return result;
	}
	
	public long getIDSCurrentTimeMillis() {
		return   currentTime;//cuidsCurrentTimeMillis;

	}
	private void updateTime(Json j) {
		
		long t=JsonUtil.getLongFromJson(j, "timestamp", -1); //j.get("timestamp").asLong();  // coorect value of time
		if (t<0) {
			t=JsonUtil.getLongFromJson(j, "time", -1);
		}
		if (t<0) {
			cslLogger().printError("Invalid time in  :"+j);
		}
		else {
			if (this.currentTime>t) {
				cslLogger().printError("Invalid time in  :"+j+" t="+t+"  before last time:"+currentTime);
			}
			this.currentTime=t;
		}
	}
	
	//=======================================================================================================================
	// Model based
	//
	
	@Override
	public void removeAlertFromModel(IAlertDescriptor a, int i) {
		
	}
	@Override
	public void addAlertToModel(IAlertDescriptor a, int level) {
			
	}
	
	//=======================================================================================================================
	// Learning
	
	@Override
	public IIDSLearnedRules getLearnedModelFromFile() {
		System.err.println("Not implemented in basic version");
		return null;
	}
	@Override
	public IILearningProcessor getLogAnalyzer(String fullPackets_dir_for_learning, boolean learnNetwork,
			boolean learnSysModel) {
		System.err.println("Not implemented in basic version");
		return null;
	}

	@Override
	public IOffLineDetectionProcessor getOffLineDetectionProcessor(String fullPackets_dir_for_detection_offline) {
		System.err.println("Not implemented in basic version");
		return null;
	}
	@Override
	public void saveLearnedModelAsNewLearnedModel() {
		System.err.println("Not implemented in basic version");
		
	}
	
	
	@Override
	public Json getIDSVariables() {
		System.err.println("Not implemented in basic version");
		return null;
	}
	@Override
	public String getProcessVariables() {
		System.err.println("Not implemented in basic version");
		return null;
	}
	
	
	
	@Override
	public List<String> getErrors() {
		System.err.println("Not implemented in basic version");
		return null;
	}
	@Override
	public String getIdsRulesSetAsString() {
		System.err.println("Not implemented in basic version");
		return null;
	}
	@Override
	public IIDSLearnedRules getCurrentLearnedModel() {
		System.err.println("Not implemented in basic version");
		return null;
	}
	@Override
	public Json getLearnedRules() {
		System.err.println("Not implemented in basic version");		
		return null;
	}
	
	@Override
	public void resetLearnedModel() {
		System.err.println("Not implemented in basic version");
		
		
	}
	@Override
	public void backupLearnedModel() {
		System.err.println("Not implemented in basic version");
		
		
	}
	@Override
	public void reverseBackupLearnedModel() {
		System.err.println("Not implemented in basic version");
	}

	@Override
	public void renameLearnedRulesWithTimeStamp() {
		System.err.println("Not implemented in basic version");		
	}
	
	@Override
	public void getParamsAsJsonNameValueArray(Json j) {
		if (j==null) j=Json.array();
		if (!j.isArray()) j= Json.array();
	}
	
	@Override
	public String getDirAndFileNamesInfo() {
		// TODO Auto-generated method stub
		String s="";
		return s;
	}
}
