package com.csl.alert;

import com.csl.core.CSLContext;
import com.csl.core.Config;
import com.csl.defaultclasses.FileLog;
import com.csl.web.jcmdoversocket.IAlertForwarder;
import com.ucsl.interfaces.IAlertLevel;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*  CONFIG

 "alert_viewer":{
		"ip":"localhost",
		"port":4445,
		"name":"My Alerts",
		"logToFile":true,
		"data_dir":"./myappdata2/alerts",
		"prefix_filename":"AL",
		"max_size_of_log_files":10000

	}

 (asme leval as modules)
 Œ
 * 
 */
public class CSLAlertManager {
    /**
     * Logger instance for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(CSLAlertManager.class);
    public static final String ERROR = "error";
    public static final String VALUE = "value";

    public static final boolean NO_ALERT_FILTERING = true;
    private CSLAlertFactory alertFactory = new CSLAlertFactory();
    boolean FDEBUG = false;

    // id client, send over udp use sockets
    IAlertForwarder alertForwarder = null;
    private int port;

    private InetAddress iNetAddress = null;

    private Config.AlertViewer config = null;
    List<AlertDescriptor> listOfCurrentAlerts = new ArrayList<>();
    // if >0 , after this duration, the alert is cleared
    private int durationOfAlert = 5000;
    private boolean doNotResendSameAlert = false;
    private DbapiHandlerForAlerts dbapiHandler;

    public CSLAlertManager(Config.AlertViewer config) {
        dbapiHandler = new DbapiHandlerForAlerts();
        init(config);
    }

    private void init(Config.AlertViewer config) {
        this.config = config;

        this.port = config.getPort();
        String ip = config.getIp();

        try {
            this.iNetAddress = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            
            // e.printStackTrace();
        }

        boolean logToFile = config.isLogToFile();
        if (logToFile) {
            initFileLog();
        }

        this.durationOfAlert = config.getAlertDuration();
        this.doNotResendSameAlert = config.isDoNotResentSameAlert();
    }

    private void initFileLog() {
        if (config != null) {
            String datadir = CSLContext.getInstance().buildFullPathInUserDir(config.getLogDir());
            String filename = config.getPrefixFilename();
            long max_size = config.getMaxSizeOfLogFiles();
            new FileLog(datadir, filename, max_size, CSLContext.getInstance()::getSystemCurrentTimeMillis);
        }
    }

    public List<AlertDescriptor> getListOfCurrentAlerts() {
        return listOfCurrentAlerts;
    }

    /**
     * Checks if the alert is ok and calls the send function to forward it
     *
     * @param alertDescriptor alert in {@link AlertDescriptor} format
     */
    public void sendAlert(AlertDescriptor alertDescriptor) {
        if (findAlert(alertDescriptor) != null) {
            return;
        }

        listOfCurrentAlerts.add(alertDescriptor);

        this.dbapiHandler.insertAlert(alertDescriptor);
    }

    /**
     * Verify that the alert is in the list of current Alerts
     *
     * @param alert alert to verify
     * @return the alert if found and valid
     */
    private AlertDescriptor findAlert(AlertDescriptor alert) {


        if (NO_ALERT_FILTERING) return null;

        long t = CSLContext.getInstance().getSystemCurrentTimeMillis();

        for (AlertDescriptor a : listOfCurrentAlerts) {
            if (a.alertEqualTo(alert)) {
                if (doNotResendSameAlert) return a;

                if (t - a.getTime() < durationOfAlert) return a;  // do not consider old alert
            }
        }
        return null;
    }

    public void registerAlertForwarder(IAlertForwarder af) {
        this.alertForwarder = af;
    }

    /**
     * Send the alert either to csl-server either to the web client
     *
     * @param jalert alert in format {@link Json}
     */
    public void sendAlertToViewerUDP(Json jalert) {

        // in client
        if (alertForwarder != null) {
            alertForwarder.sendAlert(jalert);
        } else {   // in server


            if (iNetAddress == null) {
                logger.error("Invalid IP for alert viewer");
            }
            jalert.set("type", "alert");

            String msg = jalert.toString();
            // region -- forward alerts to the Alert Listener
            // TODO: Send the alert directly to DB-API instead of using UDP Socket (This requires the implementation of authentication)
            byte[] data = msg.getBytes();

            try (DatagramSocket datagramSocket = new DatagramSocket()){
                datagramSocket.connect(this.iNetAddress, port);
                DatagramPacket payload = new DatagramPacket(data, data.length);
                datagramSocket.send(payload);
                datagramSocket.disconnect();
            } catch (IOException e) {
                // e.printStackTrace();
            }
            // endregion -- forward alerts to the Alert Listener
        }
    }

    public Json getListOfCurrentAlertsAsJson() {
        Json jarray = Json.array();

        for (AlertDescriptor alert : getListOfCurrentAlerts()) {

            if ((!alert.isMasked()) && (!alert.isAddedToModel())) jarray.add(alertToJsonForHmi(alert));
        }


        return jarray;
    }

    public Json getNumberOfCurrentAlertsAsJsonByLevel() {
        Json jarray = Json.array();

        int[] count = new int[5];

        for (AlertDescriptor alert : getListOfCurrentAlerts()) {

            if ((!alert.isMasked()) && (!alert.isAddedToModel())) {

                int l = alert.getLevelAsInt();
                if (l < 0) l = 0;
                if (l > 4) l = 4;
                count[l]++;
            }
        }
        for (int i = 0; i < 5; i++) {
            Json j = Json.object();
            j.set("count", count[i]);
            j.set("name", IAlertLevel.getAlertLevelFromInt(i));
            jarray.add(j);
        }

        return jarray;
    }

    public Json getListOfInactiveAlertsAsJson() {
        Json jarray = Json.array();

        for (AlertDescriptor alert : getListOfCurrentAlerts()) {

            if ((alert.isMasked()) || (alert.isAddedToModel())) jarray.add(alertToJsonForHmi(alert));
        }


        return jarray;
    }

    public Json getListOfMaskedAlertsAsJson() {
        Json jarray = Json.array();

        for (AlertDescriptor alert : getListOfCurrentAlerts()) {

            if ((alert.isMasked())) jarray.add(alertToJsonForHmi(alert));
        }


        return jarray;
    }

    public Json getListOfAckedAlertsAsJson() {
        Json jarray = Json.array();

        for (AlertDescriptor alert : getListOfCurrentAlerts()) {

            if ((alert.isAcked())) jarray.add(alertToJsonForHmi(alert));
        }


        return jarray;
    }

    public Json getListOfAddedToModelAlertsAsJson() {
        Json jarray = Json.array();

        for (AlertDescriptor alert : getListOfCurrentAlerts()) {
            if (alert.isAddedToModel()) jarray.add(alertToJsonForHmi(alert));
        }


        return jarray;
    }

    public Json getListOfAllAlertsAsJson() {
        Json jarray = Json.array();

        for (AlertDescriptor alert : getListOfCurrentAlerts()) {
            jarray.add(alertToJsonForHmi(alert));
        }
        return jarray;
    }

    public Json alertToJsonForHmi(AlertDescriptor alert) {
        Json jAlertInfo = Json.object();

        jAlertInfo.set("alert_id", alert.getUuid());
        jAlertInfo.set("timeStamp", alert.getTime());
        jAlertInfo.set("timeStampEndMask", alert.getTimeForEndOfMask());

        jAlertInfo.set("level", alert.getLevelAsString());
        jAlertInfo.set("ilevel", alert.getLevelAsInt());

        jAlertInfo.set("message", alert.getMsg());
        jAlertInfo.set("masked", alert.isMasked());
        jAlertInfo.set("added_to_model", alert.isAddedToModel());
        jAlertInfo.set("model_level", alert.getLevelForModel());

        jAlertInfo.set("moreInfoIT", alert.getMetaInfo(AlertDescriptor.ALERT_INFO_FROM_IDS));
        jAlertInfo.set("moreInfoOT", alert.getMetaInfo(AlertDescriptor.ALERT_INFO_FROM_SYSLEARNER));

        if (alert.hasProps()) {
            for (String key : alert.getPropsList().keySet()) {
                jAlertInfo.set(key, alert.getPropsList().get(key));
            }
        }

        Map<String, String> props = alert.getPropsList();

        if (props != null) {
            for (String key : props.keySet()) {
                jAlertInfo.set(key, props.get(key));
            }
        }

        return jAlertInfo;
    }

    public Json resetListOfCurrentAlerts( /*IDSParams idsParams*/) {
        listOfCurrentAlerts.clear();
        //saveListOfCurrentAlerts();

        return Json.array();
    }

    public AlertDescriptor getAlert(String id) {

        for (AlertDescriptor a : listOfCurrentAlerts) {
            if (a.getUuid().compareTo(id) == 0) return a;
        }

        return null;
    }

    public Json execOpAlert(Json params) {

        Json result = Json.object();
        String op = JsonUtil.getStringFromJson(params, "op", "").toLowerCase();
        String alert_id = JsonUtil.getStringFromJson(params, "alert_id", "");

        switch (op) {
            case "get_list_active" -> {
                return getListOfCurrentAlertsAsJson();   // msaked=false, added_to_mode= false
            }
            case "get_number_active_by_level" -> {
                return getNumberOfCurrentAlertsAsJsonByLevel();   // msaked=false, added_to_mode= false
            }
            case "get_list_acked" -> {
                return getListOfAckedAlertsAsJson();   // masked=true or, added_to_mode= true
            }
            case "get_list_masked" -> {
                return getListOfMaskedAlertsAsJson();   // masked=true or, added_to_mode= true
            }
            case "get_list_added_to_model" -> {
                return getListOfAddedToModelAlertsAsJson();   // masked=true or, added_to_mode= true
            }
            case "get_list_inactive" -> {
                return getListOfInactiveAlertsAsJson();   // masked=true or, added_to_mode= true
            }
            case "get_list_all" -> {
                return getListOfAllAlertsAsJson();
            }
            case "reset_list" -> resetListOfCurrentAlerts();
            case "dump_list" -> getListOfAllAlertsAsJson();
            case "add_to_model" -> {
                AlertDescriptor a = getAlert(alert_id);
                if (a == null) return Json.object().set(ERROR, getMsgAlertNotFound(alert_id));
                return alertToJsonForHmi(a);
            }
            case "set_acked" -> {
                boolean b = JsonUtil.getBooleanFromJson(params, VALUE, false);
                AlertDescriptor a = getAlert(alert_id);
                if (a == null) return Json.object().set(ERROR, getMsgAlertNotFound(alert_id));
                a.setAcked(b);
                return alertToJsonForHmi(a);
            }
            case "set_masked" -> {
                boolean b = JsonUtil.getBooleanFromJson(params, VALUE, false);
                long time_end = JsonUtil.getLongFromJson(params, "time_for_end_of_mask", 0);
                AlertDescriptor a = getAlert(alert_id);
                if (a != null) {
                    a.setMasked(b);
                    a.setTimeForEndOfMask(time_end);
                    return alertToJsonForHmi(a);
                } else {
                    return Json.object().set(ERROR, getMsgAlertNotFound(alert_id));
                }
            }
            case "test1" -> result = test1();
            case "test2" -> test2();
            case "debug_alert" -> FDEBUG = JsonUtil.getBooleanFromJson(params, VALUE, false);
            default -> System.out.println("op_alert not found:" + params);
        }
        return result;
    }

    private static @NotNull String getMsgAlertNotFound(String alert_id) {
        return "alert not found (" + alert_id + ")";
    }

    public Json getAlertStats() {

        int[] ctr = new int[5];
        int n = 0;
        for (AlertDescriptor a : listOfCurrentAlerts) {
            int idx = a.getLevelAsInt();
            if (idx <= 0) ctr[0]++;
            else if (idx >= 4) ctr[4]++;
            else ctr[idx]++;
            n++;
        }
        Json j = Json.object();
        j.set("all", n);

        for (int i = 0; i <= 4; i++) {
            j.set("l" + i, ctr[i]);
        }


        return j;
    }

    private Json test1() {
        Json list = Json.array();

        AlertDescriptor a = alertFactory.createAlertDescriptor(5, "ALERT", System.currentTimeMillis()); // AlertDescriptor(5, "ALERT");
        a.setProp("p1", "34");
        sendAlert(a);
        list.add(a.toJson());

        a = alertFactory.createAlertDescriptor(1, "ALERT level 1", System.currentTimeMillis());
        a.setMsg("This is a test green ");
        a.setProp("p1", "34");
        a.setProp("t", "" + System.currentTimeMillis());
        //a.setLevel(new AlertLevel(1));
        sendAlert(a);
        list.add(a.toJson());

        a = alertFactory.createAlertDescriptor(2, "ALERT level 2", System.currentTimeMillis());
        a.setMsg("This is a test yellow ");
        a.setProp("t", "" + System.currentTimeMillis());

        sendAlert(a);
        list.add(a.toJson());

        a = alertFactory.createAlertDescriptor(3, "ALERT level 3", System.currentTimeMillis());
        a.setMsg("This is a test orange");
        a.setProp("t", "" + System.currentTimeMillis());

        sendAlert(a);
        list.add(a.toJson());

        a = alertFactory.createAlertDescriptor(4, "ALERT level 4", System.currentTimeMillis());
        a.setMsg("This is a test red");
        a.setProp("t", "" + System.currentTimeMillis());

        sendAlert(a);
        list.add(a.toJson());

        return list;
    }

    private void test2() {
        AlertDescriptor a = alertFactory.createAlertDescriptor(5, "ALERT", System.currentTimeMillis());

        a.setProp("p1", "34");
        sendAlert(a);

        //saveListOfCurrentAlerts();
    }
}
