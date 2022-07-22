package com.csl.udp;
import java.util.concurrent.BlockingQueue;

import com.xcsl.json.Json;

public class CSLUDPDataProcessor implements Runnable {
	private final BlockingQueue<byte[]> messageQueue;
	CSLFlowManager flowManager;
	private boolean traceAllMessages=false;
	boolean interrupted=false;
	

	public CSLUDPDataProcessor(CSLFlowManager fw,BlockingQueue<byte[]> messageQueue,boolean traceAllMessages) {
		this.messageQueue = messageQueue;
		this.flowManager=fw;
		this.traceAllMessages=traceAllMessages;
	}

	
	public void stop() {
		interrupted=true;
		
		try {
			this.messageQueue.put("stop".getBytes());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		int counter = 0;
		while (!interrupted){
			try {
				/**
				 * Try and take a message from the queue. Will block if the
				 * message queue is empty, until an element becomes available.
				 */
				//CSLContext.context.logInfo("Loop received");
				byte[] rawData = this.messageQueue.take();

				if (interrupted) {
					System.out.println("STOP UDP");
					break;
				}
				
				if (traceAllMessages) System.out.println("UDP received new message");


				String msg = new String(rawData, 0, rawData.length);

				try {
					Json jsonObject = Json.read(msg);

					if (traceAllMessages) System.out.println(" Json msg:"+jsonObject);

					// System.out.println(" JMFJson msg:"+jsonObject);

					Json jdata=jsonObject.get("data");
					//System.out.println("data="+jdata);
					String id="cible";
					if (jsonObject.has("idOfTarget")) id=jsonObject.get("idOfTarget").asString();
					
					String fn="1"; 
					if (jsonObject.has("flowNumber") )  fn=jsonObject.get("flowNumber").asString();
					
					String fromPort="8001";
					if (jsonObject.has("fromPort") ) fromPort=jsonObject.get("fromPort").asString();

					if (fromPort!=null ) {
						//	new UDPSend().sendMsg(msg);  // answer ok
						//System.err.println("acquit not implemented");
					}

					int n=Integer.parseInt(fn);

				//System.out.println("idOfTarget="+id+" fromPort="+fromPort+" flowNumber="+n);
					//flowManager.addToFlow(n, jdata);
					
					if (jdata.isArray()) {
						for (Json jj:jdata.asJsonList()) {
							flowManager.addToFlow(n, jj);
						}
					}
					else
						flowManager.addToFlow(n, jdata);
					
					
				} catch (Exception e) {

					System.out.println("Received invalid UDP packet "+msg);
					System.out.println(e);
				}




				/**
				 * Simulate a 3 ms delay
				 */
				Thread.sleep(3);
			} catch (InterruptedException e) {
				System.out.println("Interrupted");
				interrupted=true;
				e.printStackTrace();
			}
		}
	}
}