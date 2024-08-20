package com.csl.logger;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


public class CSLLogger {

	public static CSLLogger instance= new CSLLogger();

    public int levelToLog=2;
	
	List<ILoggerListener> listeners =new ArrayList<ILoggerListener>();

    // TODO Auto-generated method stub
    @Getter
    @Setter
    private boolean debug;
	
	static private final String[] labels= {"FATAL","LOG1","","ERROR","WARN ","INFO ","LOG6","LOG7","DEBUG"};
			
	
	public static String label(int l) {
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
        boolean DEBUG = false;
        return DEBUG;
	}

}
