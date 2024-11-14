package com.csl.alert;

public class CSLAlertFactory  {
	
	public static final CSLAlertFactory instance = new CSLAlertFactory();

	public AlertDescriptor createAlertDescriptor(int level, String msg, long time) {
		return new AlertDescriptor().setLevelFromInt(level).setMsg(msg).setTime(time);
	}

}
