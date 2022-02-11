package com.csl.devdb.util;

import java.util.List;
import java.util.Map;

import com.xcsl.json.Json;

public class JsonCompare {

	static public boolean compareWithPrimitive(Json j1, Json j2) {
		
		
		if (j1==null) return (j2==null);			
		if (j2==null) return false;
		
		if (j1.isPrimitive()) {
			if (!j2.isPrimitive()) return false;
		}
		
		return (j1.toString().compareTo(j2.toString())==0);
		
		
	}
	
	static public boolean compareWithArray(Json j1, Json j2) {
		
		if (j1==null) return (j2==null);			
		if (j2==null) return false;
		
		if (!j1.isArray()) return false;
		if (!j2.isArray()) return false;
		
		List<Json> l1 = j1.asJsonList();
		List<Json> l2 = j2.asJsonList();
		
		if (l1.size()!=l2.size()) return false;
		
		for (int i=0;i< l1.size(); i++) {
			boolean equal= compare(l1.get(i), l2.get(i));
			if (!equal) return false;
		}
		return true;
	}
	
	
	
	static public boolean compareWithObject(Json j1, Json j2) {
		
		if (j1==null) return (j2==null);			
		if (j2==null) return false;
		
		if (!j1.isObject()) return false;
		if (!j2.isObject()) return false;
	
		Map<String, Json> l1 = j1.asJsonMap();
		Map<String, Json> l2 = j2.asJsonMap();
		
		if (l1.size()!=l2.size()) return false;
		
		for (Map.Entry<String, Json> entry : l1.entrySet()) {
			String key=entry.getKey();
			Json e1=entry.getValue();
			Json e2=l2.get(key);
			boolean equal= compare(e1, e2);
			if (!equal) return false;
		}
		
		return true;
	}
	
	static public boolean compare(Json j1, Json j2) {
		
		if (j1==null) return (j2==null);			
		if (j2==null) return false;
		
		if (j1.isPrimitive()) return compareWithPrimitive(j1, j2);
		
		if (j1.isObject()) return compareWithObject(j1, j2);
		
		if (j1.isArray()) return compareWithArray(j1, j2);
		
		System.err.println("Invalid type "+j1);
		return false;
		
	}
	

	
}
