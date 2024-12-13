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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class CSLFlowManager {
    private static final Logger logger = LoggerFactory.getLogger(CSLFlowManager.class);
    public static final String CATEGORY = "category";
    public static final String SEVERITY = "severity";
    public static final String TIMESTAMP = "timestamp";

    int maxflows = 10;
    int maxsize = 1000;
    CSLUDPDataProcessor dataProcessor = null;
    ExecutorService executorService = null;
    ActivityMonitor activityMonitor = new ActivityMonitor();
    Runnable listener;

    private long currentTime;
    CSLUdpUnicastClient client = null;

    // queues
    LinkedBlockingQueue<Json> inputflows;
    List<List<ICSLFlowListener>> listeners = new ArrayList<>();

    public CSLFlowManager(int maxflows, int maxsize) {
        this.maxflows = maxflows;
        this.maxsize = maxsize;

        inputflows = new LinkedBlockingQueue<>();
    }

    public void addListener(int n, ICSLFlowListener l) {
        if ((n < 0) | (n >= maxflows)) {
            logger.error("Invalid flow number {} (max={})",n, maxflows );
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
        client = new CSLUdpUnicastClient(ip, port, messageQueue);
        dataProcessor = new CSLUDPDataProcessor(this, messageQueue);

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
        Config.IdsConf config = Config.getInstance().idsConf;


        boolean showTicks = config.getShowTicks();

        activityMonitor.setShowTicks(showTicks);

        int maxHistSize = config.getHistoryLength();
        activityMonitor.setMaxHistorySize(maxHistSize);

        activityMonitor.startTicTask();

        CSLContext.getInstance().getStatusNotifier().registerStatusProvider("taps", activityMonitor);

        listener = () -> {
            try {
                filterJsonAlert(inputflows.take());
            } catch (InterruptedException e) {
                // exception handling
            }
        };
    }

    private void filterJsonAlert(Json jsonAlert) {
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

        if (evtsInfo.has("alert")) {

            Json alertInfo = evtsInfo.get("alert");

            if (alertInfo.has(CATEGORY)) {
                evtsInfo.set(CATEGORY, alertInfo.get(CATEGORY).asString());
            } else
                evtsInfo.set(CATEGORY, "#undef");


            if (alertInfo.has(SEVERITY)) {
                evtsInfo.set(SEVERITY, alertInfo.get(SEVERITY).asString());
            } else
                evtsInfo.set(SEVERITY, "0");

            String msg =setAlertMessageAndCodeFromSignature(evtsInfo, alertInfo);

            Json baseInfo = buildAlertInformation(evtsInfo);
            // ajouter extra info for suricata ds alert
            AlertDescriptor alert = new AlertDescriptor()
                    .setLevelFromInt(IAlertLevel.INFO.getLevelAsInt()).setMsg(msg).setTime(getIDSCurrentTimeMillis())
                    .setProp(CATEGORY, evtsInfo.get(CATEGORY).asString())
                    .setProp(SEVERITY, evtsInfo.get(SEVERITY).asString())
                    .setMetaInfo("suricata_info", getEveInfo(alertInfo))
                    .setMetaInfo("base_info", baseInfo);

            CSLContext.getInstance().getCSLAlertManager().sendAlert(alert);
        }
    }

    /**
     * From Signature of the Alert it detects the message and the code of the event. It addes to the event information
     * and returns the message.
     * @param evtsInfo event information object
     * @param alertInfo alert information
     * @return message obtained from signature
     */
    private static String setAlertMessageAndCodeFromSignature(Json evtsInfo, Json alertInfo) {
        String code = "#suricata_alert";
        String msg = "#undef";
        if (alertInfo.has("signature")) {

            String signatureAlert = alertInfo.get("signature").asString();
            if (signatureAlert.startsWith("#")) {
                int firstSpace = signatureAlert.indexOf(" ");
                if (firstSpace < 0) {
                    msg = signatureAlert;
                } else {
                    code = signatureAlert.substring(1, firstSpace);
                    msg = signatureAlert.substring(firstSpace + 1);
                }
            } else msg = signatureAlert;
        }
        evtsInfo.set("msg", msg);
        evtsInfo.set("code", code);

        return msg;
    }

    private static @NotNull Json buildAlertInformation(Json evtsInfo) {
        Json baseInfo = Json.object();
        baseInfo.set(TIMESTAMP, evtsInfo.at(TIMESTAMP));
        baseInfo.set("flow_id", evtsInfo.at("flow_id"));
        baseInfo.set("in_iface", evtsInfo.at("in_iface"));
        baseInfo.set("event_type", evtsInfo.at("event_type"));
        baseInfo.set("src_ip", evtsInfo.at("src_ip"));
        baseInfo.set("src_port", evtsInfo.at("src_port"));
        baseInfo.set("dest_ip", evtsInfo.at("dest_ip"));
        baseInfo.set("dest_port", evtsInfo.at("dest_port"));
        baseInfo.set("proto", evtsInfo.at("proto"));
        return baseInfo;
    }

    private void updateTime(Json j) {

        long t = JsonUtil.getLongFromJson(j, TIMESTAMP, -1); //j.get("timestamp").asLong();  // coorect value of time
        if (t < 0) {
            t = JsonUtil.getLongFromJson(j, "time", -1);
        }
        if (t < 0) {
            logger.error("Invalid time in : {}",  j);
        } else {
            if (this.currentTime > t) {
                logger.error("Invalid time in : {}, t={}  before last time : {}",j,t, currentTime);
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
            if ((key.compareTo(TIMESTAMP) != 0)
                    && (key.compareTo("type") != 0))

                result.set(key, e.getValue());
        }
        return result;
    }
}

