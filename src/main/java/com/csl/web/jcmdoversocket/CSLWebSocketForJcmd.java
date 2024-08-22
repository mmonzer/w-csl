package com.csl.web.jcmdoversocket;

import com.ucsl.json.Json;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CSLWebSocketForJcmd {

    private static final Logger logger = LoggerFactory.getLogger(CSLWebSocketForJcmd.class);
    public static long uuidctr = 0;
    private static final String RESPONSE = "response";
    public static final String ID = "uuid";
    public static final String X_CORRELATION_ID = "X-Correlation-ID";
    public static long TIME_OUT = 60000;

    public static String WEB_SOCKET_CMD = "/cmd";

    static Map<String, Session> sessionMap = new ConcurrentHashMap<>();

    static Map<String, Json> pendingMessages = new HashMap<>();

    static private int idebug = 2;

    static public boolean isDebug() {
        return idebug > 1;
    }

    static public void addUser(String name, Session session) {
        name = name.toLowerCase();
        CSLWebSocketForJcmd.logger.info("Connected {} endpoint to the websocket", name);

        Session s = sessionMap.get(name);

        if (s != null && s != session) {
            try {
                s.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
            s.close();
        }

        sessionMap.put(name, session);
    }

    //Sends a message from one user to all users, along with a list of current usernames
    public static void broadcastMessageJson(String name, Json j) {


        System.out.println("TEST_BROADCAST:" + name + j);
        Session session = sessionMap.get(name);
        if (session == null) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            session = sessionMap.get(name);
            if (session == null) {
                System.err.println("Invalid api name " + name + " client not connected");
                return;
            }
        }
        j.set("api", name);
        String s = j.toString();


        try {
            session.getRemote().sendString(s, new WriteCallback() {
                @Override
                public void writeFailed(Throwable x) {
                    // Handle write failure
                    x.printStackTrace();
                }

                @Override
                public void writeSuccess() {
                    // Handle write success
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static long getUuid() {
        uuidctr++;
        if (uuidctr > Long.MAX_VALUE - 10) uuidctr = 0;
        return uuidctr;
    }

    public static void addApi(String apiName, Session user) {
        // TODO Auto-generated method stub
        addUser(apiName, user);
    }

    public static void removeUser(Session user) {
        // TODO Auto-generated method stub

        List<String> keysToRemove = new ArrayList<String>();
        for (String key : sessionMap.keySet()) {

            Session x = sessionMap.get(key);
            if (user.equals(x)) {
                keysToRemove.add(key);
                System.out.println("Remove session :" + key);
            }
        }
        for (String k : keysToRemove)
            sessionMap.remove(k);
        //to del keys
    }

    public static Json execJCmd(String apiName, Json jCmd, String xCorrelationId) {
        startTimeOutDetector();

        Json fullMsg = Json.object();

        String id = UUID.randomUUID().toString();

        fullMsg.set(ID, id);
        fullMsg.set(X_CORRELATION_ID, xCorrelationId);
        fullMsg.set("api", apiName);
        fullMsg.set("jcmd", jCmd);

        broadcastMessageJson(apiName, fullMsg);

        fullMsg.set("start_time", System.currentTimeMillis());

        pendingMessages.put(id, fullMsg);


        while (true) {

            try {
                Thread.sleep(3);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return Json.object().set("error", "timeout");
            }

            if (fullMsg.has(RESPONSE)) {
                logger.info("raw response {}",fullMsg.get(RESPONSE));
                if (isDebug()) System.out.println("*** " + fullMsg);
                pendingMessages.remove(id);
                Json rep = fullMsg.get(RESPONSE);
                if (rep.has("result")) return rep.get("result");
                return Json.object().set("error", "timeout");
            }
        }
    }

    static public void messageArrived(String message) {
        if (isDebug()) System.out.println("x*************  message is : " + message);

        String s = message;

        try {

            Json j = Json.read(s);

            String key = "";

            if (j.has(ID)) key = j.get(ID).asString();

            if (!key.isEmpty()) {
                Json jo = pendingMessages.get(key);
                if (jo != null) {
                    jo.set(RESPONSE, j);
                }
            }
        } catch
        (Exception e) {
            System.out.println(e);
        }
    }

    static boolean timeOutDetectorRunning = false;

    static void startTimeOutDetector() {
        if (timeOutDetectorRunning) return;
        timeOutDetectorRunning = true;

        ScheduledExecutorService executorService;
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(
                new Runnable() {
                    public void run() {
                        long current_time = System.currentTimeMillis();
                        List<String> toDel = new ArrayList<String>();

                        for (Map.Entry<String, Json> entry : pendingMessages.entrySet()) {

                            Json message = entry.getValue();
                            long start_time = message.get("start_time").asLong();
                            long end_time = start_time + TIME_OUT;
                            if (end_time < current_time) {
                                logger.warn("request timed out");
                                message.set(RESPONSE, Json.object().set("error", "TIMEOUT"));
                            }
                        }
                    }
                },
                0, 1, TimeUnit.SECONDS);
    }
}
