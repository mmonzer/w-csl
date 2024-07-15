package com.ucsl.interfaces;

import com.csl.intercom.jsoncmd.JsonCmdPrivilegeFamily;
import com.ucsl.json.Json;

import java.util.Map;

public interface IApiCommands {

	void setName(String name);
	void setDescription(String description);

	String registerCmd(String name, IJsonCmd j);

	String registerCmd(String name, IJsonCmd j, IJsonCmdHelp jh);

	String registerCmd(String name, IJsonCmd j, JsonCmdPrivilegeFamily privilegeFamily);
	String registerCmd(String name, IJsonCmd j, IJsonCmdHelp jh, JsonCmdPrivilegeFamily privilegeFamily);

	String getName();
	String getPathName();
	String getDescription();

	Map<String, JsonCmdPrivilegeFamily> getListOfCommandPrivileges();

	public Json execJcmd(Json jCmd) throws IllegalArgumentException;
	public Json exec(String name, Json params) throws IllegalArgumentException;

	String getPathFilter();

}