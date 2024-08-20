package com.ucsl.json;

public class JsonMoreUtil {
	
	
	public static String json2str(Json j) {
		
		String s=j.toString();
		if (s.length()>80) return s.substring(0,80)+"....";
		return s;
	}

	public static String str2shortstr(String s) {
		
		
		if (s.length()>80) return s.substring(0,80)+"....";
		return s;
	}
}
