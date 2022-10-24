package com.csl.alert;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.ucsl.interfaces.IAlertDescriptor;
import com.ucsl.interfaces.IAlertLevel;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;

/*
 * 
 * jAlertInfo.set("alert_id", alert.getUuid());
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


 * 
 */


public class AlertDescriptor implements IAlertDescriptor{

	
	//Json jAlertInfoFromSysLearner=Json.object();
	//Json jAlertInfoFromIDS=Json.object();


	IAlertLevel level=IAlertLevel.UNDEF;
	String msg="";
	//private String props="";
	long time=0;
	String uuid="";
	Map<String, String> propsList = null;

	//AlertInfoFromSysLearner alertInfoFromSysLearner=null;
	//AlertInfoFromIDS alertInfoFromIDS=null;

	//Json jAlertInfoFromSysLearner=Json.object();
	//Json jAlertInfoFromIDS=Json.object();

	Map<String, Json> metaInfos = new HashMap<String, Json>();



	private boolean acked=false;
	private boolean masked=false;
	long time_for_end_of_mask=0;  ;  // (<=0 if no end )
	private boolean added_to_model=false;

	private int levelForModel=-1;

	// added to model
	// add a device and set the level of confidence
	// add a link and set the level of anomaly

	// to reverse
	//		set the level to MAX_NO_CONFIDENCE
	//		set the anomaly level ti max

	//		public AlertDescriptor(AlertLevel level2, String message, String properties, long time) {
	//		
	//			this.level=level2;
	//			this.msg=message;
	//			this.props=properties;
	//			this.time=time;
	//			uuid = UUID.randomUUID();
	//			parseProps();
	//
	//		}

	
	
	

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
		
		/*if (metaInfos.get(ALERT_INFO_FROM_IDS)!=null) {
			j.set("details_ids",metaInfos.get(ALERT_INFO_FROM_IDS));		
		}

		if (metaInfos.get(ALERT_INFO_FROM_SYSLEARNER)!=null) {
			j.set("details_sys",metaInfos.get(ALERT_INFO_FROM_SYSLEARNER));		
		}*/

		return j;


	}


	//		public AlertDescriptor(String level2, String message) {
	//			
	//			this(level2, message,"", CSLContext.instance.getTimeSystemCurrent());
	//		}
	//		
	//		public AlertDescriptor(String level2, String message, String properties) {
	//		
	//			this(level2, message, properties, CSLContext.instance.getTimeSystemCurrent());
	//		}
	//		

	public AlertDescriptor(String level2, String message, String properties, long time) {

		this.level=IAlertLevel.getAlertLevelFromString(level2); //properties) AlertLevel(level2);
		this.msg=message;
		
		this.time=time;
		uuid = UUID.randomUUID().toString();

		parseProps(properties);
	}


//	public AlertDescriptor(SeverityLevel level2, String message, String properties, long time) {
//
//		this.level=IAlertLevel.getAlertLevelFromInt(level2.getIndex());
//		this.msg=message;
//		
//		this.time=time;
//		uuid = UUID.randomUUID().toString();
//
//		parseProps(properties);
//	}

	//		public AlertDescriptor(AlertInfoFromSysLearner a) {
	//			// TODO Auto-generated constructor stub
	//			this(a.getLevel(),"System Model "+a.getModelName()+":"+a.getAlertMsg() );
	//			
	//			this.alertInfoFromSysLearner=a;
	//		
	//		}

	public AlertDescriptor() {
		this.uuid = UUID.randomUUID().toString();
		this.time=System.currentTimeMillis(); // default
	}


//	public AlertDescriptor(int n, String msg, String props) {
//		// TODO Auto-generated constructor stub
//		this();
//		setLevelFromInt(n);
//		setMsg(msg);
//		
//		parseProps(props);
//
//	}
////
//	public AlertDescriptor(int n, String msg) {
//		// TODO Auto-generated constructor stub
//		this(n,msg,"" );
//
//	}



	//		public AlertDescriptor(AlertInfoFromIDS cat, String message) {
	//			// TODO Auto-generated constructor stub
	//			
	//			this(cat.getSeverity(), cat.getName()+" ["+ message+"]","",CSLContext.instance.getTimeSystemCurrent());
	//			this.alertInfoFromIDS=cat;
	//			
	//		}
	//		
	//		
	//		public AlertDescriptor(AlertInfoFromIDS cat) {
	//			// TODO Auto-generated constructor stub
	//			
	//			this(cat.getSeverity(), cat.getMessage(),"",CSLContext.instance.getTimeSystemCurrent());
	//			this.alertInfoFromIDS=cat;
	//			
	//		}

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


	public long getTime() {
		return time;
	}


	public boolean isAcked() {
		return acked;
	}


	public void setAcked(boolean acked) {
		this.acked = acked;
	}


	public boolean isMasked() {
		return masked;
	}


	public void setMasked(boolean masked) {
		this.masked = masked;
	}


	public boolean isAdded_to_model() {
		return added_to_model;
	}


	public void setAdded_to_model(boolean added_to_model) {
		this.added_to_model = added_to_model;
	}


	public int getLevelForModel() {
		return levelForModel;
	}


	public void setLevelForModel(int levelForModel) {
		this.levelForModel = levelForModel;
	}


	//		public void addToModel(int level) {
	//			if (alertInfoFromIDS!=null) alertInfoFromIDS.addToModel(getUuid(),level);
	//			
	//			setAdded_to_model(true);
	//			setLevelForModel(level);
	//			
	//		}

	//		public void removeFromModel() {
	//			if (alertInfoFromIDS!=null) alertInfoFromIDS.removeFromModel(getUuid());
	//			setAdded_to_model(false);
	//			setLevelForModel(-1);
	//		}

	public IAlertLevel getLevel() {
		return level;
	}
	public void setLevel(IAlertLevel level) {



		this.level = level;
	}
	public String getMsg() {
		return msg;
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
	
//	public Map<String, String> getRawPropsList() {
//		return propsList;
//	}

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
			System.out.println(" Prop:"+entry.getKey() + "=" + entry.getValue());
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


	public String getUuid() {
		return uuid;
	}



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
			System.out.println(entry.getKey() + "/" + entry.getValue());
			String key=entry.getKey();
			Json value=entry.getValue();
			jarray.add(Json.object().set("key",key).set("value", value));

		}

		return jarray;
	}

	
	public Json getAlertInfoFromIDSAsJson() {

		if (metaInfos.get(ALERT_INFO_FROM_IDS)!=null) return Json.object();
		return metaInfos.get(ALERT_INFO_FROM_IDS);
	}



	public Json getAlertInfoFromSysLearnerAsJson() {

		if (metaInfos.get(ALERT_INFO_FROM_SYSLEARNER)!=null) return Json.object();
		return metaInfos.get(ALERT_INFO_FROM_SYSLEARNER);
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
