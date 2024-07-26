package com.csl.interfaces;

import com.csl.core.Config;
import com.ucsl.json.Json;

public interface IModuleContext {
	public String getName();
//	public Json getConfig();
	public Config.CSLModule.CSLModuleConfig getConfig();
	
	public int getInputPriority();  // priority set in ModuleConfiguration
	public int getStepPriority();  // priority set in ModuleConfiguration
	public int getOutputPriority();  // priority set in ModuleConfiguration
	
	public IModule getModule();
	
}
