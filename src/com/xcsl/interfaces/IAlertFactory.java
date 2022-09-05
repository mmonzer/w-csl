package com.xcsl.interfaces;

import com.xcsl.json.Json;

public interface IAlertFactory {

	
	
	
	public IAlertDescriptor createAlertDescriptor(long time) ;
	public IAlertDescriptor createAlertDescriptor(Json j) ;
	public IAlertDescriptor createAlertDescriptor(int level, String msg, long time);
	
}
