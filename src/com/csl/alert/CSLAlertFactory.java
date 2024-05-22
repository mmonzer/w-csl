package com.csl.alert;

import com.ucsl.interfaces.IAlertDescriptor;
import com.ucsl.interfaces.IAlertFactory;
import com.ucsl.json.Json;

public class CSLAlertFactory implements IAlertFactory {
	
	static public CSLAlertFactory instance = new CSLAlertFactory();

	@Override
	public IAlertDescriptor createAlertDescriptor(long time) {
		// TODO Auto-generated method stub
		return new AlertDescriptor().setTime(time);
	}

	@Override
	public IAlertDescriptor createAlertDescriptor(Json j) {
		// TODO Auto-generated method stub
		return new AlertDescriptor(j);
	}

	@Override
	public IAlertDescriptor createAlertDescriptor(int level, String msg, long time) {
		// TODO Auto-generated method stub
		return new AlertDescriptor().setLevelFromInt(level).setMsg(msg).setTime(time);
	}

}
