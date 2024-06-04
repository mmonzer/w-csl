package com.ucsl.util;

import com.ucsl.interfaces.ICSLLogger;

public class DefaultLogger implements ICSLLogger {

	int loglevel=0;
	
	// 4
	@Override
	public void logFatal(String msg) {
		if (loglevel>=4) System.err.println("[FATAL]"+msg);
	}

	// 3
	@Override
	public void logError(String msg) {
		if (loglevel>=3) System.err.println("[ERROR]"+msg);

	}

	// 2
	@Override
	public void logWarn(String msg) {
		if (loglevel>=2) System.err.println("[ERROR]"+msg);

	}

	@Override
	public void logDebug(String msg) {
		// TODO Auto-generated method stub
		if (loglevel>=1) System.out.println("[DEBUG]"+msg);

	}
	
	@Override
	public void logInfo(String msg) {
		if (loglevel>=0) System.out.println("[INFO ]"+msg);

	}
	
	@Override
	public void setLogLevel(int level) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void printError(String e) {
		// TODO Auto-generated method stub
		System.err.println(e);
	}

}
