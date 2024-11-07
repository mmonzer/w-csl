package com.csl.intercom.broker;

import com.csl.logger.CSLApplicativeLogger;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;
import java.util.List;


// csl/services/requests/api_name

public class SocketMessageMQTTHandler implements MqttCallback {
    public static final String WEBSOCKET = "websocket";
    public static final String CONTENTS = "contents";
    CSLApplicativeLogger logger = CSLApplicativeLogger.getLogger(SocketMessageMQTTHandler.class);

    String moduleName = "XXX";
    int idebug = 5;

    private static final String BROKER_TCP_LOCALHOST_1883 = "tcp://localhost:1883";

    public static final String SOCKET_TOPIC = "csl/socket/";

    boolean subscribed = false;

    List<ISocketMsgListener> listeners = new ArrayList<>();

    MqttClient clientToListen = null;
    MqttClient clientToSend = null;

    boolean useBroker = true;

    public SocketMessageMQTTHandler(boolean useBroker, int debugLevel) {

        this.useBroker = useBroker;

        setDebugLevel(debugLevel);


        if (useBroker) this.init();
    }

    public boolean isDebug() {
        return idebug > 1;
    }

    public boolean isShowInfo() {
        return idebug > 0;
    }

    public void setDebugLevel(int d) {
        idebug = d;
    }

    public void init() {
        if (subscribed) close();
        try (MemoryPersistence persistence = new MemoryPersistence()) {

            //We're using eclipse paho library so we've to go with MqttCallback
            clientToListen = new MqttClient(BROKER_TCP_LOCALHOST_1883, moduleName + "_listen", persistence);
            clientToListen.setCallback(this);
            MqttConnectOptions mqOptions = new MqttConnectOptions();
            mqOptions.setCleanSession(true);
            clientToListen.connect(mqOptions);      //connecting to broker
            clientToListen.subscribe(SOCKET_TOPIC); //subscribing to the topic name  test/topic
            logger.debug("**** listening MQTT topic:" + SOCKET_TOPIC + "(" + (moduleName + "_listen") + ")");

            subscribed = true;
        } catch (MqttException me) {
            logger.warn("Mqtt initialization failed : reason {}", me.getMessage());
        }
        connectClientToSend();
    }

    private void connectClientToSend() {
        // client to send
        try (MemoryPersistence persistence = new MemoryPersistence()) {
            String clientId = moduleName + "_sendsocket";

            if (clientToSend == null)
                clientToSend = new MqttClient(BROKER_TCP_LOCALHOST_1883, clientId, new MemoryPersistence());
            clientToSend = new MqttClient(BROKER_TCP_LOCALHOST_1883, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            logger.debug("Connecting to broker: " + BROKER_TCP_LOCALHOST_1883);
            clientToSend.connect(connOpts);
            logger.debug("Connected, topic=" + SOCKET_TOPIC);
        } catch (MqttException me) {
            logger.warn("Mqtt failed to connect : reason {}", me.getMessage());
        }
    }

    public void close() {
        if (clientToListen != null) {
            try {
                clientToListen.unsubscribe(SOCKET_TOPIC);
                clientToListen.disconnect();
                clientToListen.close(true);
                subscribed = false;
            } catch (MqttException e) {
                logger.warn("Mqtt listener failed to close : reason {}", e.getMessage());
            }
        }

        if (clientToSend != null) {

            try {
                clientToSend.disconnect();
                logger.debug("Disconnected");
                clientToSend.close();
            } catch (MqttException e) {
                logger.warn("Mqtt sender failed to close : reason {}", e.getMessage());
            }
        }
    }

    @Override
    public void connectionLost(Throwable arg0) { // Still to do
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken arg0) { // Still to do
    }

    // listen the response to the request
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String messageContent = new String(message.getPayload());

        try {
            Json messajeJson = Json.read(messageContent);

            String websocket = "";
            if (messajeJson.has(WEBSOCKET)) {
                websocket = messajeJson.get(WEBSOCKET).asString();
                String contents = "";
                if (messajeJson.has(CONTENTS)) {
                    contents = messajeJson.get(CONTENTS).asString();
                }
                for (ISocketMsgListener is : listeners) {
                    is.messageArrived(websocket, contents);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to read mqtt message");
        }
    }

    public void sendWebSocketMsg(String socketname, String contents) {
        Json fullMsg = Json.object();

        fullMsg.set(WEBSOCKET, socketname);
        fullMsg.set(CONTENTS, contents);

        if (useBroker) {
            //sinon
            sendMqttMsg(fullMsg);
        } else {
            //si local
            for (ISocketMsgListener is : listeners) {
                is.messageArrived(socketname, contents);
            }
        }
    }

    /**
     * send request to the dispatcher
     */
    private void sendMqttMsg(Json j) {
        int qos = 2;

        try {
            if (!clientToSend.isConnected()) {
                connectClientToSend();
            }
            String content = JsonUtil.prettyPrint(j);
            MqttMessage message = new MqttMessage(content.getBytes());
            message.setQos(qos);
            clientToSend.publish(SOCKET_TOPIC, message);
        } catch (MqttException me) {
            logger.warn("Failed to send mqtt message : {}", me.getMessage());
        }
    }

    public void addListener(ISocketMsgListener is) {
        listeners.add(is);
    }
}