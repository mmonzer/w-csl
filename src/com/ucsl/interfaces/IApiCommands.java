package com.ucsl.interfaces;

import com.ucsl.json.Json;

public interface IApiCommands {

	void setName(String name);
	void setDescription(String description);

	String registerCmd(String name, IJsonCmd j);

	String registerCmd(String name, IJsonCmd j, IJsonCmdHelp jh);

	String getName();
	String getPathName();
	String getDescription();

	public Json execJcmd(Json jCmd);
	public Json exec(String name, Json params) ;

	String getPathFilter();

}