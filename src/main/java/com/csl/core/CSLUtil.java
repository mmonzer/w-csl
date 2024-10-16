package com.csl.core;

import com.ucsl.json.Json;

public class CSLUtil {

	public static int getConfigIntegerValue(Json config,String name,int defaulValue) {

		if (config==null) return defaulValue;
				
		Json obj=config.get(name);
		if (obj==null) return defaulValue;
		return obj.asInteger();
	}

	public static String getConfigStringValue(Json config,String name,String defaulValue) {

		if (config==null) return defaulValue;
		Json obj=config.get(name);
		if (obj==null) return defaulValue;
		return obj.asString();

	}

	public static Boolean getConfigBooleanValue(Json config,String name,boolean defaulValue) {

		if (config==null) return defaulValue;
		Json obj=config.get(name);
		if (obj==null) return defaulValue;
		return obj.asBoolean();

	}

}