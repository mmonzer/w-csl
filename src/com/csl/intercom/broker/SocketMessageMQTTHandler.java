package com.csl.intercom.broker;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.xcsl.json.Json;
import com.xcsl.json.JsonUtil;


// csl/services/requests/api_name



public class SocketMessageMQTTHandler implements  MqttCallback {

	public static long TIME_OUT=10000;

	String moduleName="XXX";
	int idebug=5;

	private static  String BROKER_TCP_LOCALHOST_1883 = "tcp://localhost:1883";

	//private static final String RESPONSE = "response";

	//private static final String REQUEST = "request";

	//private static final String REQ_ID = "reqId";

	//public static int request_ctr=1; 

	public static String SOCKET_TOPIC="csl/socket/";


	//public static String RESPONSE_TOPIC="csl/response/";
	//public static String API_NAME="api1";

	//Map<String,Json> pendingMessages= new HashMap<>();
	//List<Json> pendingMessages= new ArrayList<Json>();

	//public String api="";

	boolean subscribed=false;

	List<ISocketMsgListener> listeners = new ArrayList<ISocketMsgListener>();

	MqttClient clientToListen=null;
	MqttClient clientToSend =null;

	boolean useBroker=true;

	public SocketMessageMQTTHandler(String moduleName,String brokerUrl,boolean useBroker, int debugLevel) {

		if (!brokerUrl.isEmpty()) BROKER_TCP_LOCALHOST_1883=brokerUrl;

		this.useBroker=useBroker;

		setDebugLevel(debugLevel);


		if (useBroker) this.init();

	}

//	public SocketMessageMQTTHandler(String moduleName, int debugLevel ) {
//
//		this(moduleName,"",true, debugLevel);
//
//	}


	public boolean isDebug() {return idebug>1;}
	public boolean isShowInfo() {return idebug>0;}
	public void setDebugLevel(int d) {idebug=d;}




	public void init() {

		if (isShowInfo()) System.out.println("Init socket sender");


		if (subscribed) close();
		try {

			//We're using eclipse paho library  so we've to go with MqttCallback 
			MemoryPersistence persistence = new MemoryPersistence();
			clientToListen = new MqttClient(BROKER_TCP_LOCALHOST_1883,moduleName+"_listen",persistence); //new MqttDefaultFilePersistence("./tmp"));
			clientToListen.setCallback(this);
			MqttConnectOptions mqOptions=new MqttConnectOptions();
			mqOptions.setCleanSession(true);
			clientToListen.connect(mqOptions);      //connecting to broker 
			clientToListen.subscribe(SOCKET_TOPIC); //subscribing to the topic name  test/topic
			if (isShowInfo()) System.out.println("**** listening MQTT topic:"+SOCKET_TOPIC+"("+(moduleName+"_listen")+")");

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
		// client to send

		try {
			int qos             = 2;
			// String broker       = BROKER_TCP_LOCALHOST_1883;

			String clientId     = moduleName+"_sendsocket";

			MemoryPersistence persistence = new MemoryPersistence();


			if (clientToSend==null)	clientToSend = new MqttClient(BROKER_TCP_LOCALHOST_1883, clientId, new MemoryPersistence());
			clientToSend = new MqttClient(BROKER_TCP_LOCALHOST_1883, clientId, persistence);
			MqttConnectOptions connOpts = new MqttConnectOptions();
			connOpts.setCleanSession(true);
			if (isDebug())System.out.println("Connecting to broker: "+BROKER_TCP_LOCALHOST_1883);
			clientToSend.connect(connOpts);
			if (isDebug())System.out.println("Connected, topic="+SOCKET_TOPIC);

		} catch(MqttException me) {
			System.out.println("reason "+me.getReasonCode());
			System.out.println("msg "+me.getMessage());
			System.out.println("loc "+me.getLocalizedMessage());
			System.out.println("cause "+me.getCause());
			System.out.println("excep "+me);
			me.printStackTrace();
		}

		//		

	}

	public void close() {

		if (clientToListen!=null) {
			try {
				clientToListen.unsubscribe(SOCKET_TOPIC);
				clientToListen.disconnect();
				clientToListen.close(true);
				subscribed=false;
			} catch (MqttException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}


		if (clientToSend!=null) {

			try {
				clientToSend.disconnect();
				if (isDebug()) System.out.println("Disconnected");
				clientToSend.close();
			} catch (MqttException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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


	//	// listen the response to the request
	//	@Override
	//	public void messageArrived(String topic, MqttMessage message) throws Exception {
	//		 System.out.println("*************  message is : "+message);
	//	        
	//	        String s=new String(message.getPayload());
	//	        
	//	        try {
	//	        	Json j=Json.read(s);
	//	        	System.out.println("JSON:"+j);
	//	        	
	//	        	String key="";
	//	        	
	//	        	if (j.has(REQ_ID)) key=j.get(REQ_ID).asString();
	//	        	
	//	        	if (!key.isEmpty()) {
	//	        		Json jo=pendingMessages.get(key);
	//	        		if (jo!=null) {
	//	        			jo.set(RESPONSE, j);
	//	        		}
	//	        	}
	//	        	
	//	        }
	//	        catch (Exception e) {
	//	        	System.out.println(e);
	//	        }
	//	}
	//
	//
	//	public void addToPendingRequest(Json j) {
	//	
	//		
	//		
	//	}


	// listen the response to the request
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		if (isDebug())
			System.out.println("*************  websocket sender received message  : "+message);

		String s=new String(message.getPayload());

		try {
			Json j=Json.read(s);
			if (isDebug())System.out.println("JSON:"+j);


			//  to which send the result
			String websocket="";
			if (j.has("websocket")) {
				websocket=j.get("websocket").asString();
				String contents="";
				if (j.has("contents")) {
					contents=j.get("contents").asString();
				}
				for (ISocketMsgListener is:listeners) {
					is.messageArrived(websocket, contents);
				}

			}


		}
		catch (Exception e) {
			System.out.println(e);
		}
	}


	public void sendWebSocketMsg(String socketname, String contents) {

		Json fullMsg=Json.object();

		fullMsg.set("websocket",socketname);
		fullMsg.set("contents", contents);


		if (useBroker) {
			//sinon
			sendMqttMsg(fullMsg);
			
		}
		else {
			//si local 
			for (ISocketMsgListener is:listeners) {
				is.messageArrived(socketname, contents);
			}
		}
		

	}


	/*
	 * 
	 * send request to the dispatcher
	 * 
	 */
	private void sendMqttMsg(Json j ) {


		if (isDebug()) System.out.println("Socket send :"+j);
		int qos             = 2;
		// String broker       = BROKER_TCP_LOCALHOST_1883;

		String clientId     = moduleName+"_sendsocket";

		MemoryPersistence persistence = new MemoryPersistence();

		try {
			//            MqttClient sampleClient = new MqttClient(BROKER_TCP_LOCALHOST_1883, clientId, persistence);
			//            MqttConnectOptions connOpts = new MqttConnectOptions();
			//            connOpts.setCleanSession(true);
			//            if (isDebug())System.out.println("Connecting to broker: "+BROKER_TCP_LOCALHOST_1883);
			//            sampleClient.connect(connOpts);
			//            if (isDebug())System.out.println("Connected, topic="+SOCKET_TOPIC);

			if (!clientToSend.isConnected()) {
				connectClientToSend();
			}
			String content = JsonUtil.prettyPrint(j);
			MqttMessage message = new MqttMessage(content.getBytes());
			message.setQos(qos);
			clientToSend.publish(SOCKET_TOPIC, message);
			if (isDebug())System.out.println("Message published "+content);

			//			sampleClient.disconnect();
			//			if (isDebug()) System.out.println("Disconnected");
			//			sampleClient.close();


		} catch(MqttException me) {
			System.out.println("reason "+me.getReasonCode());
			System.out.println("msg "+me.getMessage());
			System.out.println("loc "+me.getLocalizedMessage());
			System.out.println("cause "+me.getCause());
			System.out.println("excep "+me);
			me.printStackTrace();
		}
	}








	public void addListener(ISocketMsgListener is) {
		// TODO Auto-generated method stub
		listeners.add(is);
	}



}