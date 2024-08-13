package com.csl.udp;

import com.csl.core.CSLContext;
import com.ucsl.interfaces.ICSLFlowListener;
import com.ucsl.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CSLFlowManager {
    private static final Logger logger = LoggerFactory.getLogger(CSLFlowManager.class);

    int maxflows = 10;
    int maxsize = 1000;
    boolean traceAllMessages = true;
    CSLUDPDataProcessor dataProcessor = null;
    ExecutorService executorService = null;

    CSLUdpUnicastClient client = null;

    // queues
    List<BlockingQueue<Json>> inputflows;
    List<List<ICSLFlowListener>> listeners = new ArrayList<List<ICSLFlowListener>>();

    public CSLFlowManager(int maxflows, int maxsize, boolean trace) {
        // TODO Auto-generated constructor stub
        this.maxflows = maxflows;
        this.maxsize = maxsize;
        this.traceAllMessages = trace;

        inputflows = new ArrayList<BlockingQueue<Json>>();
        for (int i = 0; i < maxflows; i++) {
            BlockingQueue<Json> b = new ArrayBlockingQueue<>(maxsize);
            inputflows.add(b);
            listeners.add(new ArrayList<ICSLFlowListener>());
        }
    }

    public void addListener(int n, ICSLFlowListener l) {
        if ((n < 0) | (n >= maxflows)) {
            logger.error("Invalid flow number " + n + " (max=" + maxflows + ")");
        }
        listeners.get(n).add(l);
    }

    public boolean isFlowEmpty(int n) {
        if ((n < 0) | (n >= maxflows)) {
            logger.error("Invalid flow number " + n + " (max=" + maxflows + ")");
            return true;
        }
        return inputflows.get(n).isEmpty();
    }

    public int getFlowSize(int n) {
        // TODO Auto-generated method stub
        return inputflows.get(n).size();
    }

    public Json takeFromFlow(int n) {
        if ((n < 0) | (n >= maxflows)) {
            logger.error("Invalid flow number " + n + " (max=" + maxflows + ")");
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

    /**
     * Add an alert to the flow. It calls the listeners to treat it and stoke it to the input flows
     *
     * @param flowNumber number of the flow from the alert
     * @param alertData  data of the alert
     * @return true if the alert was added to the input flows, otherwise false.
     */
    public boolean addToFlow(int flowNumber, Json alertData) {

        if ((flowNumber < 0) | (flowNumber >= maxflows)) {
            logger.error("Invalid flow number " + flowNumber + " (max=" + maxflows + ")");
            return false;
        }


        boolean addToQueue = true;

        if (traceAllMessages) System.out.println("  dispatch -->" + alertData);
        for (ICSLFlowListener l : listeners.get(flowNumber)) {

            int result = ICSLFlowListener.DO_NOTHING;
            result = l.newElementOnQueue(alertData);

            if (traceAllMessages) System.out.println("    " + l.getName() + " return " + result);

            if (result == ICSLFlowListener.REMOVE_FROM_QUEUE)
                addToQueue = false;
            else if (result == ICSLFlowListener.CANCEL_OTHER_LISTENERS)
                break;
            else if (result == ICSLFlowListener.CANCEL_OTHER_LISTENERS_AND_REMOVE_FROM_QUEUE) {
                addToQueue = false;
                break;
            }
        }

        boolean ok = true;

        if (addToQueue) {
            ok = inputflows.get(flowNumber).offer(alertData);
        }

        if (traceAllMessages) System.out.println(" Queue size:" + getFlowSize(flowNumber));

        if (!ok) {
            logger.error("flow number " + flowNumber + " is full, lost of data: " + alertData);
            //if (traceAllMessages)
            System.err.println("flow number " + flowNumber + " is full, lost of data: " + alertData);
        }

        return ok;
    }

    public String dumpInputs() {
        String s = "";
        for (int i = 0; i < inputflows.size(); i++) {
            s = s + "Flow #" + i + ":" + inputflows.get(i).size() + " objects" + "\n";
        }
        return s;
    }

    public void startListener() {
        String ip = CSLContext.instance.getCslUDPServer().getCurrentIPForUCP();
        int port = CSLContext.instance.getCslUDPServer().getCurrentPortForUCP();

        /**
         * The initial capacity for the blocking collection needs to be fine tuned
         * based on your application requirements.
         */
        BlockingQueue<byte[]> messageQueue = new ArrayBlockingQueue<>(1200);

        // message queue is shared between UDP client and Data Processor
        client = new CSLUdpUnicastClient(ip, port, messageQueue, traceAllMessages);
        dataProcessor = new CSLUDPDataProcessor(this, messageQueue, traceAllMessages);

        /**
         * Execute the components as 3 different threads
         */
        try {
            executorService = Executors.newFixedThreadPool(3);
            executorService.submit(client);
            executorService.submit(dataProcessor);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void stopListener() {
        client.stop();
        dataProcessor.stop();
        if (executorService != null) executorService.shutdownNow();
    }
}

