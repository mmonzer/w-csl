package com.csl.intercom.jsoncmd;

import com.csl.intercom.broker.CSLInterModuleCommunicationManager;


public class XApiCommands {

	static boolean debug =true;

	boolean initialized = false;
	private String path = "";

	private CSLInterModuleCommunicationManager interModuleCommunicationManager;

	private String name;

	
	
	public XApiCommands(String name, CSLInterModuleCommunicationManager imcm ) {
		
		this.interModuleCommunicationManager=imcm;
		this.name=name;
	}
	
	public String getPathNameForPost() {
		String s=name;
		if (!s.startsWith("/")) s="/"+s;
		return s;
	}

	public String getCleanApiName() {
		
		String s=path;
		if (s.endsWith("/"))
			s= s.substring(0,s.length() - 1);
		
		if (s.startsWith("/")) s=s.substring(1);
		
		return s;
	}
	
	public String getName() {
	
		return getCleanApiName();
	}

}
