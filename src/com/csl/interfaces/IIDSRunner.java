package com.csl.interfaces;

import com.csl.ids.IDSParams;
import com.ucsl.interfaces.IIDSLearnedRules;


public interface IIDSRunner {

	
	public void switchModeTo(int iDSMode);
	
	public void switchModeToIdle();
	public void switchModeToLearn();
	public void switchModeToDetectOffline();
	public void switchModeToDetectOnline();
	public void switchModeToRecording();

	
	public IDSParams getIdsParams();
	
	public boolean isIdsSendToBrowser();
	public void setIdsSendToBrowser(boolean b);
	
	public boolean isIdsSendToConsole();
	public void setIdsSendToConsole(boolean b);
	
	
	public String getIDSModeAsString();
	public int getIDSMode();
	public void setIDSMode(int mode);


	//public Json getLearnedRulesAsJson();
	IIDSLearnedRules getLearnedRules();

	public void start();

	public void stop();





	
	
	
}
