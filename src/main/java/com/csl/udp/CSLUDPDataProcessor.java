package com.csl.udp;

import com.csl.udp.CSLFlowManager;
import com.csl.util.CorrelationUtils;
import com.ucsl.json.Json;

import java.util.concurrent.BlockingQueue;

/**
 * Runnable class that treats the alerts from suricata stored in a Queue
 */
public class CSLUDPDataProcessor implements Runnable {
	private final BlockingQueue<CSLUdpUnicastClient.CorrelatedMessage> messageQueue;
	CSLFlowManager flowManager;
	private boolean traceAllMessages=false;
	boolean interrupted=false;
	

	public CSLUDPDataProcessor(CSLFlowManager fw, BlockingQueue<CSLUdpUnicastClient.CorrelatedMessage> messageQueue, boolean traceAllMessages) {
		this.messageQueue = messageQueue;
		this.flowManager=fw;
		this.traceAllMessages=traceAllMessages;
	}

	
	public void stop() {
		interrupted=true;
		
		try {
			this.messageQueue.put(new CSLUdpUnicastClient.CorrelatedMessage("","stop".getBytes()));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Function that runs in a thread and reads the alerts from the Queue list and send them to
	 * add to the flow {@link CSLFlowManager}.
	 */
	@Override
	public void run() {
		int counter = 0;
		while (!interrupted){
			try {
				/**
				 * Try and take a message from the queue. Will block if the
				 * message queue is empty, until an element becomes available.
				 */
				CSLUdpUnicastClient.CorrelatedMessage message = this.messageQueue.take();
				CorrelationUtils.setXCorrelationId(message.getXCorrelationId());
				byte[] rawData = message.getBytes();

				if (interrupted) {
					System.out.println("STOP UDP");
					break;
				}
				
				if (traceAllMessages) System.out.println("UDP received new message");


				String msg = new String(rawData, 0, rawData.length);

				try {
					Json jsonObject = Json.read(msg);

					if (traceAllMessages) System.out.println(" Json msg:"+jsonObject);

					Json jdata=jsonObject.get("data");
					String id="cible";
					if (jsonObject.has("idOfTarget")) id=jsonObject.get("idOfTarget").asString();
					
					String fn="1"; 
					if (jsonObject.has("flowNumber") )  fn=jsonObject.get("flowNumber").asString();
					
					String fromPort="8001";
					if (jsonObject.has("fromPort") ) fromPort=jsonObject.get("fromPort").asString();

					if (fromPort!=null ) {
					}
					int n=Integer.parseInt(fn);
					
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