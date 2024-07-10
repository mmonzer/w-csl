package com.csl.interfaces;

import com.ucsl.json.Json;

public interface IModuleContext {

	
	
	public String getName();
	//public VariablesTable getLocalVariablesTable();
	public Json getConfig();
	
	public int getInputPriority();  // priority set in ModuleConfiguration
	public int getStepPriority();  // priority set in ModuleConfiguration
	public int getOutputPriority();  // priority set in ModuleConfiguration
	

		
	//public ParamDescriptor getParamDescriptor(String name);
//	public String getParamAsString(String name,String defaultValue);
//	public int getParamAsInteger(String name,int defaultValue);
//	public double getParamAsDouble(String name,double defaultValue);
//	public boolean getParamAsBoolean(String name,boolean defaultValue);
	
	public IModule getModule();
	
}
