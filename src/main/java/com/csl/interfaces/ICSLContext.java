package com.csl.interfaces;



import com.csl.core.Config;

public interface ICSLContext {
	
	
	public long getTimeFromStartingTime();
	
	public long getSystemCurrentTimeMillis();
	
	Config getConfig();

}
