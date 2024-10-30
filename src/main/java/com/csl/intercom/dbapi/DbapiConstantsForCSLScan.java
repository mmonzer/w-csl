package com.csl.intercom.dbapi;

import java.util.HashMap;
import java.util.Map;

public class DbapiConstantsForCSLScan {
    public static final Map<String, String> connectionFieldsDbapiToLocal = new HashMap<>();

    static {
        connectionFieldsDbapiToLocal.put("discovery_protocol", "protocol");
        connectionFieldsDbapiToLocal.put("port_number", "port");
        connectionFieldsDbapiToLocal.put("connected_devices", "devices");
        connectionFieldsDbapiToLocal.put("snmp_community", "community");
        connectionFieldsDbapiToLocal.put("username", "user");
        connectionFieldsDbapiToLocal.put("read_only_password", "pass");
        connectionFieldsDbapiToLocal.put("snmp_privacy_key", "privPassPhrase");
        connectionFieldsDbapiToLocal.put("authentication_algorithm", "authProtocolName");
        connectionFieldsDbapiToLocal.put("privacy_algorithm", "privProtocolName");
    }

    public static final Map<String, String> authAlgorithmDbapiToScan = new HashMap<>();

    static {
        authAlgorithmDbapiToScan.put("SHA-224", "AuthHMAC128SHA224");
        authAlgorithmDbapiToScan.put("SHA-256", "AuthHMAC192SHA256");
        authAlgorithmDbapiToScan.put("SHA-384", "AuthHMAC256SHA384");
        authAlgorithmDbapiToScan.put("SHA-512", "AuthHMAC384SHA512");
        authAlgorithmDbapiToScan.put("SHA", "AuthSHA");
        authAlgorithmDbapiToScan.put("SHA2", "AuthSHA2");
        authAlgorithmDbapiToScan.put("MD5", "AuthMD5");
    }

    public static final Map<String, String> privAlgorithmeDbapiToScan = new HashMap<>();

    static {
        privAlgorithmeDbapiToScan.put("AES", "PrivAES127");
        privAlgorithmeDbapiToScan.put("AES-128", "PrivAES128");
        privAlgorithmeDbapiToScan.put("AES-192", "PrivAES192");
        privAlgorithmeDbapiToScan.put("AES-256", "PrivAES256");
        privAlgorithmeDbapiToScan.put("DES", "PrivDES");
    }
}
