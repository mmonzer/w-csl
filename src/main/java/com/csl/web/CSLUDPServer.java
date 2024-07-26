package com.csl.web;

import com.csl.core.Config;
import com.csl.logger.CSLLogger;
import com.csl.udp.CSLFlowManager;
import com.csl.util.NetUtil;
import com.ucsl.interfaces.ICSLFlowListener;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import lombok.Getter;
import lombok.Setter;

import java.net.DatagramSocket;


/**
 * This class uses the ICRoute interface to create void routes.
 * The response for an ICRoute is rendered in an after-filter.
 */
public class CSLUDPServer {
	private CSLFlowManager flowManager;
	private int maxsize;
	private int maxflows;


	private String ip="127.0.0.1";
	
	private int port=-1;
	private boolean verbose =false;

    private boolean initialized =false;
	@Setter
    @Getter
    private  boolean started=false;

    private  boolean traceAllMessages=false;

	DatagramSocket dsocket=null;

	
	public void reinitServer(Json j) {
		
		stop();
		initialized=false;
		initUDPServer(j);
		
	}


	public void initUDPServer(Json j) { //String rootdir, int port, boolean verbose) {


		boolean on=JsonUtil.getBooleanFromJson(j, "on",true);
		if (!on) return;


		if (initialized) {
			System.err.println("already initialized");
			System.exit(0);
		}

		if (j==null) return;

		String userDir = System.getProperty("user.dir");

		boolean running = JsonUtil.getBooleanFromJson(j, "on", false);
		if (!running) return;


		verbose=JsonUtil.getBooleanFromJson(j,"verbose", false);
		boolean debug = JsonUtil.getBooleanFromJson(j, "debug", false);
		traceAllMessages=JsonUtil.getBooleanFromJson(j,"trace_all_messages", false);

		initialized=true;

		maxflows=JsonUtil.getIntFromJson(j,"max_input_queues",10);
		maxsize=JsonUtil.getIntFromJson(j,"max_size_of_input_queues",100);

		port = JsonUtil.getIntFromJson(j, "port",8001);
		ip = JsonUtil.getStringFromJson(j, "ip","");
		if (ip.isEmpty()) ip=NetUtil.findIPAddress();
		if (ip.isEmpty()) ip="127.0.0.1";


		int port=getCurrentPortForUCP();
		boolean ok=false;
		setCurrentPortForUCP(port);

		if (verbose)
		{
			System.out.println("");
			System.out.println("CSL UDP Server:");
			System.out.println("===============");
			System.out.println("  on  :"+ running);
			System.out.println("  ip  :"+ip);
			System.out.println("  port:"+port);
			System.out.println("  trace all messages:"+traceAllMessages);


		}
	}


	public void initUDPServer(Config.CSLUdpServerConf config) { //String rootdir, int port, boolean verbose) {


		if (config==null) return;

//		boolean on=JsonUtil.getBooleanFromJson(j, "on",true);
		boolean on=config.getOn();
		if (!on) return;


		if (initialized) {
			System.err.println("already initialized");
			System.exit(0);
		}


		String userDir = System.getProperty("user.dir");

		boolean running = config.getOn();
		if (!running) return;


//		verbose=JsonUtil.getBooleanFromJson(j,"verbose", false);
		verbose=config.getVerbose();
//		boolean debug = JsonUtil.getBooleanFromJson(j, "debug", false);
//		traceAllMessages=JsonUtil.getBooleanFromJson(j,"trace_all_messages", false);
		traceAllMessages=config.getTraceAllMessages();

		initialized=true;

//		maxflows=JsonUtil.getIntFromJson(j,"max_input_queues",10);
		maxflows=config.getMaxInputQueues();
//		maxsize=JsonUtil.getIntFromJson(j,"max_size_of_input_queues",100);
		maxsize=config.getMaxSizeOfInputQueues();

//		port = JsonUtil.getIntFromJson(j, "port",8001);
		port = config.getPort();
//		ip = JsonUtil.getStringFromJson(j, "ip","");
		ip = config.getIp();
		if (ip.isEmpty()) {ip=NetUtil.findIPAddress();}
		if (ip.isEmpty()) {ip="127.0.0.1";}


		int port=getCurrentPortForUCP();
		boolean ok=false;
		setCurrentPortForUCP(port);

		if (verbose)
		{
			System.out.println("");
			System.out.println("CSL UDP Server:");
			System.out.println("===============");
			System.out.println("  on  :"+ running);
			System.out.println("  ip  :"+ip);
			System.out.println("  port:"+port);
			System.out.println("  trace all messages:"+traceAllMessages);


		}
	}


    public boolean isTraceAllMessages() {
		return traceAllMessages;
	}




	public void setTraceAllMessages(boolean traceAllMessages) {
		this.traceAllMessages = traceAllMessages;
	}




	public int getCurrentPortForUCP() {
		return port;
	}
	
	public String getCurrentIPForUCP() {
		return ip;
	}

	private void setCurrentPortForUCP(int currentPortForUCP2) {
		port = currentPortForUCP2;
	}

	public void start() {

		if (!initialized) {
			System.err.println("CSL UDP server not initialized, cannot start");
			System.exit(0);
		}

		getFlowManager().startListener();
		CSLLogger.instance.info("Listenning on port:"+port);

		if (verbose) System.out.println("  UDP server listening on "+port);
	}

	public void stop() {
		
		if (dsocket!=null) 
			dsocket.close();
		getFlowManager().stopListener();
		
	}

	//===UDP communication
	private CSLFlowManager getFlowManager() {

		if (!initialized) {
			System.err.println("CSL UDP server not initialized, cannot start");
			System.exit(0);
		}

		if (flowManager==null) {

			flowManager= new CSLFlowManager(maxflows, maxsize,traceAllMessages);
		}
		return flowManager;
	}


	public void addListener(int queueNumber,ICSLFlowListener l ) {

		getFlowManager().addListener(queueNumber,l); 
	}

}
