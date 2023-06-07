package com.csl.intercom.cslscan;

import com.csl.core.CSLContext;
import com.ucsl.json.JsonUtil;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScanConstants {
    public static final Map<String, String> connectionInfoFields = new HashMap<>() {{
        put("queryProtocol", "queryProtocol");
        put("community", "community");
        put("user", "user");
        put("pass", "pass");
        put("privPassPhrase", "privPassPhrase");
        put("securityLevel", "securityLevel");
        put("authProtocolName", "authProtocolName");
        put("privProtocolName", "privProtocolName");
    }};
    static final List<String> snmpv2cConnectionInfoFields = new ArrayList<>() {{
        add("queryProtocol");
        add("community");
    }};
    static final List<String> snmpv3ConnectionInfoFields = new ArrayList<>() {{
        add("queryProtocol");
        add("user");
        add("pass");
        add("privPassPhrase");
        add("securityLevel");
        add("authProtocolName");
        add("privProtocolName");
    }};
}
