package com.csl.intercom.dbapi;

import java.util.HashMap;
import java.util.Map;

public class DbapiConstants {
    public static final Map<String, String> connectionFieldsDbapiToLocal = new HashMap<>() {{
        put("discovery_protocol", "protocol");
        put("port_number", "port");
        put("connected_devices", "devices");
        put("snmp_community", "community");
        put("username", "user");
        put("read_only_password", "pass");
        put("snmp_privacy_key", "privPassPhrase");
        put("authentication_algorithm", "authProtocolName");
        put("privacy_algorithm", "privProtocolName");
    }};
    public static final Map<String, String> authAlgorithmDbapiToScan = new HashMap<>() {{
        put("SHA-224", "AuthHMAC128SHA224");
        put("SHA-256", "AuthHMAC192SHA256");
        put("SHA-384", "AuthHMAC256SHA384");
        put("SHA-512", "AuthHMAC384SHA512");
        put("SHA", "AuthSHA");
        put("SHA2", "AuthSHA2");
        put("MD5", "AuthMD5");
    }};
    public static final Map<String, String> privAlgorithmeDbapiToScan = new HashMap<>() {{
        put("AES", "PrivAES127");
        put("AES-128", "PrivAES128");
        put("AES-192", "PrivAES192");
        put("AES-256", "PrivAES256");
        put("DES", "PrivDES");
    }};
}
