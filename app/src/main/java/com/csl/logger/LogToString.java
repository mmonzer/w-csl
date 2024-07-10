package com.csl.logger;

public class LogToString implements ILoggerListener {

	StringBuffer content=new StringBuffer();
	
	@Override
	public void log(int level, String message) {
		// TODO Auto-generated method stub

		content.append(CSLLogger.label(level)+message);
	}

	
	public String getContents() {
		return content.toString();
	}
	
	public void clear() {
		content.setLength(0);
	}
}
