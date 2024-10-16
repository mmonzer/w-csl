package com.csl.core;

import com.csl.modules.ModuleIDS;
import com.ucsl.json.Json;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ModuleContext {
	
	private String name;
	private ModuleIDS module;
	private  Class<Module> clazz;

	private  Json mConfig;
	private Config.Module.CSLModuleConfig config;
	
	int inputPriority=0;
	int outputPriority=0;
	int stepPriority=0;
	
	int loopNumber=0;  //default
	
	public String toString() {
		String s= "MODULE Name :"+getName()+"\n";
		
		s=s+"Class:"+getClazz()+"\n";
		s=s+"Priority: input="+inputPriority+" step="+stepPriority+" output="+outputPriority+"\n";
		//s=s+paramsToString();
		s=s+"Config:\n"+getConfig()+"\n";
		
		return s;
	}
}
