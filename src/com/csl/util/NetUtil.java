package com.csl.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;

import static java.lang.System.out;

public class NetUtil {

	
	 public static String findIPAddress()  {
	    	
	    	
	    	try {
	    	Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
		      
	    	for (NetworkInterface netint : Collections.list(nets)) {
	        out.printf("Display name: %s\n", netint.getDisplayName());
	        out.printf("Name: %s\n", netint.getName());
	        Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
	        for (InetAddress inetAddress : Collections.list(inetAddresses)) {
	            out.printf("InetAddress: %s\n", inetAddress);
	            if (inetAddress  instanceof Inet4Address) {
	            	String ip = inetAddress.getHostAddress();
	            	System.out.println("["+ip+"]");
	            	if (ip.compareTo("127.0.0.1")!=0) return ip;
	            }
	        }
	        out.printf("\n");
	    	}
	    	} catch (SocketException e) {
	    		System.out.println(e);
	    	}
	    	
	    	return "";
	     }
}
