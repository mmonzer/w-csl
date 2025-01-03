package com.csl.intercom.cslscan;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScanConstants {
    private ScanConstants(){}
    @Getter
    private static final Map<String, String> connectionInfoFields = new HashMap<>();
    public static final String QUERY_PROTOCOL = "queryProtocol";
    public static final String COMMUNITY = "community";
    public static final String USER = "user";
    public static final String PASS = "pass";
    public static final String PRIV_PASS_PHRASE = "privPassPhrase"; ad
    public static final String SECURITY_LEVEL = "securityLevel";
    public static final String AUTH_PROTOCOL_NAME = "authProtocolName";
    public static final String PRIV_PROTOCOL_NAME = "privProtocolName";

    static {
        connectionInfoFields.put(QUERY_PROTOCOL, QUERY_PROTOCOL);
        connectionInfoFields.put(COMMUNITY, COMMUNITY);
        connectionInfoFields.put(USER, USER);
        connectionInfoFields.put(PASS, PASS);
        connectionInfoFields.put(PRIV_PASS_PHRASE, PRIV_PASS_PHRASE);
        connectionInfoFields.put(SECURITY_LEVEL, SECURITY_LEVEL);
        connectionInfoFields.put(AUTH_PROTOCOL_NAME, AUTH_PROTOCOL_NAME);
        connectionInfoFields.put(PRIV_PROTOCOL_NAME, PRIV_PROTOCOL_NAME);
    }

    static final List<String> snmpv2cConnectionInfoFields = new ArrayList<>();
    static {
        snmpv2cConnectionInfoFields.add(QUERY_PROTOCOL);
        snmpv2cConnectionInfoFields.add(COMMUNITY);
    }

    static final List<String> snmpv3ConnectionInfoFields = new ArrayList<>();
    static {
        snmpv3ConnectionInfoFields.add(QUERY_PROTOCOL);
        snmpv3ConnectionInfoFields.add(USER);
        snmpv3ConnectionInfoFields.add(PASS);
        snmpv3ConnectionInfoFields.add(PRIV_PASS_PHRASE);
        snmpv3ConnectionInfoFields.add(SECURITY_LEVEL);
        snmpv3ConnectionInfoFields.add(AUTH_PROTOCOL_NAME);
        snmpv3ConnectionInfoFields.add(PRIV_PROTOCOL_NAME);
    }

    @Getter
    private static final List<String> finishedScanStatuses = new ArrayList<>(5);
    static {
        finishedScanStatuses.add("READY_CHANGES");
        finishedScanStatuses.add("READY_NO_CHANGES");
        finishedScanStatuses.add("DISCARDED");
        finishedScanStatuses.add("PARTIAL_ERROR");
        finishedScanStatuses.add("ERROR");
    }

    @Getter
    private static final List<String> successScanStatuses = new ArrayList<>(2);
    static {
        successScanStatuses.add("READY_CHANGES");
        successScanStatuses.add("READY_NO_CHANGES");
    }
}
