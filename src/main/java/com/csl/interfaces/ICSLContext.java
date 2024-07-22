package com.csl.interfaces;



import com.ucsl.json.Json;

public interface ICSLContext {
	
	
	public long getTimeFromStartingTime();
	
	public long getSystemCurrentTimeMillis();
	
	Json getConfig();
	public void logError(String msg);
	public void logInfo(String msg);
	
	
	
	
		
	

}
