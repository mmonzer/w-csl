package com.ucsl.interfaces;

public interface IILearningProcessor {

	void setDirToProcess(String fullPackets_dir_for_learning);

	void setVerbose(boolean verbose);

	void setCancelChecker(ICancelChecker iCancelChecker);

	boolean doLearning();

	String[] getPacketPreprocessingRulesErrors();

	String saveLearnedRules();

	

}
