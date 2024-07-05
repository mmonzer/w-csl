package com.csl.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CSLGlobal {

	public String IDS_RUNNER="IDS_RUNNER";
	public String CSL_HTTP_SERVER="CSL_HTTP_SERVER";
	public String CSL_UDP_SERVER="CSL_UDP_SERVER";
	public String CSL_DATABASE_SERVER="CSL_HTTP_SERVER";
	
	List<String> validNames= new ArrayList<String>();
			
	
	Map<String,Object> globals= new HashMap<>();
	
	
	public void register (String name, Object o) {
		if (globals.get(name)!=null) {
			System.err.println("Object <"+name+"> already registered");
		}
	 	
	}
	
	
}
