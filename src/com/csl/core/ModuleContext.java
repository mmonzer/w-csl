package com.csl.core;

import com.csl.interfaces.IModule;
import com.csl.interfaces.IModuleContext;
import com.ucsl.interfaces.IVariablesTable;
import com.ucsl.json.Json;
import com.wcsl.ids.VariableTableFactory;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ModuleContext implements IModuleContext {
	
	private String name;
	private  IModule module;
	private  Class<IModule> clazz;

	private  Json config;
	
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
