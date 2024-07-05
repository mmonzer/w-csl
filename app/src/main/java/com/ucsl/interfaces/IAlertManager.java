package com.ucsl.interfaces;



import com.ucsl.json.Json;

public interface IAlertManager extends IAlertSender {

	
	public IAlertManager setname(String loggerName);
	
	public void sendAlert(IAlertDescriptor a);
	public void sendAlert(IAlertDescriptor alertDescriptor,boolean toViewer, boolean toLog) ;
	
	
	//public void register(IDSAlertListener listener);

	public Json getListOfCurrentAlertsAsJson();

	public Json execOpAlert(Json params);

	public Json resetListOfCurrentAlerts();

	public Json saveListOfCurrentAlerts();

	public Json getAlertStats();

	//public IAlertFactory getAlertFactory();

	//void register(IDSAlertListener listener);
	
}
