package com.csl.web;

import com.csl.core.Config;
import com.csl.udp.CSLFlowManager;
import com.csl.util.NetUtil;
import com.ucsl.interfaces.ICSLFlowListener;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramSocket;


/**
 * This class uses the ICRoute interface to create void routes.
 * The response for an ICRoute is rendered in an after-filter.
 */
public class CSLUDPServer {
	private static final Logger logger = LoggerFactory.getLogger(CSLUDPServer.class);
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


	public void initUDPServer(Config.UdpServerConf config) {


		if (config==null) return;

		boolean on=config.getOn();
		if (!on) return;


		if (initialized) {
			System.err.println("already initialized");
			System.exit(0);
		}

		if (!config.getOn()) return;


		verbose=config.getVerbose();
		traceAllMessages=config.getTraceAllMessages();

		initialized=true;

		maxflows=config.getMaxInputQueues();
		maxsize=config.getMaxSizeOfInputQueues();

		port = config.getPort();
		ip = config.getIp();
		if (ip.isEmpty()) {ip=NetUtil.findIPAddress();}
		if (ip.isEmpty()) {ip="127.0.0.1";}


		int port= getCurrentPortForUDP();
		setCurrentPortForUCP(port);

		if (verbose)
		{
			System.out.println("");
			System.out.println("CSL UDP Server:");
			System.out.println("===============");
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


	public int getCurrentPortForUDP() {
		return port;
	}
	
	public String getCurrentIPForUDP() {
		return ip;
	}

	private void setCurrentPortForUCP(int currentPortForUDP) {
		port = currentPortForUDP;
	}

	public void start() {

		if (!initialized) {
			System.err.println("CSL UDP server not initialized, cannot start");
			System.exit(0);
		}

		getFlowManager().startListener();
		logger.info("Listenning on port:"+port);

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
