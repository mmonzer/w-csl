package com.csl.intercom.jsoncmd;

import com.csl.util.JCmd;
import com.ucsl.interfaces.ICmdHelpProvider;
import com.ucsl.json.Json;

import java.util.HashMap;
import java.util.Map;

public class JsonCmdHelp {
	public static final String INT = "int";
	public static final String LONG = "long";
	public static final String STR = "string";
	public static final String JSON = "json";
	public static final String BOOL = "boolean";

	public static final String STATUS_TODO = "TODO";
	public static final String STATUS_OK = "OK";

	boolean hidden=false;
	
	String name="";
	String desc="";
	String result="";
	String resultType="";
	Map<String,String> paramsdesc= new HashMap<>();
	Map<String,String> paramstype= new HashMap<>();
	
	String status="";
	ICmdHelpProvider helpProvider=null;
	
	public JsonCmdHelp() {
		
	}
	
	public JsonCmdHelp setName(String s) {
		name=s;
		return this;
	}
	
	public JsonCmdHelp setDesc(String s) {
		desc=s;
		return this;
	}
	
	public JsonCmdHelp setParam(String name, String desc, String type) {
		paramsdesc.put(name,desc);
		paramstype.put(name,type);
		
		return this;
	}
	
	public JsonCmdHelp setResult(String s, String type) {
		result=s;
		resultType=type;
		return this;
	}
	
	
	private String getStrPad(String name,int n) {
		
		if (name.length()<n) {
		    	name=(name+"                  ").substring(0,n);
		    }
		return name;
	}
	
	
	private Json paramsToJson() {
		
		if (paramsdesc.isEmpty()) return Json.object();
		
		Json j=Json.object();
		for (Map.Entry<String, String> entry : paramsdesc.entrySet()) {
			String type=paramstype.get(entry.getKey());
			if (type==null) type = ""; else type="["+type+"] ";
			j.set(entry.getKey(), type+entry.getValue());
		}
		return j;
	}
	
	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public Json toJson(Json mode) {
	
		
		if (helpProvider!=null) return helpProvider.getHelp(mode);
				
		Json j= Json.object();
		
		j.set(JCmd.CMD,name);
		j.set("desc",desc);
		j.set(JCmd.PARAMETERS,paramsToJson());
		String type=resultType;
		if (!type.isEmpty()) type ="["+type+"] ";
		j.set("result",type+result);
		
		if (mode.has("status")) j.set("status",status);
		return j;
	}
	
	
	public String toString() {
		String s="";
		
		s=s+"============\n";
		s=s+"cmd <"+name+">:";
		if (!desc.isEmpty()) s=s+desc+"\n";
		
		if (paramsdesc.isEmpty()) {
			s=s+"   "+getStrPad("no parameters",16)+"\n";
		}
		else {
			s=s+"params\n";//   "+getStrPad(JCmd.PARAMETERS,16)+"\n";
			for (Map.Entry<String, String> entry : paramsdesc.entrySet()) {
				s=s+"  "+getStrPad(entry.getKey(),16)+':'+entry.getValue()+"\n";
			}
		}
		if (!result.isEmpty())  {
			s=s+"result:"+result+"\n";
		}
		
		return s+"\n";
		
	}

	public JsonCmdHelp hide() {
		setHidden(true);
		return this;
	}
	
	public JsonCmdHelp setStatus(String s) {
		this.status=s;
		return this;
	}
	
	
	public JsonCmdHelp setHelpProvider(ICmdHelpProvider helpProvider) {
		this.helpProvider=helpProvider;
		return this;
	}
}
	


