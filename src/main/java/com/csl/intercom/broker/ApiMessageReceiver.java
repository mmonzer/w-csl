package com.csl.intercom.broker;

import com.csl.intercom.jsoncmd.ApiCommands;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiMessageReceiver implements  MqttCallback {
	private static final Logger logger = LoggerFactory.getLogger(ApiMessageReceiver.class);
	int idebug=0;

	String moduleName="XRECEIVER";
	
	private String brokerTcpLocalhost1883 = "tcp://localhost:1883";

	public static final String REQUEST_TOPIC="csl/request/";
	public static final String RESPONSE_TOPIC="csl/response/";

	private String api="";

	private final ApiCommands apiCommands;
	MqttClient clientToListen=null;
	boolean subscribed=false;
	
	MqttClient clientToSend=null;
	
	public ApiMessageReceiver(String moduleName,String apiName,ApiCommands apiCommands,String brokerUrl, int debugLevel) {
		if (!brokerUrl.isEmpty()) brokerTcpLocalhost1883 = brokerUrl;
		
		this.moduleName=moduleName;
		this.apiCommands=apiCommands;
		this.api=apiName;
		setDebugLevel(debugLevel);
		this.init();
		
	}

	public boolean isDebug() {return idebug>1;}
	public boolean isShowInfo() {return idebug>0;}
	public void setDebugLevel(int d) {idebug=d;}
	
	public String getClientToListenTopic() {
		return REQUEST_TOPIC+api;
	}
	
	public String getClientToSendTopic() {
		return RESPONSE_TOPIC+api;
	}
	
	public String getClientToListenID() {
		return "R_"+moduleName+api+"_listen";
	}
	
	public String getClientToSendID() {
		return "R_"+moduleName+api+"_send";
	}
	
	public void init() {
		if (isShowInfo()) logger.info("Init receiver api: {}", api);
		if (subscribed) close();

		try (MemoryPersistence persistence = new MemoryPersistence()){
			//We're using eclipse paho library  so we've to go with MqttCallback
			clientToListen = new MqttClient(brokerTcpLocalhost1883,getClientToListenID(),persistence);
			clientToListen.setCallback(this);
			MqttConnectOptions mqOptions=new MqttConnectOptions();
			mqOptions.setCleanSession(true);
			clientToListen.connect(mqOptions);      //connecting to broker 
			clientToListen.subscribe(getClientToListenTopic()); //subscribing to the topic name  test/topic
			if (isShowInfo()) logger.info("Listening MQTT topic: {}", getClientToListenTopic());
			subscribed=true;
			
		} catch(MqttException me) {
			logger.warn("Error while connecting to MQTT broker: {}", brokerTcpLocalhost1883, me);
		}
		
		connectClientToSend();
	}
	
	private void connectClientToSend() {
		try {
			String clientId     = getClientToSendID();

			if (clientToSend==null)	clientToSend = new MqttClient(brokerTcpLocalhost1883, clientId, new MemoryPersistence());
			MqttConnectOptions connOpts = new MqttConnectOptions();
			connOpts.setCleanSession(true);
			if (isDebug())logger.debug("Connecting to broker: {}", brokerTcpLocalhost1883);
			clientToSend.connect(connOpts);
			if (isDebug()) logger.debug("Connected");
		} catch(Exception me) {
			logger.warn("Error while connecting to MQTT broker: {}", brokerTcpLocalhost1883, me);
		}
	}

	public void close() {
		try {
			clientToListen.unsubscribe(getClientToListenTopic());
			clientToListen.disconnect();
			clientToListen.close(true);
			subscribed=false;
		} catch (MqttException e) {
			logger.warn("Error while closing MQTT client", e);
		}

		try {
			clientToSend.disconnect();
			if (isDebug()) logger.debug("Disconnected");
			clientToSend.close();

		} catch(Exception me) {
			logger.warn("Error while closing MQTT client", me);
		}
	}

	@Override
	public void connectionLost(Throwable arg0) {
	   // TODO : add logs
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// TODO : add logs
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) {
		if (isDebug()) logger.debug("message is: {}", message);

		String payloadString = new String(message.getPayload());

		try {
			Json payload = Json.read(payloadString);
			if (isDebug()) logger.debug("JSON:{}",payload);

			String apiName="";
			if (payload.has("api")) apiName=payload.get("api").asString();

			if (!apiName.isEmpty()) {
				Json response=Json.object();
				response.set("reqId",payload.get("reqId"));
				
				Json result;
				if (payload.has("jcmd")) {
					result=apiCommands.execJcmd(payload.get("jcmd"));
				} else {
					result=Json.object().set("error","jcmd  missing");
				}
				
				response.set("result", result);

				sendMqttMsg(getClientToSendTopic(), response);
			}
			if (isDebug()) logger.debug("message sent");
		}
		catch (Exception e) {
			logger.error("Error while parsing message {} : {}", payloadString, e.getMessage());
		}
	}

	public void sendMqttMsg(String topic,Json j ) {
		try {
			int qos             = 2;

			if (!clientToSend.isConnected()) {
				connectClientToSend();
			}

			if (isDebug()) logger.debug("topic : {}", topic);
			String content = JsonUtil.prettyPrint(j);
			MqttMessage message = new MqttMessage(content.getBytes());
			message.setQos(qos);
			clientToSend.publish(topic, message);
			if (isDebug()) logger.debug("Message published : {}", content);

		} catch(Exception me) {
			logger.warn("Error while sending MQTT message : {}", me.getMessage());
		}
	}
}