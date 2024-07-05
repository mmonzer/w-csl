package com.ucsl.interfaces;

public interface ICSLLogger {
	

	public void logFatal(String msg);
	public void logError(String msg);
	public void logWarn(String msg);
	public void logInfo(String msg);
	public void logDebug(String msg);
	public void setLogLevel(int level);
	
	
	
	public void printError(String e);
		
	

}
