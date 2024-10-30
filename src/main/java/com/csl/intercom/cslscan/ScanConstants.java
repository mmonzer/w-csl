package com.csl.intercom.cslscan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScanConstants {
    public static final Map<String, String> connectionInfoFields = new HashMap<>();
    static {
        connectionInfoFields.put("queryProtocol", "queryProtocol");
        connectionInfoFields.put("community", "community");
        connectionInfoFields.put("user", "user");
        connectionInfoFields.put("pass", "pass");
        connectionInfoFields.put("privPassPhrase", "privPassPhrase");
        connectionInfoFields.put("securityLevel", "securityLevel");
        connectionInfoFields.put("authProtocolName", "authProtocolName");
        connectionInfoFields.put("privProtocolName", "privProtocolName");
    }

    static final List<String> snmpv2cConnectionInfoFields = new ArrayList<>();
    static {
        snmpv2cConnectionInfoFields.add("queryProtocol");
        snmpv2cConnectionInfoFields.add("community");
    }

    static final List<String> snmpv3ConnectionInfoFields = new ArrayList<>();
    static {
        snmpv3ConnectionInfoFields.add("queryProtocol");
        snmpv3ConnectionInfoFields.add("user");
        snmpv3ConnectionInfoFields.add("pass");
        snmpv3ConnectionInfoFields.add("privPassPhrase");
        snmpv3ConnectionInfoFields.add("securityLevel");
        snmpv3ConnectionInfoFields.add("authProtocolName");
        snmpv3ConnectionInfoFields.add("privProtocolName");
    }

    public static final List<String> finishedScanStatuses = new ArrayList<>(5);
    static {
        finishedScanStatuses.add("READY_CHANGES");
        finishedScanStatuses.add("READY_NO_CHANGES");
        finishedScanStatuses.add("DISCARDED");
        finishedScanStatuses.add("PARTIAL_ERROR");
        finishedScanStatuses.add("ERROR");
    };

    public static final List<String> successScanStatuses = new ArrayList<>(2);
    static {
        successScanStatuses.add("READY_CHANGES");
        successScanStatuses.add("READY_NO_CHANGES");
    };
}
