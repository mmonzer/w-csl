package com.ucsl.interfaces;

import com.csl.intercom.jsoncmd.ApiCommands;

/**
 * Interface for CSL services.
 * Implementations are services, that is to say a set of commands that can be called by other services and through a web API.
 */
public interface ICSLService {
	
	public String getConfigFileSectionName() ;

	/**
	 * Initialise the service.
	 * Specifically, commands should be defined here.
	 *
	 * @return true if the service is correctly initialised
	 */
	public boolean init();

	/**
	 * Returns the list of commands of the service.
	 *
	 * @return the list of commands of the service
	 */
	public ApiCommands getApiCommands();

	public boolean terminate() ;
	
	
}
