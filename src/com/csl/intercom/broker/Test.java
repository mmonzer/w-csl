package com.csl.intercom.broker;


// /usr/local/sbin/mosquitto -c /usr/local/etc/mosquitto/mosquitto.conf


import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.xcsl.json.Json;

        public class Test {

        public static void main(String[] args) {

            String topic        = "csl/request/";
            String content      = "Message from MqttPublishSample";
            int qos             = 2;
            String broker       = "tcp://localhost:1883";
            String clientId     = "JavaSample";
            MemoryPersistence persistence = new MemoryPersistence();

            try {
                MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
                MqttConnectOptions connOpts = new MqttConnectOptions();
                connOpts.setCleanSession(true);
                System.out.println("Connecting to broker: "+broker);
                sampleClient.connect(connOpts);
                System.out.println("Connected");
                System.out.println("Publishing message: "+content);
                MqttMessage message = new MqttMessage(content.getBytes());
                message.setQos(qos);
                sampleClient.publish(topic, message);
                System.out.println("Message published "+content);
                
                Json j= Json.object();
                j.set("from", "ids");
                j.set("to", "gateway");
                j.set("uuid", "121");
                j.set("jcmd", "eaeae");
                
                content = j.toString();
                message = new MqttMessage(content.getBytes());
                message.setQos(qos);
                sampleClient.publish(topic, message);
                System.out.println("Message published "+content);
                
                
                sampleClient.disconnect();
                System.out.println("Disconnected");
                System.exit(0);
            } catch(MqttException me) {
                System.out.println("reason "+me.getReasonCode());
                System.out.println("msg "+me.getMessage());
                System.out.println("loc "+me.getLocalizedMessage());
                System.out.println("cause "+me.getCause());
                System.out.println("excep "+me);
                me.printStackTrace();
            }
        }
    }