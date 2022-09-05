package com.xcsl.interfaces;

import java.util.Map;

import com.xcsl.json.Json;

public interface IAlertDescriptor {

	// code alertes
		public static int INFO_CSL=1000;		// 1000-1999 internal

		public static int CSL_RULE_ALERT=2000;
		public static int SURICATA_RULE_ALERT=2001;


		public static String ALERT_INFO_FROM_SYSLEARNER="AlertInfoFromSysLearner";
		public static String ALERT_INFO_FROM_IDS="AlertInfoFromIDS";

	
/*	
	jAlertInfo.set("alert_id", alert.getUuid());
	jAlertInfo.set("timeStamp", alert.getTime());
	jAlertInfo.set("timeStampEndMask", alert.getTimeForEndOfMask());
	
	jAlertInfo.set("level", alert.getLevelAsString());
	jAlertInfo.set("ilevel",level.getIndex());
	
	jAlertInfo.set("message", alert.getMsg());
	jAlertInfo.set("masked", alert.isMasked());
	jAlertInfo.set("added_to_model", alert.isAdded_to_model());
	jAlertInfo.set("model_level", alert.getLevelForModel());
	
		
	jAlertInfo.set("moreInfoIT", alert.getAlertInfoFromIDSAsJson());
	jAlertInfo.set("moreInfoOT", alert.getAlertInfoFromSysLearnerAsJson());
	
	*/
	
	
	
	
	
	public String getUuid();
	public IAlertDescriptor setUuid(String uuid);
	
	
	public String getMsg();
	public IAlertDescriptor setMsg(String msg);

	
	public String getLevelAsString();
	public IAlertDescriptor setLevelFromString(String s);
	
	public int getLevelAsInt();
	public IAlertDescriptor setLevelFromInt(int l);
	
	
	public long getTime();
	public IAlertDescriptor setTime(long time);
	
	
	public long getTimeForEndOfMask();
	public IAlertDescriptor setTimeForEndOfMask(long time);

	public IAlertDescriptor setMetaInfo(String name,Json value);
	public Json getMetaInfo(String name);

	public IAlertDescriptor setProp(String name,String value);
	public String getProp(String name);
	public Json propsToJson();
	public IAlertDescriptor jsonToProps(Json jprops);
	
	public boolean isAcked() ;
	public void setAcked(boolean acked);

	public boolean isMasked();
	public void setMasked(boolean masked);
	
	public boolean isAdded_to_model(); 
	public void setAdded_to_model(boolean added_to_model) ;
	
	
	public int getLevelForModel();
	public void setLevelForModel(int levelForModel) ;
	
	public boolean alertEqualTo(IAlertDescriptor a);
	
	
	
	public boolean hasProps();
	public String getPropsAsString();
	public IAlertDescriptor setPropsFromString(String props);
	public Map<String, String>  getPropsList();
	
	
	
	public Json toJson();
	
	
	
	
	
}
