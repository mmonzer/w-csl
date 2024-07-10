package com.ucsl.interfaces;

import com.ucsl.json.Json;

public interface IApiCommands {

	void setName(String name);
	void setDescription(String description);

	String registerCmd(String name, IJsonCmd j);

	String registerCmd(String name, IJsonCmd j, IJsonCmdHelp jh);

	String getName();
<<<<<<< HEAD
	String getPathName();
	String getDescription();
=======
>>>>>>> origin/feature/refactor_code

	public Json execJcmd(Json jCmd) throws IllegalArgumentException;
	public Json exec(String name, Json params) throws IllegalArgumentException;

	String getPathFilter();

}