package com.csl.util;

import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;

import java.time.Instant;
import java.time.ZoneOffset;

public class EveMessageUtill {

	
	public static Json reformatTimeStamp(Json jj) {
		
		String s=JsonUtil.getStringFromJson(jj,"timestamp", "");
		if (!s.isEmpty()) {
			long ns=convertTimestamp(s);
			jj.set("timestamp0",s);
			jj.set("timestamp",ns);
			if (jj.has("alert")) {
				Json jalert=jj.get("alert");
				String sig= JsonUtil.getStringFromJson(jalert, "signature","???");
			}
			
		}
		return jj;
	}
	
private static long convertTimestamp(String s) {
		
		int n= s.indexOf('+');
		if (n<0) n=s.indexOf('-');
		
		String sz="";
		
		if (n>=0) {
			sz=s.substring(n, s.length());
			s=s.substring(0,n);
		}
		
		if (!s.endsWith("Z")) s=s+"Z";
			
		Instant instant = Instant.parse(s);
	
		long offset=0;
	
		if (!sz.isEmpty()) {
			ZoneOffset zoneOffSet= ZoneOffset.of(sz);
			offset=zoneOffSet.getTotalSeconds();
		}
		
		long ts=instant.toEpochMilli()-offset*1000;

		return ts;
		
	}

}
