package com.csl.util;

import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

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
				//System.err.println("ALERT:"+sig);
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
			//System.out.println("sz="+sz);
			ZoneOffset zoneOffSet= ZoneOffset.of(sz);
			offset=zoneOffSet.getTotalSeconds();
			//System.out.println("zone="+zoneOffSet.getTotalSeconds());
		}
		
		long ts=instant.toEpochMilli()-offset*1000;
		//System.out.println("TS="+ts);
		
	//	System.out.println("diff="+(ts-Instant.now().toEpochMilli()));
	//	System.out.println("diff="+(instant.toEpochMilli()-Instant.now().toEpochMilli()-1000*3600*2));
		
		// System.out.println(instant.toEpochMilli());
		 
		
		
		
		
		
		return ts;
		
	}



	static public Json getEveInfo(Json jj) {
	
		Json result=Json.object();
		for (Map.Entry<String, Json> e : jj.asJsonMap().entrySet()) {
			String key=e.getKey();
			if ((key.compareTo("timestamp")!=0)
				&& (key.compareTo("type")!=0))
				
			result.set(key, e.getValue());
			
		}
		return result;
	}

}
