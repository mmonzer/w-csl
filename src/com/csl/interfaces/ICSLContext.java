package com.csl.interfaces;



import com.xcsl.json.Json;

public interface ICSLContext {
	
	
	public long getTimeFromStartingTime();
	public int getSamplingTime(); // in ms
	
	public long getSystemCurrentTimeMillis();
	
	Json getConfig();
	
	//Json takeObjectFromInputQueue(int n);								// the number of inputs is defined in config
	//void putObjectToOutputQueue(int n, Json j);
	
	public void logFatal(String msg);
	public void logError(String msg);
	public void logWarn(String msg);
	public void logInfo(String msg);
	public void logDebug(String msg);
	public void setLogLevel(int level);
	
	
	
	
		
	

}
