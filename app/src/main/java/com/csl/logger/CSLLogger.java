package com.csl.logger;

import java.util.ArrayList;
import java.util.List;


public class CSLLogger {

	static public CSLLogger instance= new CSLLogger();
	
	private static boolean DEBUG=false;
	
	public int levelToLog=2;
	
	List<ILoggerListener> listeners =new ArrayList<ILoggerListener>();

	private boolean debug;
	
	static private String[] labels= {"FATAL","LOG1","","ERROR","WARN ","INFO ","LOG6","LOG7","DEBUG"};
			
	
	static public String label(int l) {
		if (l<0) return "??log??";
		if (l>8) return "???log??";
		return '['+labels[l]+']';
	}
	
	public CSLLogger() {
		// TODO Auto-generated constructor stub
	}
	
	private void log(int level, String message) {
	
		if (level>levelToLog) return;
		if (!isDebug()&&(level==ILogLevel.DEBUG)) return; 
		if (level<=ILogLevel.ERROR) {
			System.err.println(label(level)+message);
		}
		else { 
			//System.out.println(label(level)+message);
		}
		
		for (ILoggerListener l:listeners) {
			l.log(level, message);
		}
	}

	
	public void fatal( String message) {
		log(ILogLevel.FATAL,message);
	}
	public void error( String message) {
		log(ILogLevel.ERROR,message);
	}
	public void warn( String message) {
		log(ILogLevel.WARN,message);
	}
	public void info( String message) {
		log(ILogLevel.INFO,message);
	}
	public void debug( String message) {
		log(ILogLevel.DEBUG,message);
	}
	
	public void addListener(ILoggerListener listener) {
		listeners.add(listener);
	}
	
	public void removeListener(ILoggerListener listener) {
		listeners.remove(listener);
	}


	public void setLogLevel(int level) {
		// TODO Auto-generated method stub
		levelToLog=level;
	}

	public boolean isDebugEnabled() {
		// TODO Auto-generated method stub
		return DEBUG;
	}

	public void setDebug(boolean debug2) {
		// TODO Auto-generated method stub
		this.debug=debug2;
	}

	public boolean isDebug() {
		return debug;
	}
}
