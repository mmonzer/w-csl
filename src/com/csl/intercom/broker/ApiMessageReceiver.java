package com.csl.intercom.broker;



/*
 * 
 * 
 * 		listen the topic    csl/requests/api_name
 * 		
 * 			dispatch to 	csl/request/api_name (found in request)
 * 
 * 			if broker_url	useit
 */

//ajouter timestap

//et list par api des com envouess



import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.xcsl.interfaces.IApiCommands;
import com.xcsl.json.Json;
import com.xcsl.json.JsonUtil;

public class ApiMessageReceiver implements  MqttCallback {

	
	int idebug=0;

	String moduleName="XRECEIVER";
	
	private static  String BROKER_TCP_LOCALHOST_1883 = "tcp://localhost:1883";

	public static String REQUEST_TOPIC="csl/request/";
	public static String RESPONSE_TOPIC="csl/response/";

	public String api="";

	private IApiCommands apiCommands;
	MqttClient clientToListen=null;
	boolean subscribed=false;
	
	MqttClient clientToSend=null;
	
	public ApiMessageReceiver(String moduleName,String apiName,IApiCommands apiCommands,String brokerUrl, int debugLevel) {
		
		if (!brokerUrl.isEmpty()) BROKER_TCP_LOCALHOST_1883=brokerUrl;
		
		this.moduleName=moduleName;
		this.apiCommands=apiCommands;
		this.api=apiName;
		setDebugLevel(debugLevel);
		this.init();
		
	}

//	public ApiMessageReceiver(String moduleName,String apiName, ApiCommands apiCommands, int debugLevel) {
//		
//		this(moduleName,apiName,apiCommands,"",debugLevel);
//		System.out.println("Create MSG sender "+moduleName+" for api "+apiName);
//		
//	}
	
	public boolean isDebug() {return idebug>1;}
	public boolean isShowInfo() {return idebug>0;}
	public void setDebugLevel(int d) {idebug=d;}
	

	
	/*
	 * String receiverId = UUID.randomUUID().toString();

IMqttClient receiver = new MqttClient(
                "tcp://" + properties.getProperty("host") + ":" + properties.getProperty("port"), receiverId, new MqttDefaultFilePersistence("/tmp"));
	 */
	
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
		
		if (isShowInfo()) System.out.println("Init receiver api:"+api);
		
		if (subscribed) close();
		
		MemoryPersistence persistence = new MemoryPersistence();

		try {

			//We're using eclipse paho library  so we've to go with MqttCallback 
			clientToListen = new MqttClient(BROKER_TCP_LOCALHOST_1883,getClientToListenID(),persistence); //new MqttDefaultFilePersistence("./tmp"));
			clientToListen.setCallback(this);
			MqttConnectOptions mqOptions=new MqttConnectOptions();
			mqOptions.setCleanSession(true);
			clientToListen.connect(mqOptions);      //connecting to broker 
			clientToListen.subscribe(getClientToListenTopic()); //subscribing to the topic name  test/topic
			if (isShowInfo()) System.out.println("**** listening MQTT topic:"+getClientToListenTopic());
			subscribed=true;
			
		} catch(MqttException me) {
			System.out.println("reason "+me.getReasonCode());
			System.out.println("msg "+me.getMessage());
			System.out.println("loc "+me.getLocalizedMessage());
			System.out.println("cause "+me.getCause());
			System.out.println("excep "+me);
			me.printStackTrace();
		}
		
		connectClientToSend();
	}
	
	private void connectClientToSend() {
		try {
			
			int qos             = 2;
			String clientId     = getClientToSendID();

			if (clientToSend==null)	clientToSend = new MqttClient(BROKER_TCP_LOCALHOST_1883, clientId, new MemoryPersistence());
			MqttConnectOptions connOpts = new MqttConnectOptions();
			connOpts.setCleanSession(true);
			if (isDebug())System.out.println("Connecting to broker: "+BROKER_TCP_LOCALHOST_1883);
			clientToSend.connect(connOpts);
			if (isDebug())System.out.println("Connected");

			

		} catch(Exception me) {
			System.out.println(me);
			//  System.out.println("reason "+me.getReasonCode());
			System.out.println("msg "+me.getMessage());
			System.out.println("loc "+me.getLocalizedMessage());
			System.out.println("cause "+me.getCause());
			System.out.println("excep "+me);
			me.printStackTrace();
		}

		
		
	}



	public void close() {
		try {
			clientToListen.unsubscribe(getClientToListenTopic());
			clientToListen.disconnect();
			clientToListen.close(true);
			subscribed=false;
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	
		try {
			
			clientToSend.disconnect();
			if (isDebug())System.out.println("Disconnected");
			clientToSend.close();

		} catch(Exception me) {
			System.out.println(me);
			//  System.out.println("reason "+me.getReasonCode());
			System.out.println("msg "+me.getMessage());
			System.out.println("loc "+me.getLocalizedMessage());
			System.out.println("cause "+me.getCause());
			System.out.println("excep "+me);
			me.printStackTrace();
		}
		
	}

	@Override
	public void connectionLost(Throwable arg0) {
		// TODO Auto-generated method stub

	}


	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// TODO Auto-generated method stub

	}


	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		if (isDebug()) System.out.println("message is : "+message);

		String s=new String(message.getPayload());

		//String broker_url=BROKER_TCP_LOCALHOST_1883;
		
		try {
			Json j=Json.read(s);
			if (isDebug())System.out.println("JSON:"+j);

			// api to which send the result
			String api="";
			if (j.has("api")) api=j.get("api").asString();

			if (!api.isEmpty()) {
				Json response=Json.object();
				response.set("reqId",j.get("reqId"));
				
				Json result;
				if (j.has("jcmd")) {
					result=apiCommands.execJcmd(j.get("jcmd"));
				}
				//if (j.has("jwebsocket")) {
				//	sendWebSocketMsg(j.get("jwebsocket"));
				//}
				else {
					result=Json.object().set("error","jcmd  missing");
				}
				
				response.set("result", result);
				//if (j.has("broker_url")) broker_url=j.get("broker_url").asString();
				
				sendMqttMsg(getClientToSendTopic(), response); //RESPONSE_TOPIC+api,response);
			}
			if (isDebug()) System.out.println("message sent");

		}
		catch (Exception e) {
			System.out.println(e);
		}


	}



	public void sendMqttMsg(String topic,Json j ) {


		
		try {
			
			int qos             = 2;
//			String clientId     = moduleName+"_"+api+"_send";
//
//			MemoryPersistence persistence = new MemoryPersistence();
//
//			sampleClient = new MqttClient(BROKER_TCP_LOCALHOST_1883, clientId, persistence);
//			MqttConnectOptions connOpts = new MqttConnectOptions();
//			connOpts.setCleanSession(true);
//			if (isDebug())System.out.println("Connecting to broker: "+BROKER_TCP_LOCALHOST_1883);
//			sampleClient.connect(connOpts);
//			if (isDebug())System.out.println("Connected");
			
			
			if (!clientToSend.isConnected()) {
				connectClientToSend();
			}

			if (isDebug())System.out.println("topic:"+topic);
			String content = JsonUtil.prettyPrint(j);
			MqttMessage message = new MqttMessage(content.getBytes());
			message.setQos(qos);
			clientToSend.publish(topic, message);
			if (isDebug())System.out.println("Message published "+content);

//			sampleClient.disconnect();
//			if (isDebug())System.out.println("Disconnected");
//			sampleClient.close();

		} catch(Exception me) {
			System.out.println(me);
			//  System.out.println("reason "+me.getReasonCode());
			System.out.println("msg "+me.getMessage());
			System.out.println("loc "+me.getLocalizedMessage());
			System.out.println("cause "+me.getCause());
			System.out.println("excep "+me);
			me.printStackTrace();
		}


	}



//	public static void main(String[] args) {
//		new MessageDispatcher().init();
//
//		while (true) {
//			try {
//				Thread.sleep(100);
//				//System.out.println("test");
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//	}

}