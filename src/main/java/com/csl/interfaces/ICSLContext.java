package com.csl.interfaces;



import com.csl.core.Config;
import com.ucsl.json.Json;

public interface ICSLContext {
	
	
	public long getTimeFromStartingTime();
	
	public long getSystemCurrentTimeMillis();
	
	Config getConfig();
	public void logError(String msg);
	public void logInfo(String msg);
	
	
	
	
		
	

}
