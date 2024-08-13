package com.csl.alert;

import com.csl.core.CSLContext;
import com.csl.core.CSLUtil;
import com.csl.core.Config;
import com.csl.logger.CSLLogger;
import com.csl.logger.FileLog;
import com.csl.web.jcmdoversocket.IAlertForwarder;
import com.csl.web.websockets.CSLWebSocket;
import com.ucsl.interfaces.*;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
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
public class CSLAlertManager implements IAlertManager {
	
	public boolean NO_ALERT_FILTERING=true;
	public IAlertFactory alertFactory=new CSLAlertFactory();
	private IIDSMainProcessor idsMainProcessor =null;
	boolean FDEBUG=false;

	// id client, send over udp use sockets
	IAlertForwarder alertForwarder=null;
	public static String INFO="INFO";	
	public static String TOLERABLE="TOLERABLE";
	public static String MODERATE="MODERATE";
	public static String HIGH="HIGH";
	public static String CRITICAL="CRITICAL";  // RED
	private String loggerName;
	private int port;
	private boolean logToFile;
	private String datadir;
	private String filename;
	private long max_size;
	private FileLog fileLog=null;;
	private InetAddress iNetAddress=null;
	private boolean alert_to_web=true;
	private String alert_json_tag="alert";
	private boolean alert_to_udp=true;
	private boolean showAlert;
	/**
	 * If the alert must be stocked into the DB
	 */
	private boolean alertToDb = true;
	private String filename_current_alerts="";
	private Json jConfig=null;
	private Config.AlertViewer config=null;
	List<IAlertDescriptor> listOfCurrentAlerts= new ArrayList<>();
	// if >0 , after this duration, the alert is cleared
	private int durationOfAlert=5000;
	private boolean doNotResendSameAlert=false;
	private String subdir_backup_alerts="alerts";
	private DbapiHandlerForAlerts dbapiHandler;



	public CSLAlertManager(IIDSMainProcessor x, Json jConfig) {
		this.idsMainProcessor=x;
		this.idsMainProcessor.setAlertFactory( alertFactory);
		dbapiHandler = new DbapiHandlerForAlerts();
		init(jConfig);
	}
	public CSLAlertManager(IIDSMainProcessor x, Config.AlertViewer config) {
		this.idsMainProcessor=x;
		this.idsMainProcessor.setAlertFactory( alertFactory);
		dbapiHandler = new DbapiHandlerForAlerts();
		init(config);
	}

	public CSLAlertManager setname(String loggerName) { 
		if (loggerName.isEmpty()) return this;
		this.loggerName=loggerName;
		return this;
	}

	public Json getConfig() {
		return jConfig;
	}

	private void init(Json jConfig) {

		this.jConfig=jConfig;

		this.port= CSLUtil.getConfigIntegerValue(jConfig,  "port",8001);
		String ip=CSLUtil.getConfigStringValue(jConfig,  "ip","127.0.0.1" ); //. j.get("ip").asString();

		try {
			InetAddress iNetAddress=InetAddress.getByName(ip);
			this.iNetAddress=iNetAddress;
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		this.alert_to_web= CSLUtil.getConfigBooleanValue(jConfig,  "alert_to_web",false);
		this.alert_json_tag= CSLUtil.getConfigStringValue(jConfig, "alert_json_tag","alert");
		this.alert_to_udp= CSLUtil.getConfigBooleanValue(jConfig,  "alert_to_udp",true);
		this.alertToDb= CSLUtil.getConfigBooleanValue(jConfig,  "alertToDb",true);
		this.showAlert= CSLUtil.getConfigBooleanValue(jConfig,  "showAlerts",true);

		this.loggerName=CSLUtil.getConfigStringValue(jConfig,  "name",  "Alerts") ; //j.get("name")


		this.filename_current_alerts=CSLUtil.getConfigStringValue(jConfig,  "filename_current_alerts",  "current_alerts") ; //j.get("name")
		this.subdir_backup_alerts=CSLUtil.getConfigStringValue(jConfig,  "subdir_backup_alerts",  "alerts") ; //j.get("name")


		this.logToFile=CSLUtil.getConfigBooleanValue(jConfig, "logToFile", false);
		if (logToFile) {
			initFileLog();
		}

		this.durationOfAlert=CSLUtil.getConfigIntegerValue(jConfig,  "alert_duration",5000);
		this.doNotResendSameAlert=CSLUtil.getConfigBooleanValue(jConfig,  "do_not_resent_same_alert",false);

	}

	private void init(Config.AlertViewer config) {

//		this.jConfig=jConfig;
		this.config = config;

//		this.port= CSLUtil.getConfigIntegerValue(jConfig,  "port",8001);
		this.port= config.getPort();
//		String ip=CSLUtil.getConfigStringValue(jConfig,  "ip","127.0.0.1" ); //. j.get("ip").asString();
		String ip=config.getIp();

		try {
			InetAddress iNetAddress=InetAddress.getByName(ip);
			this.iNetAddress=iNetAddress;
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

//		this.alert_to_web= CSLUtil.getConfigBooleanValue(jConfig,  "alert_to_web",false);
		this.alert_to_web= config.getAlertToWeb();
//		this.alert_json_tag= CSLUtil.getConfigStringValue(jConfig, "alert_json_tag","alert");
		this.alert_json_tag= config.getAlertJsonTag();
//		this.alert_to_udp= CSLUtil.getConfigBooleanValue(jConfig,  "alert_to_udp",true);
		this.alert_to_udp= config.getAlertToUdp();
//		this.alertToDb= CSLUtil.getConfigBooleanValue(jConfig,  "alertToDb",true);
		this.alertToDb= config.getAlertToDb();
//		this.showAlert= CSLUtil.getConfigBooleanValue(jConfig,  "showAlerts",true);
		this.showAlert= config.getShowAlerts();

//		this.loggerName=CSLUtil.getConfigStringValue(jConfig,  "name",  "Alerts") ; //j.get("name")
		this.loggerName= config.getName();


//		this.filename_current_alerts=CSLUtil.getConfigStringValue(jConfig,  "filename_current_alerts",  "current_alerts") ; //j.get("name")
		this.filename_current_alerts=config.getFilenameCurrentAlerts(); //j.get("name")
//		this.subdir_backup_alerts=CSLUtil.getConfigStringValue(jConfig,  "subdir_backup_alerts",  "alerts") ; //j.get("name")
		this.subdir_backup_alerts=config.getSubdirBackupAlerts();


//		this.logToFile=CSLUtil.getConfigBooleanValue(jConfig, "logToFile", false);
		this.logToFile=config.getLogToFile();
		if (logToFile) {
			initFileLog();
		}

//		this.durationOfAlert=CSLUtil.getConfigIntegerValue(jConfig,  "alert_duration",5000);
		this.durationOfAlert=config.getAlertDuration();
//		this.doNotResendSameAlert=CSLUtil.getConfigBooleanValue(jConfig,  "do_not_resent_same_alert",false);
		this.doNotResendSameAlert= config.getDoNotResentSameAlert();

	}

	private void initFileLog() {
		if (config!=null) {
//			this.datadir=
//					CSLContext.instance.buildFullPathInUserDir(CSLUtil.getConfigStringValue(jConfig,"log_dir", "./logs"));
			this.datadir=					CSLContext.instance.buildFullPathInUserDir(config.getLogDir());
//			this.filename=CSLUtil.getConfigStringValue(jConfig,"prefix_filename", "alert");
			this.filename=config.getPrefixFilename();
//			this.max_size=CSLUtil.getConfigLongValue(jConfig,"max_size_of_log_files",100000);
			this.max_size= config.getMaxSizeOfLogFiles();
			this.fileLog= new FileLog(datadir, filename,max_size, CSLContext.instance::getSystemCurrentTimeMillis);

		}
	}

	public List<IAlertDescriptor> getListOfCurrentAlerts() {
		return listOfCurrentAlerts;
	}

	public void sendAlert(IAlertDescriptor alertDescriptor) {
		sendAlert(alertDescriptor,true,false);
	}

	/**
	 * Checks if the alert is ok and calls the send function to forward it
	 * @param alertDescriptor alert in {@link IAlertDescriptor} format
	 * @param toViewer if the alert will be sent to the viewer
	 * @param toLog if the alert will be logged
	 */
	public void sendAlert(IAlertDescriptor alertDescriptor, boolean toViewer, boolean toLog) {
		if (findAlert(alertDescriptor)!=null) return;

		if (showAlert)
			System.out.println("ALERT="+alertDescriptor);

		listOfCurrentAlerts.add(alertDescriptor);
		send(alertDescriptor, toViewer, toLog);
	}

	/**
	 * Verify that the alert is in the list of current Alerts
	 * @param alert alert to verify
	 * @return the alert if found and valid
	 */
	private IAlertDescriptor findAlert(IAlertDescriptor alert) {

		
		if (NO_ALERT_FILTERING ) return null;
		
		long t=CSLContext.instance.getSystemCurrentTimeMillis();

		for (IAlertDescriptor a:listOfCurrentAlerts) {
			if (a.alertEqualTo(alert)) {
				if (doNotResendSameAlert) return a;

				if (t-a.getTime()<durationOfAlert)	
					return a;  // do not consider old alert
			}
		}
		return null;
	}

	private void clearAlerts() {

		long t=CSLContext.instance.getSystemCurrentTimeMillis();

		List<IAlertDescriptor> toRemove = new ArrayList<IAlertDescriptor>();

		for (IAlertDescriptor a:listOfCurrentAlerts) {
			boolean clear=true;

			if (durationOfAlert>0) {
				if (a.getTime()+durationOfAlert<t) {
					toRemove.add(a);
				}

			}

		}
		listOfCurrentAlerts.removeAll(toRemove);

	}


	private String Log4jEvent(HashMap<String,String> event) {

		return "\n" + 
				"<log4j:event logger=\""+event.get("name")+"\"\n" + 
				"    timestamp=\""+event.get("created")+"\"\n" + 
				"    level=\""+event.get("levelName")+"\"\n" + 
				"    >\n" + 
				event.get("message")+event.get("ndc")+event.get("throwable")+event.get("locationInfo")+event.get("props")+"</log4j:event>\n";
	}

	private String Log4jMessage(String data) {
		return "<log4j:message><![CDATA["+data+"]]></log4j:message>\n";
	}

	private String Log4jProperties(String properties) {
		return "<log4j:properties>"+properties+"</log4j:properties>\n";
	}

	private String Log4jProperty(String key, String property) {
		return "<log4j:data name=\""+key+"\" value=\""+property+"\"></log4j:data>\n";
	}

	private String Log4jNdc(String data) {
		return " <log4j:ndc><![CDATA["+data+"]]></log4j:ndc>\n";
	}

	private String escapeData(String data) {
		return data.replace("]]>", "]]>]]&gt;<![CDATA[");
	}

	/**
	 * Send the alert to a UDP viewer, to logs and to Web viewer if respective flags are true
	 * @param alert alert ot forward
	 * @param toViewer if alert must be forwarded to viewer
	 * @param toFile if alert must be logged
	 */
	private void send(IAlertDescriptor alert , boolean toViewer, boolean toFile) {


		// Send to DB
		if (this.alertToDb) {
			this.dbapiHandler.insertAlert(alert);
		}

		if ((!toFile)&&(!toViewer)) return;

		// Save to file
		if (toFile|logToFile) {

			if (fileLog==null) initFileLog();
			if (fileLog==null) {
				CSLLogger.instance.error("Cannot log CSLAlert to file ");
				return;
			}
			if (alert.hasProps()) 
				fileLog.send("["+alert.getLevelAsString()+"] "+alert.getMsg()+"  "+alert.getPropsList());
			else
				fileLog.send("["+alert.getLevelAsString()+"] "+alert.getMsg());
		}

		// Send to viewer
		if (toViewer) {
			if (this.alert_to_web) {
				this.sendAlertToViewerWeb(alert);
			}
		}
	}

	public void registerAlertForwarder(IAlertForwarder af) {
		this.alertForwarder=af;
	}

	/**
	 * Send the alert either to csl-server either to the web client
	 * @param jalert alert in format {@link Json}
	 */
	public void sendAlertToViewerUDP(Json jalert) {

		// in client
		if (alertForwarder !=null) {
			alertForwarder.sendAlert(jalert);
		}
		else {   // in server


			if (iNetAddress==null) {
				CSLLogger.instance.error("Invalid IP for alert viewer");
			}
			jalert.set("type","alert");

			String msg=jalert.toString();
			// region -- forward alerts to the Alert Listener
			// TODO: Send the alert directly to DB-API instead of using UDP Socket (This requires the implementation of authentication)
			byte[]data = msg.getBytes();
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

	/**
	 * Send the alert either to web socket
	 * @param alert alert in format {@link IAlertDescriptor}
	 */
	private void sendAlertToViewerWeb(IAlertDescriptor alert) {


		Json jAlert=Json.object();
		jAlert.set("type", "newAlert");
		Json jAlertInfo=alertToJsonForHmi(alert); //Json.object();
		jAlert.set("alertInfo",jAlertInfo);


		CSLWebSocket.broadcastMessageJson(CSLWebSocket.WEB_SOCKET_ALERT, jAlert);

		if (FDEBUG) {
			System.out.println("SENDING TO WEB SOCKET:"+jAlert);
		}
	}

	public Json getListOfCurrentAlertsAsJson() {
		Json jarray=Json.array();

		for (IAlertDescriptor alert:getListOfCurrentAlerts()) {

			if ((!alert.isMasked())&&(!alert.isAdded_to_model()))
				jarray.add(alertToJsonForHmi(alert));


		}


		return jarray;
	}

	public Json getNumberOfCurrentAlertsAsJsonByLevel() {
		Json jarray=Json.array();

		int[] count= new int[5];

		for (IAlertDescriptor alert:getListOfCurrentAlerts()) {

			if ((!alert.isMasked())&&(!alert.isAdded_to_model())) {

				int l=alert.getLevelAsInt();
				if (l<0) l=0; if (l>4) l=4;
				count[l]++;
			}



		}
		for (int i=0; i<5; i++) {
			Json j= Json.object();
			j.set("count", count[i]);
			j.set("name", IAlertLevel.getAlertLevelFromInt(i));
			jarray.add(j);
		}

		return jarray;
	}

	public Json getListOfInactiveAlertsAsJson() {
		Json jarray=Json.array();

		for (IAlertDescriptor alert:getListOfCurrentAlerts()) {

			if ((alert.isMasked())||(alert.isAdded_to_model()))
				jarray.add(alertToJsonForHmi(alert));


		}


		return jarray;
	}

	public Json getListOfMaskedAlertsAsJson() {
		Json jarray=Json.array();

		for (IAlertDescriptor alert:getListOfCurrentAlerts()) {

			if ((alert.isMasked()))
				jarray.add(alertToJsonForHmi(alert));


		}


		return jarray;
	}

	public Json getListOfAckedAlertsAsJson() {
		Json jarray=Json.array();

		for (IAlertDescriptor alert:getListOfCurrentAlerts()) {

			if ((alert.isAcked()))
				jarray.add(alertToJsonForHmi(alert));


		}


		return jarray;
	}

	public Json getListOfAddedToModelAlertsAsJson() {
		Json jarray=Json.array();

		for (IAlertDescriptor alert:getListOfCurrentAlerts()) {
			if (alert.isAdded_to_model())
				jarray.add(alertToJsonForHmi(alert));


		}


		return jarray;
	}

	public Json getListOfAllAlertsAsJson() {
		Json jarray=Json.array();

		for (IAlertDescriptor alert:getListOfCurrentAlerts()) {
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
		jAlertInfo.set("ilevel",alert.getLevelAsInt());

		jAlertInfo.set("message", alert.getMsg());
		jAlertInfo.set("masked", alert.isMasked());
		jAlertInfo.set("added_to_model", alert.isAdded_to_model());
		jAlertInfo.set("model_level", alert.getLevelForModel());

		jAlertInfo.set("moreInfoIT", alert.getMetaInfo(IAlertDescriptor.ALERT_INFO_FROM_IDS));
		jAlertInfo.set("moreInfoOT", alert.getMetaInfo(IAlertDescriptor.ALERT_INFO_FROM_SYSLEARNER));

		if (alert.hasProps()) {
			for (String key : alert.getPropsList().keySet()) {
				jAlertInfo.set(key,  alert.getPropsList().get(key));
			}
		}

		Map<String, String> props = alert.getPropsList();

		if (props!=null) {
			String s ="";
			String z;
			for (String key : props.keySet()) {
				jAlertInfo.set(key,  props.get(key));
			}
		}

		return jAlertInfo;
	}

	public Json resetListOfCurrentAlerts( /*IDSParams idsParams*/) {
		listOfCurrentAlerts.clear();
		//saveListOfCurrentAlerts();

		return Json.array();
	}

	public IAlertDescriptor getAlert(String id) {

		for (IAlertDescriptor a:listOfCurrentAlerts) {
			if (a.getUuid().compareTo(id)==0) return a;
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

		Json result=Json.object();
		String op=JsonUtil.getStringFromJson(params,  "op","");
		String alert_id=JsonUtil.getStringFromJson(params,  "alert_id","");

		if (op.compareToIgnoreCase("get_list_active")==0) {  // msaked=false, added_to_mode= false 

			return getListOfCurrentAlertsAsJson();

		}
		else if (op.compareToIgnoreCase("get_number_active_by_level")==0) {  // msaked=false, added_to_mode= false 

			return getNumberOfCurrentAlertsAsJsonByLevel();

		}
		else if (op.compareToIgnoreCase("get_list_acked")==0) {  // masked=true or, added_to_mode= true  

			return getListOfAckedAlertsAsJson();

		}
		else if (op.compareToIgnoreCase("get_list_masked")==0) {  // masked=true or, added_to_mode= true  

			return getListOfMaskedAlertsAsJson();

		}		
		else if (op.compareToIgnoreCase("get_list_added_to_model")==0) {  // masked=true or, added_to_mode= true  

			return getListOfAddedToModelAlertsAsJson();

		}
		else if (op.compareToIgnoreCase("get_list_inactive")==0) {  // masked=true or, added_to_mode= true  

			return getListOfInactiveAlertsAsJson();

		}	
		else if (op.compareToIgnoreCase("get_list_all")==0) {

			return getListOfAllAlertsAsJson();

		}
		else if (op.compareToIgnoreCase("reset_list")==0) {

			resetListOfCurrentAlerts();

		}
		else if (op.compareToIgnoreCase("dump_list")==0) {

			Json list = getListOfAllAlertsAsJson();
			System.out.println(JsonUtil.prettyPrint(list));
			return list;

		}
		else if (op.compareToIgnoreCase("add_to_model")==0) {


			IAlertDescriptor a=getAlert(alert_id);
			if (a==null) return Json.object().set("error", "alert not found ("+alert_id+")");

			boolean b= JsonUtil.getBooleanFromJson(params, "value",false);

			if (b) {

				int level=JsonUtil.getIntFromJson(params, "level",4); // from 0 to 4
				if (!a.isAdded_to_model()) idsMainProcessor.addAlertToModel(a, level); //.addToModel(level);
			}
			else {
				if (a.isAdded_to_model()) idsMainProcessor.removeAlertFromModel(a,0); //a.removeFromModel();
			}

			return alertToJsonForHmi(a);
		}
		else if (op.compareToIgnoreCase("set_acked")==0) {

			boolean b= JsonUtil.getBooleanFromJson(params, "value",false);
			IAlertDescriptor a=getAlert(alert_id);
			if (a==null) return Json.object().set("error", "alert not found ("+alert_id+")");

			if (a!=null) a.setAcked(b);
			return alertToJsonForHmi(a);

		}
		else if (op.compareToIgnoreCase("set_masked")==0) {
			boolean b= JsonUtil.getBooleanFromJson(params, "value",false);

			long time_end= JsonUtil.getLongFromJson(params, "time_for_end_of_mask",0);
			IAlertDescriptor alert=getAlert(alert_id);
			IAlertDescriptor a=getAlert(alert_id);
			if (a!=null) {
				a.setMasked(b);
				a.setTimeForEndOfMask(time_end);
				return alertToJsonForHmi(a);
			}
			else {
				return Json.object().set("error", "alert not found ("+alert_id+")");
			}
		}
		else if (op.compareToIgnoreCase("test")==0) {
			result= test1();
		}
		else if (op.compareToIgnoreCase("test1")==0) {
			result= test1();
		}
		else if (op.compareToIgnoreCase("test2")==0) {
			test2();
		}
		else if (op.compareToIgnoreCase("debug_alert")==0) {
			boolean b= JsonUtil.getBooleanFromJson(params, "value",false);

			FDEBUG=b;
			System.out.println("DEBUG ALERT:"+FDEBUG);
		}
		else {
			System.out.println("op_alert not found:"+params);
		}
		return result;


	}

	public Json getAlertStats() {

		int[]  ctr= new int[5];
		int n=0;
		for (IAlertDescriptor a:listOfCurrentAlerts) {
			int idx= a.getLevelAsInt();
			if (idx<=0) ctr[0]++;
			else if (idx>=4) ctr[4]++;
			else ctr[idx]++;
			n++;
		}
		Json j= Json.object();
		j.set("all",n);

		for (int i=0;i<=4; i++) {
			j.set("l"+i, ctr[i]);
		}



		return j;
	}

	private Json test1() {
		Json list=Json.array();

		IAlertDescriptor a= alertFactory.createAlertDescriptor(5, "ALERT", System.currentTimeMillis()); // AlertDescriptor(5, "ALERT");
		a.setProp("p1", "34");
		sendAlert(a);
		list.add(a.toJson());

		a= alertFactory.createAlertDescriptor(1, "ALERT level 1", System.currentTimeMillis());
		a.setMsg("This is a test green ");
		a.setProp("p1", "34");
		a.setProp("t",""+System.currentTimeMillis());
		//a.setLevel(new AlertLevel(1));
		sendAlert(a);
		list.add(a.toJson());

		a= alertFactory.createAlertDescriptor(2, "ALERT level 2", System.currentTimeMillis());
		a.setMsg("This is a test yellow ");
		a.setProp("t",""+System.currentTimeMillis());

		sendAlert(a);
		list.add(a.toJson());

		a= alertFactory.createAlertDescriptor(3, "ALERT level 3", System.currentTimeMillis());
		a.setMsg("This is a test orange");
		a.setProp("t",""+System.currentTimeMillis());

		sendAlert(a);
		list.add(a.toJson());

		a= alertFactory.createAlertDescriptor(4, "ALERT level 4", System.currentTimeMillis());
		a.setMsg("This is a test red");
		a.setProp("t",""+System.currentTimeMillis());

		sendAlert(a);
		list.add(a.toJson());

		return list;
	}

	private void test2() {
		IAlertDescriptor a= alertFactory.createAlertDescriptor(5, "ALERT", System.currentTimeMillis()); 

		a.setProp("p1", "34");
		sendAlert(a);

		//saveListOfCurrentAlerts();
	}
}
