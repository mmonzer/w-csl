package com.csl.udp;

import com.csl.alert.AlertDescriptor;
import com.csl.alert.CSLAlertManager;
import com.csl.core.CSLContext;
import com.csl.core.Config;
import com.csl.monitor.ActivityMonitor;
import com.csl.util.EveMessageUtill;
import com.ucsl.interfaces.IAlertLevel;
import com.ucsl.interfaces.ICSLFlowListener;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class CSLFlowManager {
    private static final Logger logger = LoggerFactory.getLogger(CSLFlowManager.class);

    int maxflows = 10;
    int maxsize = 1000;
    boolean traceAllMessages = true;
    CSLUDPDataProcessor dataProcessor = null;
    ExecutorService executorService = null;
    ActivityMonitor activityMonitor = new ActivityMonitor();
    Runnable listener;

    private long currentTime;
    CSLUdpUnicastClient client = null;

    // queues
    LinkedBlockingQueue<Json> inputflows;
    List<List<ICSLFlowListener>> listeners = new ArrayList<List<ICSLFlowListener>>();

    public CSLFlowManager(int maxflows, int maxsize, boolean trace) {
        this.maxflows = maxflows;
        this.maxsize = maxsize;
        this.traceAllMessages = trace;

        inputflows = new LinkedBlockingQueue<Json>();
    }

    public void addListener(int n, ICSLFlowListener l) {
        if ((n < 0) | (n >= maxflows)) {
            logger.error("Invalid flow number " + n + " (max=" + maxflows + ")");
        }
        listeners.get(n).add(l);
    }

    public int getFlowSize(int n) {
        return inputflows.size();
    }

    /**
     * Add an alert to the flow. It calls the listeners to treat it and stoke it to the input flows
     *
     * @param flowNumber number of the flow from the alert
     * @param alertData  data of the alert
     * @return true if the alert was added to the input flows, otherwise false.
     */
    public boolean addToFlow(int flowNumber, Json alertData) {
        inputflows.add(alertData);
        return true;
    }

    public void startListener() {
        String ip = CSLContext.getInstance().getCslUDPServer().getCurrentIPForUDP();
        int port = CSLContext.getInstance().getCslUDPServer().getCurrentPortForUDP();

        /**
         * The initial capacity for the blocking collection needs to be fine tuned
         * based on your application requirements.
         */
        BlockingQueue<CSLUdpUnicastClient.CorrelatedMessage> messageQueue = new ArrayBlockingQueue<>(1200);

        // message queue is shared between UDP client and Data Processor
        client = new CSLUdpUnicastClient(ip, port, messageQueue, traceAllMessages);
        dataProcessor = new CSLUDPDataProcessor(this, messageQueue, traceAllMessages);

        /**
         * Execute the components as 3 different threads
         */
        try {
            executorService = Executors.newFixedThreadPool(5);
            executorService.submit(client);
            executorService.submit(dataProcessor);
            executorService.submit(listener);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void stopListener() {
        client.stop();
        dataProcessor.stop();
        if (executorService != null) executorService.shutdownNow();
    }

    public void init() {
        Config.IdsConf config = Config.instance.IdsConf;


        boolean showTicks = config.getShowTicks();

        activityMonitor.setShowTicks(showTicks);

        int maxHistSize = config.getHistoryLength();
        activityMonitor.setMaxHistorySize(maxHistSize);

        activityMonitor.startTicTask();

        CSLContext.getInstance().getStatusNotifier().registerStatusProvider("taps", activityMonitor);

        listener = () -> {
            try {
                d(inputflows.take());
            } catch (InterruptedException e) {
                // exception handling
            }
        };
    }

    private void d(Json jsonAlert) {
        try {
            if (jsonAlert.has("type")) {
                String type = jsonAlert.get("type").asString();
                if (type.compareTo("EVE") == 0) {
                    EveMessageUtill.reformatTimeStamp(jsonAlert);
                    processSuricataEvent(jsonAlert);
                } else if (type.compareTo("TIC") == 0) {
                    activityMonitor.processEvent(jsonAlert);
                } else {
                    throw new UnsupportedOperationException("Not implemented yet");
                }
            }
        } catch (Exception e) {
            System.out.println("Exception while processing " + jsonAlert);
            System.out.println(e);
        }
    }

    /**
     * Update the time of the alert and generate an Alert to forward
     *
     * @param event event from a suricata Alert
     */
    public void processSuricataEvent(Json event) {
        updateTime(event);
        generateAlertFromSuricataEvent(event);
    }

    /**
     * From a raw Suricata alert, reformats it and calls {@link CSLAlertManager} to send the alert
     *
     * @param evtsInfo suricata alert information
     */
    private void generateAlertFromSuricataEvent(Json evtsInfo) {

        // test pour eve event

        boolean verbose = false;
        if (verbose) System.out.println("IDS Processing Info  " + evtsInfo);
        String code = "#suricata_alert";
        String msg = "#undef";

        if (evtsInfo.has("alert")) {

            Json j = evtsInfo.get("alert");
            if (j.has("signature")) {

                String s = j.get("signature").asString();
                if (s.startsWith("#")) {
                    int p = s.indexOf(" ");
                    if (p < 0) {
                        msg = s;
                    } else {
                        code = s.substring(1, p);
                        msg = s.substring(p + 1, s.length());
                    }
                } else msg = s;
            }

            if (j.has("category")) {
                evtsInfo.set("category", j.get("category").asString());
            } else
                evtsInfo.set("category", "#undef");


            if (j.has("severity")) {
                evtsInfo.set("severity", j.get("severity").asString());
            } else
                evtsInfo.set("severity", "0");


            evtsInfo.set("msg", msg);
            evtsInfo.set("code", code);

            if (verbose) System.out.println("Suricita alert code <" + code + ">:" + msg);
            if (verbose) System.out.println(JsonUtil.prettyPrint(evtsInfo));

            //if (evtsInfo.has("msg"))
            Json base_info = Json.object();
            base_info.set("timestamp", evtsInfo.at("timestamp"));
            base_info.set("flow_id", evtsInfo.at("flow_id"));
            base_info.set("in_iface", evtsInfo.at("in_iface"));
            base_info.set("event_type", evtsInfo.at("event_type"));
            base_info.set("src_ip", evtsInfo.at("src_ip"));
            base_info.set("src_port", evtsInfo.at("src_port"));
            base_info.set("dest_ip", evtsInfo.at("dest_ip"));
            base_info.set("dest_port", evtsInfo.at("dest_port"));
            base_info.set("proto", evtsInfo.at("proto"));
            //ajouter erxtra info for suricata ds alert

            AlertDescriptor alert = new AlertDescriptor().setLevelFromInt(IAlertLevel.INFO.getLevelAsInt()).setMsg(msg).setTime(getIDSCurrentTimeMillis())

                    .setProp("category", evtsInfo.get("category").asString())
                    .setProp("severity", evtsInfo.get("severity").asString())
                    .setMetaInfo("suricata_info", getEveInfo(j))
                    .setMetaInfo("base_info", base_info);

            CSLContext.getInstance().getCSLAlertManager().sendAlert(alert);
        } else {

            // System.out.println("Suricata EVE (not an alert)"+evtsInfo);
        }
    }

    private void updateTime(Json j) {

        long t = JsonUtil.getLongFromJson(j, "timestamp", -1); //j.get("timestamp").asLong();  // coorect value of time
        if (t < 0) {
            t = JsonUtil.getLongFromJson(j, "time", -1);
        }
        if (t < 0) {
            logger.error("Invalid time in  :" + j);
        } else {
            if (this.currentTime > t) {
                logger.error("Invalid time in  :" + j + " t=" + t + "  before last time:" + currentTime);
            }
            this.currentTime = t;
        }
    }

    public long getIDSCurrentTimeMillis() {
        return currentTime;//cuidsCurrentTimeMillis;
    }

    private Json getEveInfo(Json jj) {

        Json result = Json.object();
        for (Map.Entry<String, Json> e : jj.asJsonMap().entrySet()) {
            String key = e.getKey();
            if ((key.compareTo("timestamp") != 0)
                    && (key.compareTo("type") != 0))

                result.set(key, e.getValue());
        }
        return result;
    }
}

