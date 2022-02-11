package com.csl.intercom.jsoncmd;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import com.xcsl.json.Json;


public class JServicesUtil {

	public static final String EOL = System.getProperty("line.separator");

	Json jConfig=null;
	
	public Json readConfigFileInJson(String fileName) {
		//path, String fileName) {


		//URL  url= new URL("file:///"+fileName);
		String content="{}";
		//String f=path+ File.separator+fileName;
		try {
			content = readFile(fileName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//CSLContext.cslLogger.error("Cannot read config file :"+f);
		}

		return Json.read(content);
	}

	private static String readFile(String filename) throws IOException {
		BufferedReader br = null;
		FileReader fr = null;

		try {
			fr = new FileReader(filename);
			br = new BufferedReader(fr);
			String nextLine = "";
			StringBuilder sb = new StringBuilder();
			while ((nextLine = br.readLine()) != null) {
				sb.append(nextLine); // note: BufferedReader strips the EOL character
				//   so we add a new one!
				sb.append(EOL);
			}
			return sb.toString();
		}
		finally {
			if (br != null) br.close();
			if (fr != null) fr.close();
		}
	}

}
