package com.csl.core;

import com.ucsl.json.Json;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class CSLUtil {

	public static int getConfigIntegerValue(Json config,String name,int defaulValue) {

		if (config==null) return defaulValue;
				
		Json obj=config.get(name);
		if (obj==null) return defaulValue;
		return obj.asInteger();
	}
	
	public static long getConfigLongValue(Json config,String name,int defaulValue) {

		if (config==null) return defaulValue;
				
		Json obj=config.get(name);
		if (obj==null) return defaulValue;
		return obj.asLong();
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
		if (obj==null) return new Boolean(defaulValue);
		return obj.asBoolean();

	}

}