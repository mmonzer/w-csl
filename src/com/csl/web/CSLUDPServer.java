package com.csl.web;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import com.csl.core.CSLContext;
import com.csl.logger.CSLLogger;
import com.csl.udp.CSLFlowManager;
import com.xcsl.interfaces.ICSLFlowListener;
import com.xcsl.json.Json;
import com.xcsl.json.JsonUtil;
import com.xcsl.variables.CSLVariable;


/**
 * This class uses the ICRoute interface to create void routes.
 * The response for an ICRoute is rendered in an after-filter.
 */
public class CSLUDPServer {
	
	//static public CSLUDPServer instance = new CSLUDPServer();

	static private int PORTMAX=9999;

	private CSLFlowManager flowManager;
	private int maxsize;
	private int maxflows;


	private int port=-1;
	private boolean verbose =false;
	private boolean running =false;
	private boolean debug =false;

	private boolean initialized =false;
	private  boolean started=false;

	private  String userDir="";

	private  boolean traceAllMessages=true;

	DatagramSocket dsocket=null;

	
	public void reinitServer(Json j) {
		
		stop();
		initialized=false;
		initUDPServer(j);
		
	}
	
	
	public void initUDPServer(Json j) { //String rootdir, int port, boolean verbose) {


		if (initialized) {
			System.err.println("already initialized");
			System.exit(0);
		}

		if (j==null) return;

		userDir = System.getProperty("user.dir");

		running=JsonUtil.getBooleanFromJson(j,"on", false);
		if (!running) return;


		verbose=JsonUtil.getBooleanFromJson(j,"verbose", false);
		debug=JsonUtil.getBooleanFromJson(j,"debug", false);
		traceAllMessages=JsonUtil.getBooleanFromJson(j,"trace_all_messages", false);


		initialized=true;

		maxflows=JsonUtil.getIntFromJson(j,"max_input_queues",10);
		maxsize=JsonUtil.getIntFromJson(j,"max_size_of_input_queues",100);

		port = JsonUtil.getIntFromJson(j, "port",8001);


	
		int port=getCurrentPortForUCP();
		boolean ok=false;
		/*while (!ok ) {
			ok=true;
			try {
				dsocket = new DatagramSocket(port);
			} catch (SocketException e1) {
				// TODO Auto-generated catch block
				System.out.println(e1.getMessage());
				e1.printStackTrace();
				ok=false;
				port++;	
				if (port>PORTMAX) {
					//throw 
					//System.err.println("impossible to find port ");

				}
			}
			dsocket.close();
		}*/
		
		
		/*InetSocketAddress addr = new InetSocketAddress("localhost", port);
		
		try {
			dsocket = new DatagramSocket(null);
			dsocket.setReuseAddress(true);
			
			//InetSocketAddress(InetAddress addr, int port)
			InetSocketAddress is = new InetSocketAddress((InetAddress)null, port);
			
			dsocket.bind(is);
			
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		
		setCurrentPortForUCP(port);

		if (verbose) 
		{
			System.out.println("");
			System.out.println("CSL UDP Server:");
			System.out.println("===============");
			System.out.println("  on  :"+running);
			System.out.println("  port:"+port);
			System.out.println("  trace all messages:"+traceAllMessages);


		}
	}




	public boolean isStarted() {
		return started;
	}


	public void setStarted(boolean started) {
		this.started = started;
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



	public boolean isFlowEmpty(int n) {
		return getFlowManager().isFlowEmpty(n);
	}

	public int getFlowSize(int n) {
		// TODO Auto-generated method stub
		return getFlowManager().getFlowSize(n);
	}

	public Json takeFromFlow(int n) {
		return getFlowManager().takeFromFlow(n);
	}

	public void addToFlow(int i, Json j) {
		// TODO Auto-generated method stub
		getFlowManager().addToFlow(i, j);
	}
	
	
	//== send via  UDP


	// to be used by module
	// host, port is the target name and port
	 public void sendObjectTo(String host,int port,String idOdTarget,int flowNumber,  Json objectToSend,
			boolean acquit)  {

		//String msg=encapsulateObject(idOdTarget, flowNumber, varName, objectToSend );
		String msg=encapsulateObject(idOdTarget, flowNumber ,  objectToSend,acquit).toString();
		sendEncapsulatedMsg(host, port, msg);
	}

	public void sendVarValueTo(String host,int port,String idOdTarget, CSLVariable v, boolean acquit)   {
		//mettre le flowNumber a -1 et utiliser
		//nameOfVar
		//data
		String msg=encapsulateVariable(idOdTarget, v,acquit).toString();
		sendEncapsulatedMsg(host, port, msg);

	}


	 public Json encapsulateObject(String targetID, int n,Json j,boolean acquit) {
		Json obj =Json.object();
		obj.at("fromPort",getCurrentPortForUCP());
		obj.at("idOfTarget", targetID);
		obj.at("flowNumber", ""+n);
		//obj.at("nameOfVariable", varName);

		if (acquit) obj.at("fromPort",getCurrentPortForUCP());

		obj.at("data",j);

		return obj;

	}

	public Json encapsulateVariable(String targetID,CSLVariable v, boolean acquit) {
		Json obj =Json.object();
		obj.at("fromPort",getCurrentPortForUCP());
		obj.at("idOfTarget", targetID);
		obj.at("flowNumber", ""+(-1));
		obj.at("nameOfVariable", v.getName());

		if (acquit) obj.at("fromPort",getCurrentPortForUCP());

		obj.at("data",v.getAsString());

		return obj;

	}


	public void sendEncapsulatedMsg(String host, int port, String msg) {
		CSLContext.instance.logInfo("   sending to "+host+':'+port+" >>>> msg:"+msg);
		try {
			//String host = "localhost";
			//int port = 9000;

			byte[] message = msg.getBytes();

			// Get the internet address of the specified host
			InetAddress address = InetAddress.getByName(host);

			// Initialize a datagram packet with data and address
			DatagramPacket packet = new DatagramPacket(message, message.length,
					address, port);

			// Create a datagram socket, send the packet through it, close it.
			DatagramSocket dsocket = new DatagramSocket();
			dsocket.send(packet);
			dsocket.close();
		} catch (Exception e) {
			//System.err.println(e);
		}
	}




	

}
