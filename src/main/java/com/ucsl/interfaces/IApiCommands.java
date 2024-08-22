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

	/**
	 * Executes the command given, with the parameters given
	 * @param jsonCommand json with the command and parameters for executing it
	 * @return the result of the command execution
	 * @throws IllegalArgumentException if parameter required and not found
	 */
	Json execJcmd(Json jsonCommand) throws IllegalArgumentException;

	/**
	 * Executes the command given, with the parameters given
	 * @param commandName name of the command to execute
	 * @param params parameters of the command to execute
	 * @return the result of the command execution
	 * @throws IllegalArgumentException if parameter required and not found
	 */
	Json exec(String commandName, Json params) throws IllegalArgumentException;

	String getPathFilter();

}