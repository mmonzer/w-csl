package com.ucsl.interfaces;

import java.util.List;

//import com.xcsl.learning.IDSLearnedRules;

public interface ILearningProcessor {

	
	
	public boolean isVerbose();
	public void setVerbose(boolean verbose);

	public ILearningProcessor setICSLFileFactory(ICSLFileFactory fileFactory);
	public ILearningProcessor setCancelChecker(ICancelChecker cancelChecker) ;
	
	
	public ILearningProcessor setDirToProcess(String dirToProcess);
	
	public List<String> getPacketPreprocessingRulesErrors();
	
	
	public boolean doLearning();
	public IIDSLearnedRules getIdsLearnedRules();
	public void saveLearnedRules();
	
	public void setAutoSaveLearnModel(boolean b);
	public void setAutoBackupLearnModel(boolean b);
	
	
	public void setOnFinished(Runnable runnable);
	
	public boolean hasIDSRulesCompileErrors();
	
	
}
