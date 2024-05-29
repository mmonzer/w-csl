package com.csl.alert;

import com.csl.core.CSLContext;
import com.csl.core.CSLUtil;
import com.csl.logger.CSLLogger;
import com.csl.logger.FileLog;
import com.csl.web.jcmdoversocket.IAlertForwarder;
import com.csl.web.websockets.CSLWebSocket;
import com.ucsl.interfaces.*;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import lombok.Getter;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CSLAlertManager implements IAlertManager {

    public boolean NO_ALERT_FILTERING = true;

    @Getter
    public IAlertFactory alertFactory = new CSLAlertFactory();

    private IIDSMainProcessor idsMainProcessor = null;

    boolean FDEBUG = false;

    IAlertForwarder alertForwarder = null;

    private String loggerName;

    private int port;
    private boolean logToFile;
    private FileLog fileLog = null;
    private InetAddress iNetAddress = null;

    private boolean alert_to_web = true;
    private boolean alert_to_udp = true;

    private String filename_current_alerts = "";

    private Json jConfig = null;

    @Getter
    List<IAlertDescriptor> listOfCurrentAlerts = new ArrayList<>();

    private int durationOfAlert = 5000;
    private boolean doNotResendSameAlert = false;


    public CSLAlertManager(IIDSMainProcessor x, Json jConfig) {
        this.idsMainProcessor = x;
        this.idsMainProcessor.setAlertFactory(alertFactory);

        init(jConfig);
    }

    public CSLAlertManager setname(String loggerName) {
        if (loggerName.isEmpty()) return this;
        this.loggerName = loggerName;

        return this;
    }

    public Json getConfig() {
        return jConfig;
    }

    private void init(Json jConfig) {

        this.jConfig = jConfig;
        this.port = CSLUtil.getConfigIntegerValue(jConfig, "port", 4445);
        String ip = CSLUtil.getConfigStringValue(jConfig, "ip", "127.0.0.1"); //. j.get("ip").asString();

        try {
            this.iNetAddress = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        this.alert_to_web = CSLUtil.getConfigBooleanValue(jConfig, "alert_to_web", false);
        this.alert_to_udp = CSLUtil.getConfigBooleanValue(jConfig, "alert_to_udp", true);

        this.loggerName = CSLUtil.getConfigStringValue(jConfig, "name", "Alerts");

        this.filename_current_alerts = CSLUtil.getConfigStringValue(jConfig, "filename_current_alerts", "current_alerts");

        this.logToFile = CSLUtil.getConfigBooleanValue(jConfig, "logToFile", false);
        if (logToFile) {
            initFileLog();
        }

        this.durationOfAlert = CSLUtil.getConfigIntegerValue(jConfig, "alert_duration", 5000);
        this.doNotResendSameAlert = CSLUtil.getConfigBooleanValue(jConfig, "do_not_resent_same_alert", false);
    }

    private void initFileLog() {
        if (jConfig != null) {
            String datadir = CSLContext.instance.buildFullPathInUserDir(CSLUtil.getConfigStringValue(jConfig, "log_dir", "./logs"));
            String filename = CSLUtil.getConfigStringValue(jConfig, "prefix_filename", "alert");
            long max_size = CSLUtil.getConfigLongValue(jConfig, "max_size_of_log_files", 100000);
            this.fileLog = new FileLog(datadir, filename, max_size, CSLContext.instance::getSystemCurrentTimeMillis);

        }
    }

    public void sendAlert(IAlertDescriptor alertDescriptor) {
        sendAlert(alertDescriptor, true, false);
    }

    public void sendAlert(IAlertDescriptor alertDescriptor, boolean toViewer, boolean toLog) {
        if (findAlert(alertDescriptor) != null) return;

        System.out.println("ALERT=" + alertDescriptor);
        System.out.println("ALERT=" + alertDescriptor.toJson());

        listOfCurrentAlerts.add(alertDescriptor);
        send(alertDescriptor, toLog, toViewer);
    }

    private IAlertDescriptor findAlert(IAlertDescriptor alert) {
        if (NO_ALERT_FILTERING) return null;

        long t = CSLContext.instance.getTimeSystemCurrent();

        for (IAlertDescriptor a : listOfCurrentAlerts) {
            if (a.alertEqualTo(alert)) {
                if (doNotResendSameAlert) return a;

                if (t - a.getTime() < durationOfAlert)
                    return a;  // do not consider old alert
            }
        }
        return null;
    }

    private void send(IAlertDescriptor alert, boolean toFile, boolean toViewer) {
        if (this.alert_to_udp)
            this.sendAlertToViewerUDP(alert.toJson());

        if ((!toFile) && (!toViewer)) return;

        if (toFile | logToFile) {

            if (fileLog == null) initFileLog();
            if (fileLog == null) {
                CSLLogger.instance.error("Cannot log CSLAlert to file ");
                return;
            }
            if (alert.hasProps())
                fileLog.send("[" + alert.getLevelAsString() + "] " + alert.getMsg() + "  " + alert.getPropsList());
            else
                fileLog.send("[" + alert.getLevelAsString() + "] " + alert.getMsg());

        }

        if (toViewer) {

            if (this.alert_to_web)
                this.sendAlertToViewerWeb(alert);
        }

    }

    public void registerAlertForwarder(IAlertForwarder af) {
        this.alertForwarder = af;
    }

    public void sendAlertToViewerUDP(Json jalert) {
        // in client
        if (alertForwarder != null) {
            alertForwarder.sendAlert(jalert);
        } else {   // in server

            if (iNetAddress == null) {
                CSLLogger.instance.error("Invalid IP for alert viewer");
            }
            jalert.set("type", "alert");

            String msg = jalert.toString();
            // region -- forward alerts to the Alert Listener
            // TODO: Send the alert directly to DB-API instead of using UDP Socket (This requires the implementation of authentication)
            byte[] data = msg.getBytes();
            DatagramSocket s;
            try {
                s = new DatagramSocket();
                s.connect(this.iNetAddress, port);
                DatagramPacket payload = new DatagramPacket(data, data.length);
                s.send(payload);
                s.disconnect();
                s.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            // endregion -- forward alerts to the Alert Listener
        }
    }

    private void sendAlertToViewerWeb(IAlertDescriptor alert) {
        Json jAlert = Json.object();
        jAlert.set("type", "newAlert");
        Json jAlertInfo = alertToJsonForHmi(alert); //Json.object();
        jAlert.set("alertInfo", jAlertInfo);

        CSLWebSocket.broadcastMessageJson(CSLWebSocket.WEB_SOCKET_ALERT, jAlert);

        if (FDEBUG) {
            System.out.println("SENDING TO WEB SOCKET:" + jAlert);
        }
    }

    public Json getListOfCurrentAlertsAsJson() {
        Json jarray = Json.array();

        for (IAlertDescriptor alert : getListOfCurrentAlerts()) {

            if ((!alert.isMasked()) && (!alert.isAdded_to_model()))
                jarray.add(alertToJsonForHmi(alert));
        }
        return jarray;
    }

    public Json getNumberOfCurrentAlertsAsJsonByLevel() {
        Json jarray = Json.array();

        int[] count = new int[5];

        for (IAlertDescriptor alert : getListOfCurrentAlerts()) {

            if ((!alert.isMasked()) && (!alert.isAdded_to_model())) {

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

        for (IAlertDescriptor alert : getListOfCurrentAlerts()) {

            if ((alert.isMasked()) || (alert.isAdded_to_model()))
                jarray.add(alertToJsonForHmi(alert));

        }
        return jarray;
    }

    public Json getListOfMaskedAlertsAsJson() {
        Json jarray = Json.array();

        for (IAlertDescriptor alert : getListOfCurrentAlerts()) {
            if ((alert.isMasked()))
                jarray.add(alertToJsonForHmi(alert));

        }
        return jarray;
    }

    public Json getListOfAckedAlertsAsJson() {
        Json jarray = Json.array();

        for (IAlertDescriptor alert : getListOfCurrentAlerts()) {
            if ((alert.isAcked()))
                jarray.add(alertToJsonForHmi(alert));

        }
        return jarray;
    }

    public Json getListOfAddedToModelAlertsAsJson() {
        Json jarray = Json.array();

        for (IAlertDescriptor alert : getListOfCurrentAlerts()) {
            if (alert.isAdded_to_model())
                jarray.add(alertToJsonForHmi(alert));
        }
        return jarray;
    }

    public Json getListOfAllAlertsAsJson() {
        Json jarray = Json.array();

        for (IAlertDescriptor alert : getListOfCurrentAlerts()) {
            jarray.add(alertToJsonForHmi(alert));
        }
        return jarray;
    }

    public Json alertToJsonForHmi(IAlertDescriptor alert) {
        Json jAlertInfo = Json.object();

        jAlertInfo.set("alert_id", alert.getUuid());
        jAlertInfo.set("timeStamp", alert.getTime());
        jAlertInfo.set("timeStampEndMask", alert.getTimeForEndOfMask());

        jAlertInfo.set("level", alert.getLevelAsString());
        jAlertInfo.set("ilevel", alert.getLevelAsInt());

        jAlertInfo.set("message", alert.getMsg());
        jAlertInfo.set("masked", alert.isMasked());
        jAlertInfo.set("added_to_model", alert.isAdded_to_model());
        jAlertInfo.set("model_level", alert.getLevelForModel());


        jAlertInfo.set("moreInfoIT", alert.getMetaInfo(IAlertDescriptor.ALERT_INFO_FROM_IDS));
        jAlertInfo.set("moreInfoOT", alert.getMetaInfo(IAlertDescriptor.ALERT_INFO_FROM_SYSLEARNER));

        if (alert.hasProps()) {
            for (String key : alert.getPropsList().keySet()) {
                jAlertInfo.set(key, alert.getPropsList().get(key));
            }
        }

        Map<String, String> props = alert.getPropsList();

        if (props != null) {
            String s = "";
            String z;
            for (String key : props.keySet()) {
                jAlertInfo.set(key, props.get(key));
            }
        }
        return jAlertInfo;
    }

    public Json saveListOfCurrentAlerts() {
        Json jlist = Json.array();
        for (IAlertDescriptor a : listOfCurrentAlerts) {
            jlist.add(a.toJson());
        }
        CSLContext.instance.getDatabaseServer().saveJsonAsDataFile(this.filename_current_alerts,
                Json.object().set("contents", jlist), true);

        return jlist;
    }

    public Json resetListOfCurrentAlerts( /*IDSParams idsParams*/) {
        listOfCurrentAlerts.clear();
        saveListOfCurrentAlerts();

        return Json.array();
    }

    public IAlertDescriptor getAlert(String id) {

        for (IAlertDescriptor a : listOfCurrentAlerts) {
            if (a.getUuid().compareTo(id) == 0) return a;
        }

        return null;
    }

    //
    // op : reset_list

    // op : set_acked			id_alert	value
    //		set_masked
    //		add_to_model	id_alert	alert_level 0, 1, 2, 3, 4 or -1 UNDEF
    //		remove_from_model

	/*  Etat d'une alerte
	 * 		initialement ack : false, ce qui signifie qu'elle n'a pas été vue 
	 * 
	 * 		ack 			: true	acquittée  (c'est à dire non nouvelle) 
	 *		masked			: true	masquée, elle peut ne plus être affichée mais reste active
			added_to_model 	: true	elle n'est plus active car elle a été ajoutée au modèle
				Mets un niveau de risque et ajoute au modèle 
	 */

    public Json execOpAlert(Json params) {

        Json result = Json.object();
        String op = JsonUtil.getStringFromJson(params, "op", "");
        String alert_id = JsonUtil.getStringFromJson(params, "alert_id", "");

        if (op.compareToIgnoreCase("get_list_active") == 0) {  // msaked=false, added_to_mode= false

            return getListOfCurrentAlertsAsJson();

        } else if (op.compareToIgnoreCase("get_number_active_by_level") == 0) {  // msaked=false, added_to_mode= false

            return getNumberOfCurrentAlertsAsJsonByLevel();

        } else if (op.compareToIgnoreCase("get_list_acked") == 0) {  // masked=true or, added_to_mode= true

            return getListOfAckedAlertsAsJson();

        } else if (op.compareToIgnoreCase("get_list_masked") == 0) {  // masked=true or, added_to_mode= true

            return getListOfMaskedAlertsAsJson();

        } else if (op.compareToIgnoreCase("get_list_added_to_model") == 0) {  // masked=true or, added_to_mode= true

            return getListOfAddedToModelAlertsAsJson();

        } else if (op.compareToIgnoreCase("get_list_inactive") == 0) {  // masked=true or, added_to_mode= true

            return getListOfInactiveAlertsAsJson();

        } else if (op.compareToIgnoreCase("get_list_all") == 0) {

            return getListOfAllAlertsAsJson();

        } else if (op.compareToIgnoreCase("reset_list") == 0) {

            resetListOfCurrentAlerts(); //CSLContext.instance.getIdsParams());

        } else if (op.compareToIgnoreCase("dump_list") == 0) {

            Json list = getListOfAllAlertsAsJson();
            System.out.println(JsonUtil.prettyPrint(list));
            return list;

        } else if (op.compareToIgnoreCase("add_to_model") == 0) {


            IAlertDescriptor a = getAlert(alert_id);
            if (a == null) return Json.object().set("error", "alert not found (" + alert_id + ")");

            boolean b = JsonUtil.getBooleanFromJson(params, "value", false);

            if (b) {

                int level = JsonUtil.getIntFromJson(params, "level", 4); // from 0 to 4
                if (!a.isAdded_to_model()) idsMainProcessor.addAlertToModel(a, level); //.addToModel(level);
            } else {
                if (a.isAdded_to_model()) idsMainProcessor.removeAlertFromModel(a, 0); //a.removeFromModel();
            }

            return alertToJsonForHmi(a);
        } else if (op.compareToIgnoreCase("set_acked") == 0) {

            boolean b = JsonUtil.getBooleanFromJson(params, "value", false);
            IAlertDescriptor a = getAlert(alert_id);
            if (a == null) return Json.object().set("error", "alert not found (" + alert_id + ")");

            if (a != null) a.setAcked(b);
            return alertToJsonForHmi(a);

        } else if (op.compareToIgnoreCase("set_masked") == 0) {
            boolean b = JsonUtil.getBooleanFromJson(params, "value", false);

            long time_end = JsonUtil.getLongFromJson(params, "time_for_end_of_mask", 0);
            IAlertDescriptor alert = getAlert(alert_id);
            IAlertDescriptor a = getAlert(alert_id);
            if (a != null) {
                a.setMasked(b);
                a.setTimeForEndOfMask(time_end);
                return alertToJsonForHmi(a);
            } else {
                return Json.object().set("error", "alert not found (" + alert_id + ")");
            }
        } else if (op.compareToIgnoreCase("test") == 0) {
            result = test1();
        } else if (op.compareToIgnoreCase("test1") == 0) {
            result = test1();
        } else if (op.compareToIgnoreCase("test2") == 0) {
            test2();
        } else if (op.compareToIgnoreCase("debug_alert") == 0) {
            boolean b = JsonUtil.getBooleanFromJson(params, "value", false);

            FDEBUG = b;
            System.out.println("DEBUG ALERT:" + FDEBUG);
        } else {
            System.out.println("op_alert not found:" + params);
        }
        return result;

    }

    public Json getAlertStats() {
        int[] ctr = new int[5];
        int n = 0;
        for (IAlertDescriptor a : listOfCurrentAlerts) {
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

        IAlertDescriptor a = alertFactory.createAlertDescriptor(5, "ALERT", System.currentTimeMillis()); // AlertDescriptor(5, "ALERT");
        a.setProp("p1", "34");
        sendAlert(a);
        list.add(a.toJson());


        a = alertFactory.createAlertDescriptor(1, "ALERT level 1", System.currentTimeMillis());
        a.setMsg("This is a test green ");
        a.setProp("p1", "34");
        a.setProp("t", "" + System.currentTimeMillis());
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

        IAlertDescriptor a = alertFactory.createAlertDescriptor(5, "ALERT", System.currentTimeMillis());

        a.setProp("p1", "34");
        sendAlert(a);

        saveListOfCurrentAlerts();
    }

}
