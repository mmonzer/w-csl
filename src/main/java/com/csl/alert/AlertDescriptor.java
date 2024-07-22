package com.csl.alert;

import com.ucsl.interfaces.IAlertDescriptor;
import com.ucsl.interfaces.IAlertLevel;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlertDescriptor implements IAlertDescriptor{

    IAlertLevel level=IAlertLevel.UNDEF;
    String msg="";
    long time=0;
    String uuid="";
    private boolean acked=false;
    private boolean masked=false;
    private boolean added_to_model=false;
    private int levelForModel=-1;

	long time_for_end_of_mask=0;  ;  // (<=0 if no end )

	Map<String, String> propsList = null;

	Map<String, Json> metaInfos = new HashMap<String, Json>();

	public AlertDescriptor(Json j) {

		level = IAlertLevel.getAlertLevelFromString(JsonUtil.getStringFromJson(j,"level",""));
		msg=JsonUtil.getStringFromJson(j,"msg","");
		uuid= JsonUtil.getStringFromJson(j,"uuid","");
		time = JsonUtil.getLongFromJson(j,"time",0);
		time_for_end_of_mask = JsonUtil.getLongFromJson(j,"time_for_end_of_mask",0);

		if (j.has("props")) {
			for (Json jp:j.get("props").asJsonList()) {
				String key=JsonUtil.getStringFromJson(jp,"key","");
				String value=JsonUtil.getStringFromJson(jp,"value","");
				if (!key.isEmpty()) getPropsList().put(key,  value);
			}
		}
		
		if (j.has("meta_infos")) {
			for (Json jp:j.get("meta_infos").asJsonList()) {
				String key=JsonUtil.getStringFromJson(jp,"key","");
				Json value=jp.get("value");
				if (!key.isEmpty()) metaInfos.put(key,  value);
			}
		}
		
		if (j.has("details_ids")) metaInfos.put(ALERT_INFO_FROM_IDS, j.get("details_ids"));				
		if (j.has("details_sys")) metaInfos.put(ALERT_INFO_FROM_SYSLEARNER, j.get("details_sys"));

	}

	public Json toJson() {

		Json j= Json.object();
		j.set("level", level.toString());
		j.set("msg", msg);
		j.set("uuid",uuid);
		j.set("time",time);
		j.set("time_for_end_of_mask", time_for_end_of_mask);

		j.set("acked",acked);
		j.set("masked",masked);
		j.set("added_to_model",added_to_model);


		if (hasProps()) j.set("props", propsToJson());

		j.set("meta_infos", metasToJson());

		return j;

	}

	public AlertDescriptor() {
		this.uuid = UUID.randomUUID().toString();
		this.time=System.currentTimeMillis(); // default
	}

	public AlertDescriptor addProp(String name, String value) {
		if (propsList==null)  propsList = new HashMap<String, String>();

		propsList.put(name,  value);
		return this;
	}

	private void parseProps(String props) {

		if (!props.isEmpty()) {
			propsList = new HashMap<String, String>();

			String [] p=props.split(";");

			for(String keyvalue : p) {
				String[] x=keyvalue.split("=");
				if (x.length==2) propsList.put(x[0],x[1]);
			}
		}
	}

	public boolean alertEqualTo(IAlertDescriptor a) {
		if (msg.compareToIgnoreCase(a.getMsg())!=0) return false;
		if (level.getLevelAsInt()!=a.getLevelAsInt()) return false;
		
	
		if (getPropsAsString().compareToIgnoreCase(a.getPropsAsString())!=0) return false;
		return true;
	}

    public IAlertDescriptor setMsg(String msg) {
		this.msg = msg;
		return this;
	}

	public Map<String, String> getPropsList() {
		if (propsList==null)  propsList = new HashMap<String, String>();

		return propsList;
	}

	public Map<String,Json> getMetaInfos() {
		if (metaInfos==null) metaInfos= new HashMap<String,Json>();

		return metaInfos;
	}

	public String getLevelAsString() {
		return getLevel().getLevellAsString();

	}

	public String toString() {
		return "["+level.toStringWithIndex()+"] "+msg+ "<"+getPropsAsString()+">";
	}

	public String getPropsAsString() {
		
		if (propsList==null) return "";
		
		String s="";
		
		for (Map.Entry<String, String> entry : getPropsList().entrySet()) {
			//System.out.println(" Prop:"+entry.getKey() + "=" + entry.getValue());
			String key=entry.getKey();
			String value=entry.getValue();
			if (!s.isEmpty()) s=s+";";
			s=s+key+"="+value;

		}
		
		return s;

		
	}

	@Override
	public IAlertDescriptor setPropsFromString(String props) {
		
		parseProps(props);
		// TODO Auto-generated method stub
		return this;
	};

    public long getTimeForEndOfMask() {
		return time_for_end_of_mask;
	}

	public IAlertDescriptor setTimeForEndOfMask(long time_for_end_of_mask) {
		this.time_for_end_of_mask = time_for_end_of_mask;
		return this;
	}

	public boolean hasProps() {
		if (propsList==null) return false;
		return (propsList.size()>0);
	}

	public Json propsToJson() {
		Json jarray= Json.array();

		for (Map.Entry<String, String> entry : getPropsList().entrySet()) {
			//System.out.println(entry.getKey() + "/" + entry.getValue());
			String key=entry.getKey();
			String value=entry.getValue();
			jarray.add(Json.object().set("key",key).set("value", value));

		}

		return jarray;
	}

	public Json metasToJson() {
		Json jarray= Json.array();

		for (Map.Entry<String, Json> entry : metaInfos.entrySet()) {
			//System.out.println(entry.getKey() + "/" + entry.getValue());
			String key=entry.getKey();
			Json value=entry.getValue();
			jarray.add(Json.object().set("key",key).set("value", value));

		}

		return jarray;
	}

	@Override
	public IAlertDescriptor setUuid(String uuid) {
		// TODO Auto-generated method stub
		this.uuid=uuid;
		return this;
	}

	@Override
	public IAlertDescriptor setLevelFromString(String s) {
		// TODO Auto-generated method stub
		this.level=IAlertLevel.getAlertLevelFromString(s);
		return this;
	}

	@Override
	public int getLevelAsInt() {
		// TODO Auto-generated method stub
		return level.getLevelAsInt();
	}

	@Override
	public IAlertDescriptor setLevelFromInt(int l) {
		// TODO Auto-generated method stub
		this.level=IAlertLevel.getAlertLevelFromInt(l);
		return this;
	}

	@Override
	public IAlertDescriptor setTime(long time) {
		// TODO Auto-generated method stub
		this.time=time;
		return this;
	}

	@Override
	public IAlertDescriptor setMetaInfo(String name, Json value) {
		// TODO Auto-generated method stub
		getMetaInfos().put(name, value);
		return this;
	}

	@Override
	public Json getMetaInfo(String name) {
		// TODO Auto-generated method stub
		Json j= getMetaInfos().get(name);
		if (j!=null) return j;
		return Json.object();
	}

	@Override
	public IAlertDescriptor setProp(String name, String value) {
		// TODO Auto-generated method stub
		getPropsList().put(name,  value);
		return this;
	}

	@Override
	public String getProp(String name) {
		// TODO Auto-generated method stub
		return getPropsList().get(name);
	}

	@Override
	public IAlertDescriptor jsonToProps(Json jprops) {
		// TODO Auto-generated method stub
		return null;
	}




	


}
