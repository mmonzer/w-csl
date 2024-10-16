package com.wcsl.ids;

import com.csl.alert.AlertDescriptor;
import com.csl.alert.CSLAlertFactory;
import com.csl.alert.CSLAlertManager;
import com.csl.core.CSLContext;
import com.csl.core.Config;
import com.csl.defaultclasses.FileLogFactory;
import com.csl.defaultclasses.FileStoreService;
import com.ucsl.interfaces.*;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;

public class IDSMainProcessor {
    /**
     * Logger instance for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(IDSMainProcessor.class);

    private FileStoreService fileStoreServices;

    private IDSMainProcessorParams idsMainProcessorParams;

    private Config.IdsConf config;

    private IFileLogFactory fileLogFactory;

    private CSLAlertManager alertManager;

    private CSLAlertFactory alertFactory;

    private long currentTime;

    //=======================================================================================================================
    // Logger
    //
//    @Setter
//    @Getter
//    static private ICSLLogger logger1 = new DefaultLogger();

    //=======================================================================================================================
    public IDSMainProcessor(Config.IdsConf config, String cslConfDir) {
        this.fileStoreServices = new FileStoreService(cslConfDir);

        this.fileLogFactory = new FileLogFactory();

        this.idsMainProcessorParams = new IDSMainProcessorParams(this, config);

        this.alertFactory = new CSLAlertFactory();
    }

    public CSLAlertManager getDefaultAlertManager() {
        return CSLContext.instance.getCSLAlertManager();
    }

    public void setAlertFactory(CSLAlertFactory alertFactory) {
       this.alertFactory = alertFactory;
    }

    public CSLAlertFactory getAlertFactory() {
       return alertFactory;
    }

    public void init() {
        // TODO Auto-generated method stub

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

    public void setFileLogFactory(IFileLogFactory fileLogFactory) {
        this.fileLogFactory = fileLogFactory;
    }

    public void setFileStoreServices(FileStoreService fileUtils) {
       this.fileStoreServices = fileUtils;
    }

    public void setAlertManager(CSLAlertManager cslAlertManager) {
       this.alertManager = cslAlertManager;
    }

    public void saveJsonInModelDir(String dir, String fileName, Json j) {
        if (!dir.isEmpty())
            dir = idsMainProcessorParams.getIdsModelDir() + File.separator + dir;
        getFileStoreServices().saveJsonToFile(dir, fileName, j);
    }

    public Json readJsonFromModelDir(String dir, String fileName) {
        if (!dir.isEmpty())
            dir = idsMainProcessorParams.getIdsModelDir() + File.separator + dir;
        return getFileStoreServices().readJsonFromFile(dir, fileName);
    }

    public FileStoreService getFileStoreServices() {
       return fileStoreServices;
    }

    public IDSMainProcessorParams getIdsMainProcessorParams() {

        return idsMainProcessorParams;
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

            AlertDescriptor alert =
                    getAlertFactory().createAlertDescriptor(
                                    IAlertLevel.INFO.getLevelAsInt(),
                                    msg,
                                    getIDSCurrentTimeMillis()
                            )
                            .setProp("category", evtsInfo.get("category").asString())
                            .setProp("severity", evtsInfo.get("severity").asString())
                            .setMetaInfo("suricata_info", getEveInfo(j))
                            .setMetaInfo("base_info", base_info);

            CSLContext.instance.getCSLAlertManager().sendAlert(alert);
        } else {

            // System.out.println("Suricata EVE (not an alert)"+evtsInfo);
        }
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

    public long getIDSCurrentTimeMillis() {
        return currentTime;//cuidsCurrentTimeMillis;
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

    //=======================================================================================================================
    // Model based
    //

    public void removeAlertFromModel(AlertDescriptor a, int i) {

    }

    public void addAlertToModel(AlertDescriptor a, int level) {

    }

    //=======================================================================================================================
    // Learning

    public void saveLearnedModelAsNewLearnedModel() {
        System.err.println("Not implemented in basic version");
    }

    public Json getIDSVariables() {
        System.err.println("Not implemented in basic version");
        return null;
    }

    public String getProcessVariables() {
        System.err.println("Not implemented in basic version");
        return null;
    }

    public List<String> getErrors() {
        System.err.println("Not implemented in basic version");
        return null;
    }

    public String getIdsRulesSetAsString() {
        System.err.println("Not implemented in basic version");
        return null;
    }

    public Json getLearnedRules() {
        System.err.println("Not implemented in basic version");
        return null;
    }

    public void resetLearnedModel() {
        System.err.println("Not implemented in basic version");
    }

    public void backupLearnedModel() {
        System.err.println("Not implemented in basic version");
    }

    public void reverseBackupLearnedModel() {
        System.err.println("Not implemented in basic version");
    }

    public void renameLearnedRulesWithTimeStamp() {
        System.err.println("Not implemented in basic version");
    }

    public void getParamsAsJsonNameValueArray(Json j) {
        if (j == null) j = Json.array();
        if (!j.isArray()) j = Json.array();
    }

    public String getDirAndFileNamesInfo() {
       String s = "";
        return s;
    }
}
