package main;

import com.csl.util.ProcessUtil;

public class KillMosquitto {

	
	
	public static void main(String[] args) {
		
		ProcessUtil.killProcess("mosquitto");
		
		
	}
	
}
