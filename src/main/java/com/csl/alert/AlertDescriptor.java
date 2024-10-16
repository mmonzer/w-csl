package com.csl.alert;

import com.ucsl.interfaces.IAlertLevel;
import com.ucsl.json.Json;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class AlertDescriptor {

    IAlertLevel level = IAlertLevel.UNDEF;
    String msg = "";
    long time = 0;
    String uuid = "";
    private boolean acked = false;
    private boolean masked = false;
    private boolean added_to_model = false;
    private int levelForModel = -1;

    public static final String ALERT_INFO_FROM_SYSLEARNER = "AlertInfoFromSysLearner";
    public static final String ALERT_INFO_FROM_IDS = "AlertInfoFromIDS";

    long time_for_end_of_mask = 0;
    ;  // (<=0 if no end )

    Map<String, String> propsList = null;

    Map<String, Json> metaInfos = new HashMap<String, Json>();

    public Json toJson() {

        Json j = Json.object();
        j.set("level", level.toString());
        j.set("msg", msg);
        j.set("uuid", uuid);
        j.set("time", time);
        j.set("time_for_end_of_mask", time_for_end_of_mask);

        j.set("acked", acked);
        j.set("masked", masked);
        j.set("added_to_model", added_to_model);


        if (hasProps()) j.set("props", propsToJson());

        j.set("meta_infos", metasToJson());

        return j;
    }

    public AlertDescriptor() {
        this.uuid = UUID.randomUUID().toString();
        this.time = System.currentTimeMillis(); // default
    }

    public boolean alertEqualTo(AlertDescriptor a) {
        if (msg.compareToIgnoreCase(a.getMsg()) != 0) return false;
        if (level.getLevelAsInt() != a.getLevelAsInt()) return false;


        if (getPropsAsString().compareToIgnoreCase(a.getPropsAsString()) != 0) return false;
        return true;
    }

    public AlertDescriptor setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public Map<String, String> getPropsList() {
        if (propsList == null) propsList = new HashMap<String, String>();

        return propsList;
    }

    public Map<String, Json> getMetaInfos() {
        if (metaInfos == null) metaInfos = new HashMap<String, Json>();

        return metaInfos;
    }

    public String getLevelAsString() {
        return getLevel().getLevellAsString();
    }

    public String toString() {
        return "[" + level.toStringWithIndex() + "] " + msg + "<" + getPropsAsString() + ">";
    }

    public String getPropsAsString() {

        if (propsList == null) return "";

        String s = "";

        for (Map.Entry<String, String> entry : getPropsList().entrySet()) {
            //System.out.println(" Prop:"+entry.getKey() + "=" + entry.getValue());
            String key = entry.getKey();
            String value = entry.getValue();
            if (!s.isEmpty()) s = s + ";";
            s = s + key + "=" + value;
        }

        return s;
    }

    public long getTimeForEndOfMask() {
        return time_for_end_of_mask;
    }

    public AlertDescriptor setTimeForEndOfMask(long time_for_end_of_mask) {
        this.time_for_end_of_mask = time_for_end_of_mask;
        return this;
    }

    public boolean hasProps() {
        if (propsList == null) return false;
        return (propsList.size() > 0);
    }

    public Json propsToJson() {
        Json jarray = Json.array();

        for (Map.Entry<String, String> entry : getPropsList().entrySet()) {
            //System.out.println(entry.getKey() + "/" + entry.getValue());
            String key = entry.getKey();
            String value = entry.getValue();
            jarray.add(Json.object().set("key", key).set("value", value));
        }

        return jarray;
    }

    public Json metasToJson() {
        Json jarray = Json.array();

        for (Map.Entry<String, Json> entry : metaInfos.entrySet()) {
            //System.out.println(entry.getKey() + "/" + entry.getValue());
            String key = entry.getKey();
            Json value = entry.getValue();
            jarray.add(Json.object().set("key", key).set("value", value));
        }

        return jarray;
    }

    public AlertDescriptor setUuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public int getLevelAsInt() {
        return level.getLevelAsInt();
    }

    public AlertDescriptor setLevelFromInt(int l) {
        this.level = IAlertLevel.getAlertLevelFromInt(l);
        return this;
    }

    public AlertDescriptor setTime(long time) {
        this.time = time;
        return this;
    }

    public AlertDescriptor setMetaInfo(String name, Json value) {
        getMetaInfos().put(name, value);
        return this;
    }

    public Json getMetaInfo(String name) {
        Json j = getMetaInfos().get(name);
        if (j != null) return j;
        return Json.object();
    }

    public AlertDescriptor setProp(String name, String value) {
        getPropsList().put(name, value);
        return this;
    }
}
