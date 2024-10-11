package com.csl.logger;

/**
 * "Endpoints" for the logger. Small string to identify for autonomous threads.
 */
public final class LoggerCustomEndpoints {
    public static final String SYNC_DISCOVERED_DEVICES= "synchronize discovered devices";
    public static final String RECONNECT_MQTT= "reconnect mqtt";
    public static final String SCAN_LIST_SANITIZER = "scan list sanitizer";
    public static final String RECONNECT_WS_CSL = "reconnect ws csl";
    public static final String KEEP_ALIVE_WS_CSL= "keep alive ws csl";
    public static final String HANDLE_IMPORT_EXPORT_TASKS= "handle import/export BSON task";
    public static final String START_IMPORT_BSON_TASK= "start import BSON task";
    public static final String TICS_MONITOR= "tics monitor";
    public static final String RECONNECT_WS_SCAN= "reconnect ws scan";
    public static final String MQTT_TIMEOUT_DETECTOR= "mqtt timeout detector";
    public static final String AUTO_DELETING_FILES= "auto deleting files";
    public static final String SYNC_EXT_CONNECTION_INFO= "synchronize external connection infos";
    public static final String DISCOVERY_SYNC= "discovery sync";
    public static final String AUTOCRYPT_SYNC= "autocrypt sync";
    public static final String KEEP_ALIVE_IHM_WS= "keep alive ihm ws";
}

