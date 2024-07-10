package com.csl.interfaces;

import java.util.HashMap;
import java.util.List;

public interface ICSLHtmlCommand {
	
	String getName();
	
	public String exec(String command,List<String>varnames, HashMap<String,String> params);
	
}
