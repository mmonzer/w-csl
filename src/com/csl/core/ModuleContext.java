package com.csl.core;

import com.csl.interfaces.IModule;
import com.csl.interfaces.IModuleContext;
import com.xcsl.json.Json;
import com.xcsl.variables.VariablesTable;

public class ModuleContext implements IModuleContext {
	
	private String name;
	private  IModule module;
	private  Class<IModule> clazz;
	private  VariablesTable localVariablesTable=new VariablesTable();
	private  Json config;
	
	int inputPriority=0;
	int outputPriority=0;
	int stepPriority=0;
	
	int loopNumber=0;  //default
	
	//HashMap<String,ParamDescriptor> listOfParamsDescriptors=new HashMap<String,ParamDescriptor>();

	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Class<IModule> getClazz() {
		return clazz;
	}
	public void setClazz(Class<IModule> clazz) {
		this.clazz = clazz;
	}
	public IModule getModule() {
		return module;
	}
	public void setModule(IModule module) {
		this.module = module;
	}
	public VariablesTable getLocalVariablesTable() {
		return localVariablesTable;
	}
	public void setLocalVariablesTable(VariablesTable localVariablesTable) {
		this.localVariablesTable = localVariablesTable;
	}
	public Json getConfig() {
		return config;
	}
	public void setConfig(Json config) {
		this.config = config;
	}
	public int getInputPriority() {
		return inputPriority;
	}
	public void setInputPriority(int inputPriority) {
		this.inputPriority = inputPriority;
	}
	public int getOutputPriority() {
		return outputPriority;
	}
	public void setOutputPriority(int outputPriority) {
		this.outputPriority = outputPriority;
	}
	public int getStepPriority() {
		return stepPriority;
	}
	public void setStepPriority(int stepPriority) {
		this.stepPriority = stepPriority;
	}
	
	
	
	public int getLoopNumber() {
		return loopNumber;
	}
	public void setLoopNumber(int loopNumber) {
		this.loopNumber = loopNumber;
	}
	
	
//	void initParameters() {
//		
//
//		Json params=getConfig().get("params");
//		if (params==null) return ;
//		
//		
//		Iterator<Json> itr=params.iterator();
//
//		while(itr.hasNext()) {
//			Json jv = itr.next();
//			////System.out.println(jv);
//			ParamDescriptor mvd= new ParamDescriptor(jv);
//			listOfParamsDescriptors.put(mvd.getName(),mvd);
//			//System.out.println("XXXXX_INIT_params="+mvd);
//
//		}
//
//	}
//
//	
//	public ParamDescriptor getParamDescriptor(String name) {
//		return listOfParamsDescriptors.get(name);
//	}
//	
//	public String getParamAsString(String name,String defaultValue) {
//		ParamDescriptor z = listOfParamsDescriptors.get(name);
//		if (z==null) return defaultValue;
//		return z.getValue();
//	}
//	
//	public int getParamAsInteger(String name,int defaultValue) {
//		ParamDescriptor z = listOfParamsDescriptors.get(name);
//		if (z==null) return defaultValue;
//		return z.asInteger(defaultValue);
//	}
//	
//	public double getParamAsDouble(String name,double defaultValue) {
//		ParamDescriptor z = listOfParamsDescriptors.get(name);
//		if (z==null) return defaultValue;
//		return z.asDouble(defaultValue);
//	}
//	
//	public boolean getParamAsBoolean(String name,boolean defaultValue) {
//		ParamDescriptor z = listOfParamsDescriptors.get(name);
//		if (z==null) return defaultValue;
//		return z.asBoolean(defaultValue);
//	}
//	
//	public String paramsToString() {
//		String s="Parameters:";
//		
//		String l="";
//		for( Map.Entry<String,ParamDescriptor> e : listOfParamsDescriptors.entrySet() ) {
//			if (!s.isEmpty()) s=s+'\n';
//			l="";
//			l=l+"  "+e.getValue().getName()+"="+e.getValue().getValue();
//			int n=Math.max(0, 20-l.length());
//			l=l+"                     ".substring(0,n );
//			l=l+"//"+e.getValue().getDescription();
//			s=s+l;
//			}
//		return s+"\n";
//		
//	}
	
	public String toString() {
		String s= "MODULE Name :"+getName()+"\n";
		
		s=s+"Class:"+getClazz()+"\n";
		s=s+"Priority: input="+inputPriority+" step="+stepPriority+" output="+outputPriority+"\n";
		//s=s+paramsToString();
		s=s+"Config:\n"+getConfig()+"\n";
		
		return s;
	}


}
