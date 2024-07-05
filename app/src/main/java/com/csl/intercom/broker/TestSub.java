package com.csl.intercom.broker;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

        public class TestSub implements  MqttCallback {

        	
        	public void init() {
        		 String topic        = "MQTT Examples";
                 String content      = "Message from MqttPublishSample";
                 int qos             = 2;
                 String broker       = "tcp://localhost:1883";
                 String clientId     = "JavaSample";
                 MemoryPersistence persistence = new MemoryPersistence();

                 try {
                   /*  MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
                     MqttConnectOptions connOpts = new MqttConnectOptions();
                     connOpts.setCleanSession(true);
                     System.out.println("Connecting to broker: "+broker);
                     sampleClient.connect(connOpts);
                     System.out.println("Connected");
                     System.out.println("Publishing message: "+content);
                     MqttMessage message = new MqttMessage(content.getBytes());
                     message.setQos(qos);
                     sampleClient.publish(topic, message);
                     System.out.println("Message published");
                     sampleClient.disconnect();
                     System.out.println("Disconnected");
                     System.exit(0);*/
                 	
               
                	 
                 	
                 	//We're using eclipse paho library  so we've to go with MqttCallback 
                 	 MqttClient client = new MqttClient("tcp://localhost:1883","clientid");
                 	     client.setCallback(this);
                 	MqttConnectOptions mqOptions=new MqttConnectOptions();
                 	     mqOptions.setCleanSession(true);
                 	     client.connect(mqOptions);      //connecting to broker 
                 	        client.subscribe("csl/response/api1"); //subscribing to the topic name  test/topic

                 	
                 
                 	
                 } catch(MqttException me) {
                     System.out.println("reason "+me.getReasonCode());
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
 	        System.out.println("message is : "+message);
 	    }
		
		
		public static void main(String[] args) {

	           new TestSub().init();
	           
        }

    }