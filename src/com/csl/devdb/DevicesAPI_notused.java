package com.csl.devdb;

import com.xcsl.json.Json;

public class DevicesAPI_notused {
	
	
	Json NO_INFO=Json.object();
	
	public Json addDevice() {
		
		return NO_INFO;
	}
	
	public Json delDevice() {
		
		return NO_INFO;
	}
	
	public Json updateDeviceIp() {
		return NO_INFO;
	}
	
	
	//ip ou ip@network
	public Json getDeviceProps(String xIP) {
		return NO_INFO;
	}
	
	public Json getDeviceProp(String xIP, String propname) {
		return NO_INFO;
	}
	
	public Json setDeviceProp(String xIP, String propname, Json element) {
		return NO_INFO;
	}
	
	public Json addToDeviceProp(String xIP, String propname, Json element) {
		return NO_INFO;
	}
	
	public Json delFromDeviceProp(String xIP, String propname, Json element) {
		return NO_INFO;
	}
	
	public Json delFromDeviceProp(String xIP, String propname, int idx) {
		return NO_INFO;
	}
	
	public Json clrToDeviceProp(String xIP, String propname) {
		return NO_INFO;
	}
	
	
	
	//===
	
	// anomaly : -1, no link,  0-ok  1-lowrisk  4-abnormal
	
	public Json addLink(String src, String dst,int port_src, int port_dst, int prot, String app_prot, int anomaly) {
		
		return NO_INFO;
	}
			
	
	public Json delLink(String src, String dst,int port_src, int port_dst, int prot, String app_prot, int anomaly) {
		
		return NO_INFO;
	}
	
	
	// return anomaly level
	public Json getLink(String src, String dst,int port_src, int port_dst, int prot, String app_prot, int anomaly) {
		
		return NO_INFO;
	}
	
	
	// graph
	
}

