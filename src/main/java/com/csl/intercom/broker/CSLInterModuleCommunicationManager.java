package com.csl.intercom.broker;

import com.csl.intercom.jsoncmd.JServiceLoader;
import com.csl.intercom.jsoncmd.XApiCommands;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.json.Json;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


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
	private static final Logger logger = LoggerFactory.getLogger(CSLInterModuleCommunicationManager.class);
	int idebug=1;

    @Getter
    boolean useBroker=false; //true; //  use mosquitto broker, if flase (for tests mainly, only local commands)
	
	boolean mustDeclareApi=false;

    @Setter
    String moduleName="XXX";

    private String BROKER_TCP_LOCALHOST_1883 = "tcp://localhost:1883";

	Map<String,IApiCommands> listOfRegisteredAPI= new HashMap<String, IApiCommands>();
	Map<String,ApiMessageReceiver> listOfReceivers= new HashMap<>();
	Map<String,ApiMessageSender> listOfSenders= new HashMap<>();
	
	SocketMessageMQTTHandler socketMessageMQTTHandler =null;
	
	public CSLInterModuleCommunicationManager(String moduleName, MosquittoConfig config) {
		this.moduleName=moduleName;
		setConfig(config);
		
		socketMessageMQTTHandler=new SocketMessageMQTTHandler(moduleName,BROKER_TCP_LOCALHOST_1883, useBroker, getDebugLevel());
	}
	
	public boolean isDebug() {return idebug>1;}

	public boolean isShowInfo() {return idebug>0;}

	public int getDebugLevel() { return idebug; }

	public void setConfig(MosquittoConfig config) {

        String mosquittoCmd = config.getMosquittoCmd();
        String mosquittoDir = config.getMosquittoDir();
		
		BROKER_TCP_LOCALHOST_1883=config.getBrokerURL();
		
		useBroker=config.isUseBroker();
		
	}

    public void registerAPI(IApiCommands api) {
		if (isShowInfo()) logger.info("REGISTER API FOR BROKER :"+api.getName());
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
		
		if (sender!=null){
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


}
