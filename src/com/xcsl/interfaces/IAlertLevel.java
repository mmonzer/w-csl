package com.xcsl.interfaces;


public enum IAlertLevel {
	UNDEF,
	INFO,
	DEBUG,
	TOLERABLE, 
	MODERATE,
	HIGH,
	CRITICAL;
	
	
	public int getLevelAsInt() {
		if (this.equals(INFO)) return 0;
		if (this.equals(DEBUG)) return 0;
		if (this.equals(TOLERABLE)) return 1;
		if (this.equals(MODERATE)) return 2;
		if (this.equals(HIGH)) return 3;
		if (this.equals(CRITICAL)) return 4;
		
		return -1;
	}
	
	public String getLevellAsString() {
		return this.toString();
	}
	
	
	public static IAlertLevel getAlertLevelFromInt( int l) {
	
		if (l==0) return IAlertLevel.INFO;
		if (l==1) return IAlertLevel.TOLERABLE;
		if (l==2) return IAlertLevel.MODERATE;
		if (l==3) return IAlertLevel.HIGH;
		if (l==4) return IAlertLevel.CRITICAL;
		
		return IAlertLevel.UNDEF;
	}
	
	public static IAlertLevel getAlertLevelFromString(String s) {
		if (s==null) return IAlertLevel.UNDEF;
		s=s.toUpperCase();
		if (s.compareTo("INFO")==0) return IAlertLevel.INFO;
		if (s.compareTo("DEBUG")==0) return IAlertLevel.DEBUG;
		if (s.compareTo("TOLERABLE")==0) return IAlertLevel.TOLERABLE;
		if (s.compareTo("MODERATE")==0) return IAlertLevel.MODERATE;
		if (s.compareTo("HIGH")==0) return IAlertLevel.HIGH;
		if (s.compareTo("CRITICAL")==0) return IAlertLevel.CRITICAL;
		
		return IAlertLevel.UNDEF;
	}
	
	
	
	public String toStringWithIndex() {
		int level =getLevelAsInt();
		return getLevellAsString()+':'+level;
	}
	
	//public static AlertLevel UNDEF_LEVEL = new AlertLevel(-1);

	//public static String UNDEF = "UNDEF";
	// public static SeverityLevel UNDEF_LEVEL= new SeverityLevel(-1);

	//int level = -1;

	//public static String ALERT_INFO = "INFO";
	//public static String ALERT_DEBUG = "IDEBUG";

	//public static String[] LEVEL0 = new String[] { "INFO", "DEBUG" };
	//public static String[] LEVEL1 = new String[] { "TOLERABLE" };
	//public static String[] LEVEL2 = new String[] { "MODERATE" };
	//public static String[] LEVEL3 = new String[] { "HIGH" };
	//public static String[] LEVEL4 = new String[] { "CRITICAL" };

}
