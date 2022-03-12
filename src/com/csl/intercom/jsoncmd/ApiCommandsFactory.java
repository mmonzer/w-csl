package com.csl.intercom.jsoncmd;

import com.xcsl.interfaces.IApiCommands;
import com.xcsl.interfaces.IApiCommandsFactory;

public class ApiCommandsFactory implements IApiCommandsFactory {

	@Override
	public IApiCommands createApiCommands(String name) {
		// TODO Auto-generated method stub
		
		ApiCommands api= new ApiCommands();
		api.setName(name);
		return api;
	}

}
