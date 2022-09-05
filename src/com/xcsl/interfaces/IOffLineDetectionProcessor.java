package com.xcsl.interfaces;

import java.util.List;

import com.xcsl.ids.IDSVariables;
import com.xcsl.ids.rules.IDSRulesSet;
import com.xcsl.learning.IDSLearnedRules;

public interface IOffLineDetectionProcessor {

	
	
	public boolean isVerbose();
	public void setVerbose(boolean verbose);

	public IOffLineDetectionProcessor setICSLFileFactory(ICSLFileFactory fileFactory);
	public IOffLineDetectionProcessor setCancelChecker(ICancelChecker cancelChecker) ;
	
	
	public IOffLineDetectionProcessor setDirToProcess(String dirToProcess);
	
	public List<String> getIDSRulesErrors();
	
	
	public boolean doDetection();
	public IDSLearnedRules getIdsNewLearnedRules();
	public void saveNewLearnedRules();
	
	public void setAutoSaveLearnModel(boolean b);
	public void setAutoBackupLearnModel(boolean b);
	
	
	public void setOnFinished(Runnable runnable);
	
	public boolean hasIDSRulesCompileErrors();
	IDSRulesSet getIdsRulesSet();
	IDSVariables getIdsVariables();
	String getProcessVariables();
	IDSLearnedRules getLearnedRules();
	
	
	
}
