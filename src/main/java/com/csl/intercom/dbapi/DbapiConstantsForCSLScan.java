package com.csl.intercom.dbapi;

import java.util.HashMap;
import java.util.Map;

public class DbapiConstantsForCSLScan {
    private static final Map<String, String> CONNECTION_FIELDS_DBAPI_TO_LOCAL = new HashMap<>();

    static {
        CONNECTION_FIELDS_DBAPI_TO_LOCAL.put("discovery_protocol", "protocol");
        CONNECTION_FIELDS_DBAPI_TO_LOCAL.put("port_number", "port");
        CONNECTION_FIELDS_DBAPI_TO_LOCAL.put("connected_devices", "devices");
        CONNECTION_FIELDS_DBAPI_TO_LOCAL.put("snmp_community", "community");
        CONNECTION_FIELDS_DBAPI_TO_LOCAL.put("username", "user");
        CONNECTION_FIELDS_DBAPI_TO_LOCAL.put("read_only_password", "pass");
        CONNECTION_FIELDS_DBAPI_TO_LOCAL.put("snmp_privacy_key", "privPassPhrase");
        CONNECTION_FIELDS_DBAPI_TO_LOCAL.put("authentication_algorithm", "authProtocolName");
        CONNECTION_FIELDS_DBAPI_TO_LOCAL.put("privacy_algorithm", "privProtocolName");
    }

    private static final Map<String, String> AUTH_ALGORITHM_DBAPI_TO_SCAN = new HashMap<>();

    static {
        AUTH_ALGORITHM_DBAPI_TO_SCAN.put("SHA-224", "AuthHMAC128SHA224");
        AUTH_ALGORITHM_DBAPI_TO_SCAN.put("SHA-256", "AuthHMAC192SHA256");
        AUTH_ALGORITHM_DBAPI_TO_SCAN.put("SHA-384", "AuthHMAC256SHA384");
        AUTH_ALGORITHM_DBAPI_TO_SCAN.put("SHA-512", "AuthHMAC384SHA512");
        AUTH_ALGORITHM_DBAPI_TO_SCAN.put("SHA", "AuthSHA");
        AUTH_ALGORITHM_DBAPI_TO_SCAN.put("SHA2", "AuthSHA2");
        AUTH_ALGORITHM_DBAPI_TO_SCAN.put("MD5", "AuthMD5");
    }

    private static final Map<String, String> PRIV_ALGORITHME_DBAPI_TO_SCAN = new HashMap<>();

    static {
        PRIV_ALGORITHME_DBAPI_TO_SCAN.put("AES", "PrivAES127");
        PRIV_ALGORITHME_DBAPI_TO_SCAN.put("AES-128", "PrivAES128");
        PRIV_ALGORITHME_DBAPI_TO_SCAN.put("AES-192", "PrivAES192");
        PRIV_ALGORITHME_DBAPI_TO_SCAN.put("AES-256", "PrivAES256");
        PRIV_ALGORITHME_DBAPI_TO_SCAN.put("DES", "PrivDES");
    }
}
