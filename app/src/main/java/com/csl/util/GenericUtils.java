package com.csl.util;

public class GenericUtils {

	
	public static int str2int(String s,int defaulft) {
		int i=defaulft;
		
		try {
		      i = Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return defaulft;
		}
		return i;

	}

}