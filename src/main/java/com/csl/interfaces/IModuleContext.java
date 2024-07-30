package com.csl.interfaces;

import com.csl.core.Config;

public interface IModuleContext {
	public String getName();
//	public Json getConfig();
	public Config.Module.CSLModuleConfig getConfig();
	
	public int getInputPriority();  // priority set in ModuleConfiguration
	public int getStepPriority();  // priority set in ModuleConfiguration
	public int getOutputPriority();  // priority set in ModuleConfiguration
	
	public IModule getModule();
	
}
