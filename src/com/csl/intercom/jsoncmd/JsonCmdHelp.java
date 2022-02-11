package com.csl.intercom.jsoncmd;

import java.util.HashMap;
import java.util.Map;

import com.xcsl.json.Json;

public class JsonCmdHelp {

	static public String INT="int";
	static public String LONG="long";
	static public String STR="string";
	static public String JSON="json";
	static public String BOOL="boolean";
	
	static public String STATUS_TODO="TODO";
	static public String STATUS_TOTEST="TO TEST";
	static public String STATUS_OK="OK";
	
	
	boolean hidden=false;
	
	String name="";
	String desc="";
	String result="";
	String resultType="";
	Map<String,String> paramsdesc= new HashMap<String, String>();
	Map<String,String> paramstype= new HashMap<String, String>();
	
	String status="";
			
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
		
		if (paramsdesc.size()==0) return Json.object();
		
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
	
		Json j= Json.object();
		
		j.set("cmd",name);
		j.set("desc",desc);
		j.set("params",paramsToJson());
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
		
		if (paramsdesc.size()==0) {
			s=s+"   "+getStrPad("no parameters",16)+"\n";
		}
		else {
			s=s+"params\n";//   "+getStrPad("params",16)+"\n";
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
		// TODO Auto-generated method stub
		setHidden(true);
		return this;
	}
	
	public JsonCmdHelp setStatus(String s) {
		// TODO Auto-generated method stub
		this.status=s;
		return this;
	}
}
	


