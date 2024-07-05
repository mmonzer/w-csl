package com.ucsl.util;

import java.io.File;

import com.ucsl.json.Json;

public class IDSUtil {
	

	public static  String fileSeparator=File.separator;


	
	public static int str2int(String s,int defaulft) {
		int i=defaulft;
		
		try {
		      i = Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return defaulft;
		}
		return i;

	}
	
	public static String getProtocolFromInt(int n) {
		if (n==1) return "ICMP";
		else if (n==6) return "TCP";
		else if (n==17) return "UDP";
		else return ""+n;
	}

	public static int getProtocolIntFromName(String name) {

		if (name==null) return -1;
		if (name.compareTo("ICMP")==0) return 1;
		if (name.compareTo("TCP")==0) return 6;
		if (name.compareTo("UDP")==0) return 17;

		if (name.startsWith("#")) name=name.substring(1);
		
		return str2int(name, -1);
		
		//return -1;
	}
	
	static public String getProtocoleName(Json j) {
		Json g=j.get("prot");
		if (g!=null) {
			int n=g.asInteger();
			if (n==1) return "ICMP";
			else if (n==6) return "TCP";
			else if (n==17) return "UDP";
			else return "???";
		}
		return "???";

	}


}
