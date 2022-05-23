package com.csl.alert;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.csl.core.CSLContext;
import com.csl.core.CSLUtil;
import com.csl.devdb.DevicesDB;
import com.csl.logger.CSLLogger;
import com.csl.logger.FileLog;
import com.csl.web.jcmdoversocket.IAlertForwarder;
import com.csl.web.websockets.CSLWebSocket;
import com.xcsl.ids.IDSMainProcessor;
import com.xcsl.ids.IDSTrace;
import com.xcsl.interfaces.IAlertDescriptor;
import com.xcsl.interfaces.IAlertFactory;
import com.xcsl.interfaces.IAlertLevel;
import com.xcsl.interfaces.IAlertManager;
import com.xcsl.json.Json;
import com.xcsl.json.JsonUtil;

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

	//public static CSLAlertManager instance = new CSLAlertManager();

	public IAlertFactory alertFactory=new CSLAlertFactory();

	private IDSMainProcessor idsMainProcessor =null;

	//Json config=null;
	boolean FDEBUG=false;


	// id client, send over udp use sockets
	IAlertForwarder alertForwarder=null;


	//public static String DEBUG="DEBUG";
	public static String INFO="INFO";	
	public static String TOLERABLE="TOLERABLE";
	public static String MODERATE="MODERATE";
	public static String HIGH="HIGH";


	//public static String WARN="WARN";	
	//public static String WARNING="WARN";
	//public static String ERROR="ERROR";
	//public static String FATAL="FATAL";		  	
	//public static String HIGH="HIGH";	  	

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

	private String filename_current_alerts="";

	private Json jConfig=null;

	List<IAlertDescriptor> listOfCurrentAlerts= new ArrayList<>();
	//private List<IDSAlertListener> listeners= new ArrayList<IDSAlertListener>();

	// if >0 , after this duration, the alert is cleared
	private int durationOfAlert=5000;
	private boolean doNotResendSameAlert=false;

	private String subdir_backup_alerts="alerts";



	public CSLAlertManager(IDSMainProcessor x, Json jConfig) { 
		this.idsMainProcessor=x;
		this.idsMainProcessor.setAlertFactory( alertFactory);

		init(jConfig);
	}

	public IAlertFactory getAlertFactory() {
		return alertFactory;
	}
	public CSLAlertManager setIDSMainProcessor(IDSMainProcessor x) {
		this.idsMainProcessor=x;
		this.idsMainProcessor.setAlertFactory( alertFactory);
		return this;
	}

	public CSLAlertManager setname(String loggerName) { 
		if (loggerName.isEmpty()) return this;
		this.loggerName=loggerName;
		return this;
	}

	public void reinit(Json jConfig) {
		init(jConfig);
		clearAlerts();

	}

	public Json getConfig() {
		return jConfig;
	}

	public String getLogDir() {
		return datadir;
	}

	private void init(Json jConfig) {

		this.jConfig=jConfig;

		//if (j==null) return;
		this.port= CSLUtil.getConfigIntegerValue(jConfig,  "port",4445); 
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

		this.loggerName=CSLUtil.getConfigStringValue(jConfig,  "name",  "Alerts") ; //j.get("name")


		this.filename_current_alerts=CSLUtil.getConfigStringValue(jConfig,  "filename_current_alerts",  "current_alerts") ; //j.get("name")
		this.subdir_backup_alerts=CSLUtil.getConfigStringValue(jConfig,  "subdir_backup_alerts",  "alerts") ; //j.get("name")


		this.logToFile=CSLUtil.getConfigBooleanValue(jConfig, "logToFile", false);
		if (logToFile) {
			initFileLog();
		}

		IDSTrace.log(IDSTrace.ALERT, "send alerts to UDP:"+alert_to_udp+" WEB:"+alert_to_web+" LOGFILE:"+logToFile);

		this.durationOfAlert=CSLUtil.getConfigIntegerValue(jConfig,  "alert_duration",5000); 
		this.doNotResendSameAlert=CSLUtil.getConfigBooleanValue(jConfig,  "do_not_resent_same_alert",false); 

	}

	private void initFileLog() {
		if (jConfig!=null) {
			this.datadir=
					CSLContext.instance.buildFullPathInUserDir(CSLUtil.getConfigStringValue(jConfig,"log_dir", "./logs"));
			this.filename=CSLUtil.getConfigStringValue(jConfig,"prefix_filename", "alert");
			this.max_size=CSLUtil.getConfigLongValue(jConfig,"max_size_of_log_files",100000);
			this.fileLog= new FileLog(datadir, filename,max_size, CSLContext.instance::getSystemCurrentTimeMillis);

		}
	}

	//	@Override
	//	public void register(IDSAlertListener listener) {
	//		this.listeners.add(listener);
	//	}

	public List<IAlertDescriptor> getListOfCurrentAlerts() {
		return listOfCurrentAlerts;
	}

	//	private void sendAlert(String level, String message) {
	//		sendAlert(level, message, "",true,false);
	//	}
	//	

	public void sendAlert(IAlertDescriptor alertDescriptor) {
		sendAlert(alertDescriptor,true,false);
	}

	//	public void sendAlert(String level, String message, boolean toViewer, boolean toLog) {
	//		sendAlert(level, message, "", toViewer,toLog);
	//	}


	//	// MAIN FCT to send alert
	//	private void sendAlert(String level, String message, String properties,boolean toViewer, boolean toLog) {
	//		
	//		if (findAlert(level,message,properties)!=null) return;
	//	
	//			
	//		System.err.println("send alert: "+message);
	//		long t=CSLContext.instance.getTimeSystemCurrent();
	//		
	//		//IAlertDescriptor alertDescriptor= new IAlertDescriptor(level, message, properties, CSLContext.instance.getSystemCurrentTimeMillis());
	//		IAlertDescriptor alertDescriptor = 
	//	
	//	}

	public void sendAlert(IAlertDescriptor alertDescriptor,boolean toViewer, boolean toLog) {


		//System.err.println("zaza:"+alertDescriptor.toJson());
		if (findAlert(alertDescriptor)!=null) return;



		System.out.println("ALERT="+alertDescriptor);
		System.out.println("ALERT="+alertDescriptor.toJson());


		listOfCurrentAlerts.add(alertDescriptor);
		send(alertDescriptor,

				//alertDescriptor.getUuid().toString(), alertDescriptor.getTime(), 
				//alertDescriptor.getLevel().toString(), 
				//alertDescriptor.getMsg(),
				//alertDescriptor.getProps(),
				//message, properties, 
				toLog, toViewer);
		//for (IDSAlertListener l:listeners) l.newAlert(alertDescriptor);


	}

	private IAlertDescriptor findAlert(IAlertDescriptor alert) {

		long t=CSLContext.instance.getTimeSystemCurrent();

		for (IAlertDescriptor a:listOfCurrentAlerts) {
			if (a.alertEqualTo(alert)) {
				//System.err.println("Found alert:"+a);
				//System.err.println("delta t="+(t-a.getTime()));
				if (doNotResendSameAlert) return a;

				if (t-a.getTime()<durationOfAlert)	
					return a;  // do not consider old alert
			}
		}
		return null;
	}

	private void clearAlerts() {

		long t=CSLContext.instance.getTimeSystemCurrent();

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


	//	public void sendToViewer(String level, String message) {
	//	
	//		send(level, message,"",false,true);
	//	}
	//
	//	public void sendToLog(String level, String message) {
	//		send(level, message,"",true,false);
	//	}
	//
	//	public void sendToViewer(String level, String message, String properties) {
	//		send(level, message,properties,false,true);
	//		
	//	}
	//	public void sendToLog(String level, String message, String properties) {
	//		send(level, message,properties,true,false);
	//	}


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

	private String Log4jThrowable(String data) {
		return " <log4j:throwable><![CDATA["+data+"]]></log4j:throwable>\n";
	}	

	private String Log4jLocationInfo(String module, String method, String file, String line) {
		return " <log4j:locationInfo class=\""+module+"\"\n" + 
				"        method=\""+method+"\"\n" + 
				"        file=\""+file+"\"\n" + 
				"        line=\""+line+"\"/>\n";
	}

	private String escapeData(String data) {
		return data.replace("]]>", "]]>]]&gt;<![CDATA[");
	}




	private String XMLformatter(String loggerName, String levelName, String message, String ndc, String throwable, Map<String, String>propsMap) {
		long time = CSLContext.instance.getSystemCurrentTimeMillis(); //System.currentTimeMillis();

		HashMap<String, String> event = new HashMap<String, String>();
		event.put("name", loggerName);
		event.put("levelName", levelName);
		event.put("created", String.valueOf(time));
		event.put("message", Log4jMessage(escapeData(message)));
		event.put("ndc",Log4jNdc(escapeData(ndc)));
		event.put("throwable", Log4jNdc(escapeData(throwable)));

		if (propsMap!=null) {
			String s ="";
			String z;
			for (String key : propsMap.keySet()) {
				z=Log4jProperty(key, propsMap.get(key));
				s=s+z;
			}
			String props = Log4jProperties(s);
			event.put("props", props);
		}
		event.put("locationInfo", "");
		return Log4jEvent(event);
	}





	private void send(IAlertDescriptor alert , /*String uuid,long time,String level, String message, String properties,*/ boolean toFile, boolean toViewer) {

		//		if (level.compareTo("CRITICAL")==0) level="FATAL";
		//
		/*boolean ok=false;

		level=level.toUpperCase();

		if (level.compareTo("RED")==0) level=CRITICAL;
		if (level.compareTo("ORANGE")==0) level=HIGH;
		if (level.compareTo("YELLOW")==0) level=MODERATE;
		if (level.compareTo("GREEN")==0) level=TOLERABLE;
		if (level.compareTo("UNDEF")==0) level=INFO;




		if (level.compareTo("CRITICAL")==0) ok=true;
		if (level.compareTo("HIGH")==0) ok=true;
		if (level.compareTo("MODERATE")==0) ok=true;
		if (level.compareTo("TOLERABLE")==0) ok=true;
		if (level.compareTo(INFO)==0) ok=true;

		if (!ok) {
			System.err.println("Invalid Alert level:"+level+" msg:"+message);
			level="INFO";

		}*/
		if (this.alert_to_udp)
			this.sendAlertToViewerUDP(alert.toJson());

		if ((!toFile)&&(!toViewer)) return;

		if (IDSTrace.isDebug(IDSTrace.ALERT_PRINT )) {
			String s="********[";
			if (toFile) s=s+"F"; else s=s+" ";
			if (toViewer) s=s+"V"; else s=s+" ";

			s=s+"|"+alert.getLevelAsString()+"]"+alert.getMsg()+' '+"********";
			System.out.println(s);
			IDSTrace.log(IDSTrace.ALERT, s);
		}


		/*	Map<String, String> props = new HashMap<String, String>();

		if (!alert.getProps().isEmpty()) {
			String [] p=properties.split(";");

			for(String keyvalue : p) {
				String[] x=keyvalue.split("=");
				if (x.length==2) props.put(x[0],x[1]);
			}
		}*/

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

		String msg = XMLformatter(loggerName, alert.getLevelAsString(), alert.getMsg(),"","", alert.getPropsList());

		if (toViewer) {


			if (this.alert_to_web)
				this.sendAlertToViewerWeb(alert) ; //uuid,time,level,message,props);
		}



	}

	
	public void registerAlertForwarder(IAlertForwarder af) {
		this.alertForwarder=af;
	}
	
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
			byte[]data = msg.getBytes();
			DatagramSocket s;

			//System.err.println("SEND MSG:"+msg);

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
		}

	}

	private void sendAlertToViewerWeb(IAlertDescriptor alert
			/*String uuid,long time,String level, String message, Map<String, String> props */) {


		Json jAlert=Json.object();
		jAlert.set("type", "newAlert");
		Json jAlertInfo=alertToJsonForHmi(alert); //Json.object();
		jAlert.set("alertInfo",jAlertInfo);



		// private String alert_json_tag="alert";

		IDSTrace.log(IDSTrace.ALERT,"Send alert tag="+alert_json_tag+" json="+jAlert.toString());
		//CSLWebSocketForAlert.broadcastMessageJson(alert_json_tag, jAlert);

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


	public Json saveListOfCurrentAlerts( ) {
		Json jlist=Json.array();
		for (IAlertDescriptor a:listOfCurrentAlerts) {
			jlist.add(a.toJson());
		}


		//System.out.println(jlist);

		CSLContext.instance.getDatabaseServer().saveJsonAsDataFile(this.filename_current_alerts,
				Json.object().set("contents",jlist),true);


		return jlist;
	}


	public Json loadListOfCurrentAlerts( ) {
		Json jlist=Json.array();
		for (IAlertDescriptor a:listOfCurrentAlerts) {
			jlist.add(a.toJson());
		}


		System.out.println(jlist);



		Json j=CSLContext.instance.getDatabaseServer().loadDataFileAsJson(this.filename_current_alerts);

		System.err.println("j - recuperer contents");
		return jlist;
	}


	public Json resetListOfCurrentAlerts( /*IDSParams idsParams*/) {



		/*		Json jlist=Json.array();
		for (IAlertDescriptor a:listOfCurrentAlerts) {
			jlist.add(a.toJson());
		}


		System.out.println(jlist);

		//String dirname= idsParams.getIdsModelDirBackup()+File.separator+
		String dir=
				idsParams.getIdsModelDirBackup()+File.separator+subdir_backup_alerts;
		String filename=
						this.filename_current_alerts;

		filename=FileUtils.setTimeStampToFilePath(filename);

		FileUtils.saveJsonToFile( dir, filename,jlist); */

		listOfCurrentAlerts.clear();
		saveListOfCurrentAlerts();

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

			resetListOfCurrentAlerts(); //CSLContext.instance.getIdsParams());

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
				if (a.isAdded_to_model()) idsMainProcessor.removeAlertFromModel(a); //a.removeFromModel();
			}

			return alertToJsonForHmi(a);
		}
		//		else if (op.compareToIgnoreCase("remove_from_model")==0) {
		//			
		//			IAlertDescriptor a=getAlert(alert_id);
		//			if (a!=null) a.removeFromModel();
		//			
		//		}
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
		/*else if (op.compareToIgnoreCase("config1")==0) {
			DevicesDB.instance.config1();
		}*/
		else if (op.compareToIgnoreCase("clear_devices")==0) {
			clearDevices();
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




	IAlertDescriptor getAlert(Json params) {

		String alert_id=JsonUtil.getStringFromJson(params,  "alert_id","");
		for (IAlertDescriptor a:listOfCurrentAlerts) {
			if (a.getUuid().compareTo(alert_id)==0) {
				return a;
			}
		}

		return null;
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

		//a.setLevel(new AlertLevel(2));
		sendAlert(a);
		list.add(a.toJson());


		a= alertFactory.createAlertDescriptor(3, "ALERT level 3", System.currentTimeMillis());
		a.setMsg("This is a test orange");
		a.setProp("t",""+System.currentTimeMillis());

		//a.setLevel(new AlertLevel(3));
		sendAlert(a);
		list.add(a.toJson());


		a= alertFactory.createAlertDescriptor(4, "ALERT level 4", System.currentTimeMillis());
		a.setMsg("This is a test red");
		a.setProp("t",""+System.currentTimeMillis());

		//a.setLevel(new AlertLevel(4));
		sendAlert(a);
		list.add(a.toJson());



		return list;

	}


	private void test2() {

		IAlertDescriptor a= alertFactory.createAlertDescriptor(5, "ALERT", System.currentTimeMillis()); 

		a.setProp("p1", "34");
		sendAlert(a);




		saveListOfCurrentAlerts();
	}


	private void clearDevices() {
		DevicesDB.instance.clear();

	}
}
