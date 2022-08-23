package com.csl.udp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.csl.core.CSLContext;
import com.xcsl.ids.IDSTrace;
import com.xcsl.interfaces.ICSLFlowListener;
import com.xcsl.json.Json;

public class CSLFlowManager {

	int maxflows=10;
	int maxsize=1000;
	boolean traceAllMessages=true;
//	private CSLContext cslContext;

	CSLUDPDataProcessor dataProcessor=null;
	ExecutorService executorService =null;

	CSLUdpUnicastClient client =null;
	
	// queues
	List<BlockingQueue<Json>> inputflows;
	List<List<ICSLFlowListener>> listeners= new ArrayList<List<ICSLFlowListener>>();
	
	
	public CSLFlowManager(int maxflows, int maxsize,boolean trace) {
		// TODO Auto-generated constructor stub
		this.maxflows=maxflows;
		this.maxsize=maxsize;
		this.traceAllMessages=trace;
		
		//this.cslContext=context;
		inputflows=new ArrayList<BlockingQueue<Json>>();
		for (int i=0;i<maxflows;i++) {
			BlockingQueue<Json> b=  new ArrayBlockingQueue<>(maxsize);
			inputflows.add(b);
			listeners.add(new ArrayList<ICSLFlowListener>());
		}


	}

	public void addListener(int n, ICSLFlowListener l) {
		if ((n<0)|(n>=maxflows)) {
			CSLContext.instance.logError("Invalid flow number "+n+" (max="+maxflows+")");
		}
		listeners.get(n).add(l);
	}
	
	public boolean isFlowEmpty(int n) {
		if ((n<0)|(n>=maxflows)) {
			CSLContext.instance.logError("Invalid flow number "+n+" (max="+maxflows+")");
			return true;
		}
		return inputflows.get(n).isEmpty();
	}

	public int getFlowSize(int n) {
		// TODO Auto-generated method stub
		return inputflows.get(n).size();
	}
	
	public Json takeFromFlow(int n) {
		if ((n<0)|(n>=maxflows)) {
			CSLContext.instance.logError("Invalid flow number "+n+" (max="+maxflows+")");
			return null;
		}
		if (inputflows.get(n).isEmpty()) return null;
		try {
			return inputflows.get(n).take();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	
//	public boolean addToFlow(int n, Json j) {
//
//		if ((n<0)|(n>=maxflows)) {
//			CSLContext.context.logError("Invalid flow number "+n+" (max="+maxflows+")");
//			return false ;
//		}
//		boolean ok= inputflows.get(n).offer(j);
//		//CSLContext.context.logInfo("added to flow #"+n+" ok="+ok+" o="+j);
//		if (!ok)
//			CSLContext.context.logError("flow number "+n+" is full, lost of data: "+j);
//
//		for (ICSLFlowListener l: listeners.get(n)) {
//			l.newElementOnQueue();
//		}
//		
//		//CSLContext.context.logInfo(dumpInputs());
//
//		return ok;
//
//	}
	
	
	public boolean addToFlow(int n, Json j) {

		if ((n<0)|(n>=maxflows)) {
			CSLContext.instance.logError("Invalid flow number "+n+" (max="+maxflows+")");
			return false ;
		}
		
		
		boolean addToQueue=true;
		
		if (traceAllMessages) System.out.println("  dispatch -->"+j); 
		for (ICSLFlowListener l: listeners.get(n)) {
	
			int result=ICSLFlowListener.DO_NOTHING;
			result= l.newElementOnQueue(j);
			
			if (traceAllMessages) System.out.println("    "+l.getName()+" return "+result);
			
			if (result==ICSLFlowListener.REMOVE_FROM_QUEUE) 
				addToQueue=false;
			else if (result==ICSLFlowListener.CANCEL_OTHER_LISTENERS) 
				break;
			else if (result==ICSLFlowListener.CANCEL_OTHER_LISTENERS_AND_REMOVE_FROM_QUEUE) {
				addToQueue=false;
				break;
			}
		}
		
		
		//CSLContext.context.logInfo(dumpInputs());

		boolean ok= true;
		
		if (addToQueue) {
			ok=inputflows.get(n).offer(j);
		}
		//CSLContext.context.logInfo("added to flow #"+n+" ok="+ok+" o="+j);
		
		if (traceAllMessages) System.out.println(" Queue size:"+getFlowSize(n));
		
		if (!ok) {
			CSLContext.instance.logError("flow number "+n+" is full, lost of data: "+j);
			//if (traceAllMessages) 
				System.err.println("flow number "+n+" is full, lost of data: "+j);
		}
		
		return ok;

	}



	public String dumpInputs() {
		String s="";
		for (int i=0;i<inputflows.size();i++) {
			s=s+"Flow #"+i+":"+ inputflows.get(i).size()+ " objects"+"\n";

		}

		return s;

	}



	public void startListener() {
		int port = CSLContext.instance.getCslUDPServer().getCurrentPortForUCP();
			//CSLContext.context.logInfo("Listening udp to port :"+port);
		IDSTrace.log(IDSTrace.UDP_TRACE, "Listening udp to port :"+port);
		
		/**
		 * The initial capacity for the blocking collection needs to be fine tuned
		 * based on your application requirements.
		 */
		BlockingQueue<byte[]> messageQueue = new ArrayBlockingQueue<>(1200);

		//UDPSendTest server = new UDPSendTest();
		// message queue is shared between UDP client and Data Processor
		client = new CSLUdpUnicastClient(port, messageQueue,traceAllMessages);
		dataProcessor = new CSLUDPDataProcessor(this,messageQueue,traceAllMessages);

		/**
		 * Execute the components as 3 different threads
		 */
		try {
		executorService = Executors.newFixedThreadPool(3);
		executorService.submit(client);
		// executorService.submit(server);
		executorService.submit(dataProcessor);
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public void stopListener() {
		
		client.stop();
		dataProcessor.stop();
		if (executorService!=null) executorService.shutdownNow();
	}

}

