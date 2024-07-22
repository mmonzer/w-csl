package com.ucsl.interfaces;

public interface IAlertSender {
	public void sendAlert(IAlertDescriptor alertDescriptor,boolean toViewer, boolean toLog) ;
	
}
