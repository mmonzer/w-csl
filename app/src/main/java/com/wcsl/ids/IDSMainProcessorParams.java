package com.wcsl.ids;

import java.util.ArrayList;
import java.util.List;

import com.ucsl.interfaces.ICancelChecker;
import com.ucsl.interfaces.IConsole;
import com.ucsl.interfaces.IFileStoreService;
import com.ucsl.interfaces.IIDSMainProcessor;
import com.ucsl.interfaces.IIDSMainProcessorParams;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import com.ucsl.util.IDSUtil;

public class IDSMainProcessorParams implements IIDSMainProcessorParams {

	
	public static final String IDS_CONF = "";
	private static final String IDS_CONF_SEP =""; //  IDS_CONF+"/";
	
	
	public static String TAPS_ID="taps_id";
	public static String TAPS_DIR="taps_dir";
	
	
	
	

	
	private IIDSMainProcessor idsMainProcessor;
	private IFileStoreService fileUtils;

	
	List<String> listTapIds= new ArrayList<>();
	String tapsDir="";

	String rulesForSuricataBaseFileName="";
	
	
	


	public IDSMainProcessorParams(IDSMainProcessor idsMainProcessor, Json config) {
	
		this.idsMainProcessor = idsMainProcessor;
		fileUtils=idsMainProcessor.getFileStoreServices();
		initFromJson(config);
	}
	
	
	public IIDSMainProcessor getIdsMainProcessor() {
		return idsMainProcessor;
	}
	public void setIdsMainProcessor(IIDSMainProcessor idsMainProcessor) {
		this.idsMainProcessor = idsMainProcessor;
	}

	

	public IFileStoreService getFileUtils() {
		return fileUtils;
	}
	
	
	public String getIdsModelDirBackup() {
		return getIdsModelDir()+IDSUtil.fileSeparator+"backup";
	}

	public String getIdsModelDir() {
		return "";
	}


	
	
	public String getTapsDir() {
		return tapsDir;
	}
	
	public List<String> getTapsIDs() {
		return listTapIds;
	}
	
	
	
	
	public void initFromJson(Json j) {
		
		
		rulesForSuricataBaseFileName=JsonUtil.getStringFromJson(j, IDS_CONF_SEP+"rules_for_suricata_base","rulesForSuricataBase.txt");
		
		
		Json listIDs=JsonUtil.getJson(j,  IDS_CONF_SEP+TAPS_ID);
		
		if (listIDs!=null) {
			if (listIDs.isArray()) {
			for (Json e:listIDs.asJsonList()) {
				if (e.isString())
					this.listTapIds.add(e.asString());
				}
			}
		}
		tapsDir=JsonUtil.getStringFromJson(j, IDS_CONF_SEP+TAPS_DIR,"taps");



		System.out.println(this);
	}


	@Override
	public String getRulesForSuricataBaseFileName() {
		// TODO Auto-generated method stub
		return rulesForSuricataBaseFileName;
	}


	
}
