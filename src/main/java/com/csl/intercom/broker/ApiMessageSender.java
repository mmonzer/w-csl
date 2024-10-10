package com.csl.intercom.broker;

import com.csl.logger.LoggerInterfaces;
import com.csl.util.ThreadUtils;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ApiMessageSender implements MqttCallback {

    private static final Logger logger = LoggerFactory.getLogger(ApiMessageSender.class);
    public static long TIME_OUT = 10000;

    int idebug = 0;

    String moduleName = "XSENDER";

    private static String BROKER_TCP_LOCALHOST_1883 = "tcp://localhost:1883";

    private static final String RESPONSE = "response";

    private static final String REQ_ID = "reqId";

    public static int request_ctr = 1;

    public static String REQUEST_TOPIC = "csl/request/";
    public static String RESPONSE_TOPIC = "csl/response/";

    Map<String, Json> pendingMessages = new HashMap<>();

    public String api = "";

    boolean subscribed = false;

    MqttClient clientToListen = null;
    MqttClient clientToSend = null;

    public ApiMessageSender(String moduleName, String apiName, String brokerUrl, int debugLevel) {
        if (!brokerUrl.isEmpty()) BROKER_TCP_LOCALHOST_1883 = brokerUrl;

        this.api = apiName;
        setDebugLevel(debugLevel);
        this.init();
    }

    public ApiMessageSender(String moduleName, String apiName, int debugLevel) {
        this(moduleName, apiName, "", debugLevel);
        logger.info("Create MSG sender {} for api {}", moduleName, apiName);
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

    public String getClientToListenID() {
        return "S_" + moduleName + api + "_listen";
    }

    public String getClientToSendID() {
        return "S_" + moduleName + api + "_send";
    }

    public void init() {

        if (isShowInfo()) logger.info("Init sender api:" + api);
        if (subscribed) close();

        MemoryPersistence persistence = new MemoryPersistence();

        try {
            //We're using eclipse paho library  so we've to go with MqttCallback
            clientToListen = new MqttClient(BROKER_TCP_LOCALHOST_1883, getClientToListenID(), persistence);
            clientToListen.setCallback(this);
            MqttConnectOptions mqOptions = new MqttConnectOptions();
            mqOptions.setCleanSession(true);
            clientToListen.connect(mqOptions);      //connecting to broker
            clientToListen.subscribe(RESPONSE_TOPIC + api); //subscribing to the topic name  test/topic

            subscribed = true;
        } catch (MqttException me) {
            logger.error("Error while connecting to broker, reason {}, msg {}, loc {}, cause {}", me.getReasonCode(), me.getMessage(), me.getLocalizedMessage(), me.getCause(), me);
        }

        connectClientToSend();
    }

    private void connectClientToSend() {
        // client to send

        try {
            String clientId = getClientToSendID();
            if (clientToSend == null)
                clientToSend = new MqttClient(BROKER_TCP_LOCALHOST_1883, clientId, new MemoryPersistence());
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            if (isDebug()) logger.debug("Connecting to broker: " + BROKER_TCP_LOCALHOST_1883);
            clientToSend.connect(connOpts);
            if (isDebug()) logger.debug("Connected, topic=" + REQUEST_TOPIC + api);
        } catch (MqttException me) {
            logger.error("Error while connecting to broker, reason {}, msg {}, loc {}, cause {}", me.getReasonCode(), me.getMessage(), me.getLocalizedMessage(), me.getCause(), me);
        }

        ScheduledExecutorService executorService;
        executorService = Executors.newSingleThreadScheduledExecutor();
        ThreadUtils.uncorrelatedSingleThreadScheduledAtFixedRate(
                executorService,
                this::detectTimeOut,
                0, 1, TimeUnit.SECONDS,
                "mqtt timeout detector", LoggerInterfaces.CSL_CLIENT
        );
    }

    public void close() {
        try {
            clientToListen.unsubscribe(RESPONSE_TOPIC + api);
            clientToListen.disconnect();
            clientToListen.close(true);
            subscribed = false;
        } catch (MqttException e) {
            logger.error("Error while closing connection to broker, reason {}, msg {}, loc {}, cause {}", e.getReasonCode(), e.getMessage(), e.getLocalizedMessage(), e.getCause(), e);
        }
        try {

            clientToSend.disconnect();
            if (isDebug()) logger.debug("Disconnected");
            clientToSend.close();
        } catch (MqttException me) {
            logger.error("Error while closing connection to broker, reason {}, msg {}, loc {}, cause {}", me.getReasonCode(), me.getMessage(), me.getLocalizedMessage(), me.getCause(), me);
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
    public void messageArrived(String topic, MqttMessage message) {
        if (isDebug()) logger.debug("*************  message is : " + message);

        String s = new String(message.getPayload());

        try {
            Json j = Json.read(s);

            String key = "";

            if (j.has(REQ_ID)) key = j.get(REQ_ID).asString();

            if (!key.isEmpty()) {
                Json jo = pendingMessages.get(key);
                if (jo != null) {
                    jo.set(RESPONSE, j);
                }
            }
        } catch (Exception e) {
            logger.error("Error while parsing message {}", s, e);
        }
    }

    public Json execCmd(Json jCmd) {

        Json fullMsg = Json.object();

        String key = "" + request_ctr;

        request_ctr++;

        fullMsg.set(REQ_ID, key);
        fullMsg.set("api", api);
        fullMsg.set("jcmd", jCmd);

        sendMqttMsg(fullMsg);

        fullMsg.set("start_time", System.currentTimeMillis());

        pendingMessages.put(key, fullMsg);


        while (true) {

            try {
                Thread.sleep(3);
                if (isDebug()) logger.debug("{}", fullMsg);
            } catch (InterruptedException e) {
                logger.error("Error while waiting for response", e);
                return Json.object().set("error", "timeout");
            }

            if (fullMsg.has(RESPONSE)) {
                if (isDebug()) logger.debug("*** " + fullMsg);
                pendingMessages.remove(key);
                Json rep = fullMsg.get(RESPONSE);
                if (rep.has("result")) return rep.get("result");
                return Json.object().set("error", "no result");
            }
        }
    }

    /*
     *
     * send request to the dispatcher
     *
     */
    public void sendMqttMsg(Json j) {


        int qos = 2;

        try {
            if (!clientToSend.isConnected()) {
                connectClientToSend();
            }

            String content = JsonUtil.prettyPrint(j);
            MqttMessage message = new MqttMessage(content.getBytes());
            message.setQos(qos);
            clientToSend.publish(REQUEST_TOPIC + api, message);
            if (isDebug()) logger.debug("Message published {}", content);
        } catch (MqttException me) {
            logger.error("Error while sending message to broker, reason {}, msg {}, loc {}, cause {}", me.getReasonCode(), me.getMessage(), me.getLocalizedMessage(), me.getCause(), me);
        }
    }

    // detect timeout in pending messages
    public void detectTimeOut() {
        long current_time = System.currentTimeMillis();

        for (Map.Entry<String, Json> entry : pendingMessages.entrySet()) {

            Json message = entry.getValue();
            long start_time = message.get("start_time").asLong();
            long end_time = start_time + TIME_OUT;
            if (end_time < current_time) {
                if (isDebug()) logger.debug("Time out: {}", message);
                message.set(RESPONSE, Json.object().set("error", "TIMEOUT"));
            }
        }
    }
}