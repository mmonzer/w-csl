package com.csl.intercom.broker;

import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import org.apache.commons.net.ntp.TimeStamp;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MqttBrokerHandler implements AutoCloseable {
    private static final String clientId = "mqtt_agent_concentrator";
    private String apiKey;
    private String brokerUri;
    private Map<String, IMqttMessageListener> topics = new HashMap<>();
    private MqttClient mqttClient;
    private MqttConnectOptions mqttConnectOptions;
    private String organization = "None";
    private ScheduledExecutorService mqttConnectionAttempts;

    public MqttBrokerHandler(Json config) {
        Json globalConfig = config.get("global");
        brokerUri = JsonUtil.getBooleanFromJson(globalConfig, "use_ssl", true) ? "wss://" : "ws://";
        brokerUri += JsonUtil.getStringFromJson(globalConfig, "ip_server_remote", "localhost");
        brokerUri += "/mqtt";
        this.apiKey = globalConfig.get("api_key").asString();
        mqttConnectionAttempts = Executors.newScheduledThreadPool(1);
        mqttConnectionAttempts.scheduleAtFixedRate(this::connectToMqttClientIfNecessary, 0, 2, TimeUnit.SECONDS);
        mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setUserName(apiKey);
        mqttConnectOptions.setPassword("not_used".toCharArray());
    }

    @Override
    public void close() {
        if (mqttClient != null) {
            try {
                mqttConnectionAttempts.shutdown();
                mqttClient.disconnect();
            } catch (MqttException e) {
                e.printStackTrace(System.err);
            }
        }
    }

    public void subscribeToTopic(String topic, IMqttMessageListener callback) {
        String fullTopic = organization + "/" + topic;
        topics.put(fullTopic, callback);
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.subscribe(fullTopic, callback);
            } catch (MqttException e) {
                e.printStackTrace(System.err);
            }
        }
    }

    private void connectToMqttClientIfNecessary() {
        if (mqttClient == null || !mqttClient.isConnected()) {
            try {
                mqttClient = new MqttClient(brokerUri, clientId + Instant.now().toEpochMilli());
                MqttConnectOptions connectOptions = new MqttConnectOptions();
                mqttClient.connect(mqttConnectOptions);
                for (Map.Entry<String, IMqttMessageListener> topic: topics.entrySet()) {
                    mqttClient.subscribe(topic.getKey(), topic.getValue());
                }
            } catch (MqttException e) {
                mqttClient = null;
                System.err.println("Could not connect to MQTT Broker at " + brokerUri);
            }
        }
    }
}
