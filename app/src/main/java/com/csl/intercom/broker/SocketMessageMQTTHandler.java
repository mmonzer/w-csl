package com.csl.intercom.broker;

import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;
import java.util.List;


// csl/services/requests/api_name



public class SocketMessageMQTTHandler implements  MqttCallback {
	String moduleName="XXX";
	int idebug=5;

	private static  String BROKER_TCP_LOCALHOST_1883 = "tcp://localhost:1883";

	public static String SOCKET_TOPIC="csl/socket/";

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
	}

	public void close() {
		if (clientToListen!=null) {
			try {
				clientToListen.unsubscribe(SOCKET_TOPIC);
				clientToListen.disconnect();
				clientToListen.close(true);
				subscribed=false;
			} catch (MqttException e) {
				e.printStackTrace();
			}
		}

		if (clientToSend!=null) {

			try {
				clientToSend.disconnect();
				if (isDebug()) System.out.println("Disconnected");
				clientToSend.close();
			} catch (MqttException e) {
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

	// listen the response to the request
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		if (isDebug())
			System.out.println("*************  websocket sender received message  : "+message);
		String s=new String(message.getPayload());

		try {
			Json j=Json.read(s);
			if (isDebug())System.out.println("JSON:"+j);

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

		String clientId     = moduleName+"_sendsocket";

		MemoryPersistence persistence = new MemoryPersistence();

		try {
			if (!clientToSend.isConnected()) {
				connectClientToSend();
			}
			String content = JsonUtil.prettyPrint(j);
			MqttMessage message = new MqttMessage(content.getBytes());
			message.setQos(qos);
			clientToSend.publish(SOCKET_TOPIC, message);
			if (isDebug())System.out.println("Message published "+content);

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