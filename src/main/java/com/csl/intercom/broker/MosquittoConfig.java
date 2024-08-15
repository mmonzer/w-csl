package com.csl.intercom.broker;

import lombok.Getter;

@Getter
public class MosquittoConfig {

	private final String mosquittoDir="/usr/local/sbin/";
	private final String mosquittoCmd="./mosquitto";
	
	
	private int port=1883;
	
	private String ip="localhost";
	
	boolean useBroker=false;

	
	public String getBrokerURL() {
		
		return "tcp://"+ip+":"+port;
	}

    public MosquittoConfig setPort(int port) {
		this.port = port;
		return this;
	}

    public MosquittoConfig setIp(String ip) {
		this.ip = ip;
		return this;
	}

    public MosquittoConfig setUseBroker(boolean useBroker) {
		this.useBroker = useBroker;
		return this;
	}
}
