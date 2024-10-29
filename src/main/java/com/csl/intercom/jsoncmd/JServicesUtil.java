package com.csl.intercom.jsoncmd;

import com.ucsl.json.Json;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


public class JServicesUtil {

	public static final String EOL = System.getProperty("line.separator");

	Json jConfig=null;
	
	public Json readConfigFileInJson(String fileName) {

		String content="{}";
		try {
			content = readFile(fileName);
		} catch (IOException e) {
			
			// e.printStackTrace();
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
