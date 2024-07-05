package com.csl.intercom.broker;

import java.util.HashMap;
import java.util.Map;

import com.csl.intercom.jsoncmd.XApiCommands;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.json.Json;


/*
 * /usr/local/sbin/mosquitto -c /usr/local/etc/mosquitto/mosquitto.conf
 * 
 */

/*
 * String receiverId = UUID.randomUUID().toString();

IMqttClient receiver = new MqttClient(
                "tcp://" + properties.getProperty("host") + ":" + properties.getProperty("port"), receiverId, new MqttDefaultFilePersistence("/tmp"));
 */
public class CSLInterModuleCommunicationManager {

	//public static CSLInterModuleCommunicationManager instance = new CSLInterModuleCommunicationManager("IDS");
	
	int idebug=1;
	
	

		
		

	
	boolean useBroker=false; //true; //  use mosquitto broker, if flase (for tests mainly, only local commands)
	
	boolean mustDeclareApi=false;
	String moduleName="XXX";
	
	private String mosquittoDir="/usr/local/sbin/";
	private String mosquittoCmd="./mosquitto";
	private String BROKER_TCP_LOCALHOST_1883 = "tcp://localhost:1883";

	
	Map<String,IApiCommands> listOfRegisteredAPI= new HashMap<String, IApiCommands>();
	Map<String,XApiCommands> listOfRegisteredExternalAPI= new HashMap<String, XApiCommands>();
	Map<String,ApiMessageReceiver> listOfReceivers= new HashMap<>();
	Map<String,ApiMessageSender> listOfSenders= new HashMap<>();
	
	SocketMessageMQTTHandler socketMessageMQTTHandler =null;
	
	public CSLInterModuleCommunicationManager(String moduleName, MosquittoConfig config) {
		this.moduleName=moduleName;
		setConfig(config);
		
		socketMessageMQTTHandler=new SocketMessageMQTTHandler(moduleName,BROKER_TCP_LOCALHOST_1883, useBroker, getDebugLevel());
	}
	
//	public CSLInterModuleCommunicationManager(String moduleName) {
//		this.moduleName=moduleName;
//		setConfig(new MosquittoConfig());  // default
//		
//		socketMessageMQTTHandler=new SocketMessageMQTTHandler(moduleName,BROKER_TCP_LOCALHOST_1883, useBroker, getDebugLevel());
//
//		
//	}
	
	public boolean isDebug() {return idebug>1;}
	public boolean isShowInfo() {return idebug>0;}
	public void setDebugLevel(int d) {idebug=d;}
	public int getDebugLevel() { return idebug; }
	
	
	public void setConfig(MosquittoConfig config) {
		
		mosquittoCmd=config.getMosquittoCmd();
		mosquittoDir=config.getMosquittoDir();
		
		BROKER_TCP_LOCALHOST_1883=config.getBrokerURL();
		
		useBroker=config.isUseBroker();
		
	}
	
	
	public boolean isUseBroker() {
		// TODO Auto-generated method stub
		return useBroker;
	}

	public void registerAPI(IApiCommands api) {
		
		if (isShowInfo()) System.out.println("REGISTER API FOR BROKER :"+api.getName());
		listOfRegisteredAPI.put(api.getName(), api);
	}
	
	public void registerExternalAPI(XApiCommands api) {
		
		
		if (isShowInfo()) System.out.println("REGISTER X API FOR BROKER :"+api.getCleanApiName());
		listOfRegisteredExternalAPI.put(api.getCleanApiName(), api);
	}
	
	
	public void registerWebSocketHandler(IApiCommands api) {
	
		if (isShowInfo()) System.out.println("REGISTER SOCKET FOR BROKER :"+api.getName());
		listOfRegisteredAPI.put(api.getName(), api);
	}
	
	// start the MQTT listener
	public void start() {
		
		
		if (isUseBroker()) {
		for (Map.Entry<String,IApiCommands> entry : listOfRegisteredAPI.entrySet()) {
				
			IApiCommands api=entry.getValue();
			
			ApiMessageReceiver receiver = new ApiMessageReceiver(moduleName,api.getName(),api,
					BROKER_TCP_LOCALHOST_1883,
					getDebugLevel());
			listOfReceivers.put(entry.getKey(),receiver);
			
			
			
		}
		
		}
		
		
		
	}
	
	public void stop() {
		
		
		if (isUseBroker()) {
		
		for (Map.Entry<String,IApiCommands> entry : listOfRegisteredAPI.entrySet()) {
				
			ApiMessageReceiver receiver=listOfReceivers.get(entry.getKey());
			if (receiver!=null) receiver.close();
			
				
		
			
			ApiMessageSender sender=listOfSenders.get(entry.getKey());
			if (sender!=null) sender.close();
			
		}
		}
		
		if (socketMessageMQTTHandler!=null) socketMessageMQTTHandler.close();
		
		
		
	}
	
	public void resstart() {
		
		
		
		stop();
		start();
	}
	
	
	
	
	// create the sender if not found and use it
	public Json executeExternalCommand(String apiName, Json jCmd) {
		
	
		if (!useBroker) System.err.println("Warning : external communication not started");
		
		ApiMessageSender sender =listOfSenders.get(apiName);
		if (sender==null) {
			sender = new ApiMessageSender(moduleName,apiName, getDebugLevel());
			listOfSenders.put(apiName,sender);
		}
		return sender.execCmd(jCmd);
		
		
	}
	
	public Json executeLocalCommand(String apiName, Json jCmd) {
		
		
		IApiCommands api = listOfRegisteredAPI.get(apiName);
		
		if (api==null) return Json.object().set("error", "Invalid API:"+apiName);
		
		Json r=api.execJcmd(jCmd);
		
		return r;
		
	}
	
	
	public Json executeCommand(String apiName, Json jCmd) {
		
		
		
		// local command
		IApiCommands api = listOfRegisteredAPI.get(apiName);
		
		if (api!=null) {
			Json r=api.execJcmd(jCmd);
			return r;
		}
		
		
		// try as external command
		
		ApiMessageSender sender =listOfSenders.get(apiName);
		if (sender==null) {
			if (!mustDeclareApi) {
				sender = new ApiMessageSender(moduleName,apiName, getDebugLevel());
				listOfSenders.put(apiName,sender);
			}
		}
		
		if (sender!=null) {
			return sender.execCmd(jCmd);
		}
		
		return Json.object().set("error", "Invalid API:"+apiName);
		
	}
	
	// create the sender if not found and use it
	public void sendSocketMsg(String socketname,String msg) {
			
		
		
			socketMessageMQTTHandler.sendWebSocketMsg(socketname,msg);
			
	}

	
	public void registerSocketMsgListener(ISocketMsgListener is) {
		socketMessageMQTTHandler.addListener(is);	
	}

	public void setModuleName(String moduleName2) {
		// TODO Auto-generated method stub
		this.moduleName=moduleName2;
	}

	
}
