package com.csl.intercom.broker;

import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import org.eclipse.paho.client.mqttv3.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Handle the MQTT client.
 */
public class CSLMqttBrokerHandler implements AutoCloseable {
    /**
     * The known topics.
     */
    public enum Topic {
        DEVICES("device"),
        CPE_ITEMS("cpe_item");

        private String name;

        Topic(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final String clientId = "mqtt_agent_concentrator";       // MQTT client id's prefix
    private String apiKey;                                                  // The API key to include in each message
    private String organization = "None";                                   // The organization, default id None.
    private MqttClient mqttClient;
    private String brokerUri;
    private Map<String, IMqttMessageListener> topics = new HashMap<>();     // The topic we should subscribe to, together with callbacks to execute when a message is received on that topic
    private MqttConnectOptions mqttConnectOptions;
    private ScheduledExecutorService mqttConnectionAttempts;

    /**
     * Create a new {@link CSLMqttBrokerHandler} from the project's configuration.
     *
     * @param config The configuration of the project. Can be retrieved with <code>CSLContext.instance.getConfig()</code>.
     */
    public CSLMqttBrokerHandler(Json config) {
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

    private void subscribeToTopic(String topic, IMqttMessageListener callback) {
        String fullTopic = genFullTopic(topic);
        topics.put(fullTopic, callback);
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.subscribe(fullTopic, callback);
            } catch (MqttException e) {
                e.printStackTrace(System.err);
            }
        }
    }


    /**
     * Subscribe to a topic, with a callback to execute when a message is published in the topic.
     * If connection is currently unavailable, store the request for latter subscription.
     *
     * @param topic    The {@link Topic} to subscribe to.
     * @param callback The callback to execute. Takes two arguments (String, MqttMessage) and returns nothing.
     */
    public void subscribeToTopic(Topic topic, Consumer<CSLMqttMessage> callback) {
        subscribeToTopic(topic.toString(), (s, message) -> callback.accept(CSLMqttMessage.parse(new String(message.getPayload()))));
    }

    private void publish(String topic, String message) throws MqttException {
        String fullTopic = genFullTopic(topic);
        if (mqttClient != null && mqttClient.isConnected()) {
            mqttClient.publish(fullTopic, new MqttMessage(message.getBytes()));
        }
    }

    /**
     * Publish a message to a topic.
     *
     * @param topic   The {@link Topic} on which to send the message.
     * @param message The contents of the message.
     * @throws MqttException If the sending failed.
     */
    public void publish(Topic topic, CSLMqttMessage message) throws Exception {
        try {
            publish(topic.toString(), message.toString());
        } catch (MqttException e) {
            throw new Exception("MQTT error: " + e.getMessage());
        }
    }

    private String genFullTopic(String topic) {
        return organization + "/" + topic;
    }

    private void connectToMqttClientIfNecessary() {
        if (mqttClient == null || !mqttClient.isConnected()) {
            try {
                mqttClient = new MqttClient(brokerUri, clientId + Instant.now().toEpochMilli());
                MqttConnectOptions connectOptions = new MqttConnectOptions();
                mqttClient.connect(mqttConnectOptions);
                for (Map.Entry<String, IMqttMessageListener> topic : topics.entrySet()) {
                    mqttClient.subscribe(topic.getKey(), topic.getValue());
                }
            } catch (MqttException e) {
                mqttClient = null;
                System.err.println("Could not connect to MQTT Broker at " + brokerUri);
            }
        }
    }

}
