package com.ucsl.interfaces;

import com.ucsl.json.Json;

public interface IJsonCmdHelp {

	static public String INT="int";
	static public String LONG="long";
	static public String STR="string";
	static public String JSON="json";
	static public String BOOL="boolean";
	
	static public String STATUS_TODO="TODO";
	static public String STATUS_OK="OK";
	
	IJsonCmdHelp setName(String s);

	IJsonCmdHelp setDesc(String s);

	IJsonCmdHelp setParam(String name, String desc, String type);

	IJsonCmdHelp setResult(String s, String type);

	boolean isHidden();

	void setHidden(boolean hidden);

	Json toJson(Json mode);

	IJsonCmdHelp hide();

	IJsonCmdHelp setStatus(String s);
	IJsonCmdHelp  setHelpProvider(ICmdHelpProvider h);


}