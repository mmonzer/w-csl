package com.ucsl.interfaces;

import java.util.List;



public interface IOffLineDetectionProcessor {

	
	
	public boolean isVerbose();
	public void setVerbose(boolean verbose);

	public IOffLineDetectionProcessor setICSLFileFactory(ICSLFileFactory fileFactory);
	public IOffLineDetectionProcessor setCancelChecker(ICancelChecker cancelChecker) ;
	
	
	public IOffLineDetectionProcessor setDirToProcess(String dirToProcess);
	
	public List<String> getIDSRulesErrors();
	
	
	public boolean doDetection();
	public IIDSLearnedRules getIdsNewLearnedRules();
	public void saveNewLearnedRules();
	
	public void setAutoSaveLearnModel(boolean b);
	public void setAutoBackupLearnModel(boolean b);
	
	
	public void setOnFinished(Runnable runnable);
	
	public boolean hasIDSRulesCompileErrors();
	IIDSRulesSet getIdsRulesSet();
//	IDSVariables getIdsVariables();
	String getProcessVariables();
	IIDSLearnedRules getLearnedRules();
	
	
	
}
