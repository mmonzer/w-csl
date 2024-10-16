package com.csl.intercom.jsoncmd;

public class ApiCommandsFactory {

	public ApiCommands createApiCommands(String name) {
		ApiCommands api= new ApiCommands();
		api.setName(name);
		return api;
	}

}
