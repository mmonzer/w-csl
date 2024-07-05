package com.csl.util;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class TimeUtil {



	static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	static SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
	static DateTimeFormatter DATE_TIME_FORMATTER=	DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-nnnnnnnnn")
	        .withZone(ZoneId.systemDefault());


	static public  String getTimeAsString() {

		Date date = new Date();
		//String dir=System.getProperty("user.dir");

		//fileToLog=dir+File.separatorChar+f+'_'+sdf.format(date.getTime())+".txt";

		return  sdf.format(date.getTime());
	}


	static public String timeStamp() {
		Date date = new Date();

		String s="ID-"+sdf2.format(date.getTime());

		return s;

	}

	static public String timeStampNano() {
		
		Instant instant = Instant.now() ;
		
		
	
		//return(DATE_TIME_FORMATTER.format(instant));
		//System.out.print("time in nanoseconds = ");
	    //  System.out.println(System.nanoTime());

	      // returns the current value of the system timer, in milliseconds
	     // System.out.print("time in milliseconds = ");
	     // System.out.println(System.currentTimeMillis());
	      
	      return ""+System.nanoTime();
		
	}
}
