package com.csl.intercom.broker;

import lombok.Getter;

@Getter
public class MosquittoConfig {

	private static final String MOSQUITTO_DIR ="/usr/local/sbin/";
	private static final String MOSQUITTO_CMD ="./mosquitto";
	
	
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
