package com.csl.intercom.broker;

public class MosquittoConfig {

	
	
	
	private String mosquittoDir="/usr/local/sbin/";
	private String mosquittoCmd="./mosquitto";
	
	
	private int port=1883;
	
	private String ip="localhost";
	
	boolean useBroker=true;
	
	//private static  String BROKER_TCP_LOCALHOST_1883 = "tcp://localhost:1883";

	
	public String getBrokerURL() {
		
		return "tcp://"+ip+":"+port;
	}

	public String getMosquittoDir() {
		return mosquittoDir;
	}

	public MosquittoConfig setMosquittoDir(String mosquittoDir) {
		this.mosquittoDir = mosquittoDir;
		return this;
	}

	public String getMosquittoCmd() {
		return mosquittoCmd;
	}

	public MosquittoConfig setMosquittoCmd(String mosquittoCmd) {
		this.mosquittoCmd = mosquittoCmd;
		return this;
	}

	public int getPort() {
		return port;
	}

	public MosquittoConfig setPort(int port) {
		this.port = port;
		return this;
	}

	public String getIp() {
		return ip;
	}

	public MosquittoConfig setIp(String ip) {
		this.ip = ip;
		return this;
	}

	public boolean isUseBroker() {
		return useBroker;
	}

	public MosquittoConfig setUseBroker(boolean useBroker) {
		this.useBroker = useBroker;
		return this;
	}

	
	
	
	
	
}
