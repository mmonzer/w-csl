package com.ucsl.interfaces;

import com.ucsl.json.Json;

import java.util.List;

public interface IIDSMainProcessor {

	void setAlertFactory(IAlertFactory alertFactory);

	void removeAlertFromModel(IAlertDescriptor a, int i);
	void addAlertToModel(IAlertDescriptor a, int level);
	
	
	IAlertFactory getAlertFactory();

	void processPacket(Json jj);
	void processVariables(Json jj);

	void init();

	void execSysStateRules(long systemCurrentTimeMillis);

	void processSuricataEvent(Json jj);

	void setFileLogFactory(IFileLogFactory fileLogFactory);

	void setFileStoreServices(IFileStoreService fileUtils);

	void setAlertManager(IAlertManager cslAlertManager);

	Json readJsonFromModelDir(String string, String fileName);

	void saveJsonInModelDir(String string, String fileName, Json j);

	IFileStoreService getFileStoreServices();

	void setConsole(IConsole console);

	IIDSLearnedRules getLearnedModelFromFile();

	IILearningProcessor getLogAnalyzer(String fullPackets_dir_for_learning, boolean learnNetwork, boolean learnSysModel);
	IOffLineDetectionProcessor getOffLineDetectionProcessor(String fullPackets_dir_for_detection_offline);

	void renameLearnedRulesWithTimeStamp();

	String getDirAndFileNamesInfo();

	void saveLearnedModelAsNewLearnedModel();

	IAlertSender getAlertManager();

	Json getIDSVariables();

	String getProcessVariables();


	List<String> getErrors();

	String getIdsRulesSetAsString();

	IIDSLearnedRules getCurrentLearnedModel();

	Json getLearnedRules();

	IIDSMainProcessorParams getIdsMainProcessorParams();

	void resetLearnedModel();

	void backupLearnedModel();

	void reverseBackupLearnedModel();

	void getParamsAsJsonNameValueArray(Json j);

	
	


	

}
