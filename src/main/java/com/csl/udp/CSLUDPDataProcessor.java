package com.csl.udp;

import com.csl.util.CorrelationUtils;
import com.ucsl.json.Json;

import java.util.concurrent.BlockingQueue;

/**
 * Runnable class that treats the alerts from suricata stored in a Queue
 */
public class CSLUDPDataProcessor implements Runnable {
    private final BlockingQueue<CSLUdpUnicastClient.CorrelatedMessage> messageQueue;
    CSLFlowManager flowManager;
    private boolean traceAllMessages = false;
    boolean interrupted = false;

    public CSLUDPDataProcessor(CSLFlowManager fw, BlockingQueue<CSLUdpUnicastClient.CorrelatedMessage> messageQueue, boolean traceAllMessages) {
        this.messageQueue = messageQueue;
        this.flowManager = fw;
        this.traceAllMessages = traceAllMessages;
    }

    public void stop() {
        interrupted = true;

        try {
            this.messageQueue.put(new CSLUdpUnicastClient.CorrelatedMessage("", "stop".getBytes()));
        } catch (InterruptedException e) {
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
        while (!interrupted) {
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

                String msg = new String(rawData);

                try {
                    Json jsonObject = Json.read(msg);
                    Json jdata = jsonObject.get("data");

                    String fn = "1";
                    if (jsonObject.has("flowNumber")) fn = jsonObject.get("flowNumber").asString();

                    int n = Integer.parseInt(fn);

                    if (jdata.isArray()) {
                        for (Json jj : jdata.asJsonList()) {
                            flowManager.addToFlow(n, jj);
                        }
                    } else
                        flowManager.addToFlow(n, jdata);
                } finally {
                    Thread.sleep(3);
                }
            } catch (InterruptedException e) {
                interrupted = true;
                e.printStackTrace();
            }
        }
    }
}