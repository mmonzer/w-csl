package com.ucsl.interfaces;

import com.ucsl.json.Json;

public interface IApiCommands {

	void setName(String name);

	String registerCmd(String name, IJsonCmd j);

	String registerCmd(String name, IJsonCmd j, IJsonCmdHelp jh);

	String getName();
	String getPathName();

	public Json execJcmd(Json jCmd);
	public Json exec(String name, Json params) ;

	String getPathFilter();

}