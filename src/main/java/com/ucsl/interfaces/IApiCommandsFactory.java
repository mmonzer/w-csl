package com.ucsl.interfaces;

import com.csl.intercom.jsoncmd.ApiCommands;

public interface IApiCommandsFactory {

	public ApiCommands createApiCommands(String name);
}
