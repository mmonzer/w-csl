package com.csl.intercom.jsoncmd;

import com.ucsl.interfaces.IApiCommands;
import com.ucsl.interfaces.IApiCommandsFactory;

public class ApiCommandsFactory implements IApiCommandsFactory {

	@Override
	public IApiCommands createApiCommands(String name) {
		// TODO Auto-generated method stub
		
		ApiCommands api= new ApiCommands();
		api.setName(name);
		return api;
	}

}
