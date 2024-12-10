package com.csl.intercom.broker;

import com.csl.core.Config;
import com.csl.intercom.dbapi.DbapiHandlerForCSLScan;
import com.csl.logger.LoggerCustomEndpoints;
import com.csl.logger.LoggerInterfaces;
import com.csl.util.ThreadUtils;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
        CPE_ITEMS("configuration_discovered_item"),

        FILE_ACTION_STATUS("file_action_status"),
        CONFIGURATIONS("configuration")
        ;

        private final String name;

        Topic(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final String CLIENT_ID = "mqtt_agent_concentrator";       // MQTT client id's prefix
    private String organization = "None";                                   // The organization, default id None.
    private MqttClient mqttClient;
    private String brokerUri;
    private final Map<String, IMqttMessageListener> topics = new HashMap<>();     // The topic we should subscribe to, together with callbacks to execute when a message is received on that topic
    private final MqttConnectOptions mqttConnectOptions;
    private final ScheduledExecutorService mqttConnectionAttempts;

    private static final Logger logger = LoggerFactory.getLogger(CSLMqttBrokerHandler.class);

    /**
     * Create a new {@link CSLMqttBrokerHandler} from the project's configuration.
     *
     * @param config The configuration of the project. Can be retrieved with <code>CSLContext.getInstance().getConfig()</code>.
     */
    public CSLMqttBrokerHandler(Config config) {
        Config.Client clientConfig = config.Client;
        brokerUri = clientConfig.getUseSsl() ? "wss://" : "ws://";
        brokerUri += clientConfig.getIpServerRemote();
        brokerUri += "/mqtt";
        // The API key to include in each message
        String apiKey = clientConfig.getApiKey();

        // Get the organization name, or "None" if it doesn't exist.
        try (DbapiHandlerForCSLScan dbapiHandler = new DbapiHandlerForCSLScan(config)) {
            this.organization = dbapiHandler.getMqttTopicPrefix();
        } catch (Exception e) {
            this.organization = "None";
        }
        mqttConnectionAttempts = Executors.newScheduledThreadPool(1);
        ThreadUtils.uncorrelatedSingleThreadScheduledAtFixedRate(
                mqttConnectionAttempts,
                this::connectToMqttClientIfNecessary,
                0, 10, TimeUnit.SECONDS,
                LoggerCustomEndpoints.RECONNECT_MQTT, LoggerInterfaces.CSL_CLIENT
        );
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
                logger.info("MQTT client disconnected");
            } catch (MqttException e) {
                logger.error(e.getMessage());
            }
        }
    }

    private void subscribeToTopic(String topic, IMqttMessageListener callback) {
        String fullTopic = genFullTopic(topic);
        topics.put(fullTopic, callback);
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.subscribe(fullTopic, callback);
                logger.info("MQTT client subscribed to {}", topic);
            } catch (MqttException e) {
                logger.error(e.getMessage());
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

    private String genFullTopic(String topic) {
        return organization + "/" + topic;
    }

    private void connectToMqttClientIfNecessary() {
        if (mqttClient == null || !mqttClient.isConnected()) {
            try (MemoryPersistence persistence = new MemoryPersistence()) {
                removeLockFiles();
                mqttClient = new MqttClient(brokerUri, CLIENT_ID + UUID.randomUUID(), persistence);
                mqttClient.connect(mqttConnectOptions);
                for (Map.Entry<String, IMqttMessageListener> topic : topics.entrySet()) {
                    mqttClient.subscribe(topic.getKey(), topic.getValue());
                }
            } catch (MqttException e) {
                if (!"MqttException (0) - java.io.IOException: Already connected".equals(e.toString())) {
                    mqttClient = null;
                    logger.warn("Could not connect to MQTT Broker at {}", brokerUri);
                }
            }
        }
    }

    /**
     * Remove the existing lock files of the MQTT client.
     * These should be located in subdirectories of the working directory.
     */
    private static void removeLockFiles() {
        String regex = CLIENT_ID + ".*";
        try {
            Path workDir = Paths.get("");
            Files.walkFileTree(workDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    String dirName = dir.getFileName().toString();
                    if (dirName.matches(regex)) {
                        // Remove the contents of the directory if it exists, should be only one file : .lck
                        Files.deleteIfExists(Path.of(dirName + "/.lck"));
                        // Delete the directory itself
                        Files.delete(dir);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            logger.error("Could not delete MQTT lock files");
        }
    }
}
