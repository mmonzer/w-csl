package com.csl.intercom.dbapi.models;

import com.csl.intercom.dbapi.enums.SNMPv3AuhtenticationAlgorithm;
import com.csl.intercom.dbapi.enums.SNMPv3ConnectionField;
import com.csl.intercom.dbapi.enums.SNMPv3PrivacyAlgorithm;
import com.csl.intercom.dbapi.enums.StaticConnectionProtocol;
import com.ucsl.json.Json;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

import static com.ucsl.json.JsonUtil.*;

/**
 * Model to represent a SNMPv3 connection.
 */
public class SNMPv3Connection extends Connection {
    @Getter
    private final int port;
    @Getter
    private final String username;
    @Getter
    private final String password;
    @Getter
    private final String passphrase;
    @Getter
    private final SNMPv3AuhtenticationAlgorithm authenticationAlgorithm;
    @Getter
    private final SNMPv3PrivacyAlgorithm privacyAlgorithm;
    private final String securityLevel;
    private Boolean isKeepPassword;
    private Boolean isKeepSnmpPrivacyKey;


    protected SNMPv3Connection(String id, int port, List<String> devices, String username, String password, String passphrase, SNMPv3AuhtenticationAlgorithm authenticationAlgorithm, SNMPv3PrivacyAlgorithm privacyAlgorithm, Boolean isSimulated) {
        this(null, id, port, devices, username, password, passphrase, authenticationAlgorithm, privacyAlgorithm, isSimulated);
    }
    protected SNMPv3Connection(String name, String id, int port, List<String> devices, String username, String password, String passphrase, SNMPv3AuhtenticationAlgorithm authenticationAlgorithm, SNMPv3PrivacyAlgorithm privacyAlgorithm, Boolean isSimulated) {
        this(name, id, port, devices, username, password, passphrase, authenticationAlgorithm, privacyAlgorithm, isSimulated, null, null);
    }

    protected SNMPv3Connection(String name, String id, int port, List<String> devices, String username, String password, String passphrase, SNMPv3AuhtenticationAlgorithm authenticationAlgorithm, SNMPv3PrivacyAlgorithm privacyAlgorithm, Boolean isSimulated, Boolean isKeepPassword, Boolean isKeepSnmpPrivacyKey) {
        super(name, id, devices, StaticConnectionProtocol.SNMP_V3);
        this.port = port;
        this.username = username;
        this.password = password;
        this.passphrase = passphrase;
        this.authenticationAlgorithm = authenticationAlgorithm;
        this.privacyAlgorithm = privacyAlgorithm;

        String authString = this.authenticationAlgorithm == null ? "noAuth" : "auth";
        String privString = this.privacyAlgorithm == null ? "NoPriv" : "Priv";
        this.securityLevel = authString + privString;

        this.isKeepPassword = isKeepPassword;
        this.isKeepSnmpPrivacyKey = isKeepSnmpPrivacyKey;
    }

    /**
     * Parse the JSON serialization received from DB-API.
     *
     * @param connectionJson The serialized connection as handed by DB-API.
     * @return An instance of {@link SNMPv3Connection} if the parsing was successful, null otherwise.
     */
    public static SNMPv3Connection fromJson(Json connectionJson) {
        try {
            String uuid;
            if (connectionJson.has("uuid")) {
                uuid = connectionJson.get("uuid").asString();
            } else {
                uuid = getValueStringOrNull(connectionJson, "mongo_entity_id");
            }
            int port = connectionJson.get(SNMPv3ConnectionField.PORT.dbapiName()).asInteger();
            String username = connectionJson.get(SNMPv3ConnectionField.USERNAME.dbapiName()).asString();
            String password = getValueStringOrNull(connectionJson, SNMPv3ConnectionField.PASSWORD.dbapiName());
            Json otherData = connectionJson.get("other_data");
            Json readOnlyOtherData = connectionJson.get("read_only_other_data");

            String passphrase = readValueStringFrom(otherData, readOnlyOtherData, SNMPv3ConnectionField.PASSPHRASE.dbapiName());
            SNMPv3AuhtenticationAlgorithm authenticationAlgo = SNMPv3AuhtenticationAlgorithm.fromDbapiName(readValueStringFrom(otherData, readOnlyOtherData, SNMPv3ConnectionField.AUTHENTICATION_ALGORITHM.dbapiName()));
            SNMPv3PrivacyAlgorithm privacyAlgo = SNMPv3PrivacyAlgorithm.fromDbapiName(readValueStringFrom(otherData, readOnlyOtherData, SNMPv3ConnectionField.PRIVACY_ALGORITHM.dbapiName()));

            List<String> devices = new ArrayList<>();
            for (Json device : connectionJson.get("connected_devices").asJsonList()) {
                devices.add(device.asString());
            }

            boolean isSimulated = getValueBooleanOrDefault(connectionJson, "is_simulated", false);

            String name = connectionJson.get("name").asString();

            // check if is_keep_password is present in the json
            boolean isKeepPassword = getValueBooleanOrDefault(connectionJson,SNMPv3ConnectionField.IS_KEEP_PASSWORD.dbapiName(), false);
            boolean isKeepSnmpPrivacyKey = getValueBooleanOrDefault(connectionJson,SNMPv3ConnectionField.IS_KEEP_SNMP_PRIVACY_KEY.dbapiName(), false);

            return new SNMPv3Connection(name, uuid, port, devices, username, password, passphrase, authenticationAlgo, privacyAlgo, isSimulated, isKeepPassword, isKeepSnmpPrivacyKey);
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Reads the value of the given key, in the first json object, or , if not found in the second object. null if not found.
     * @param data1 first object to check
     * @param data2 second object to check
     * @param key key to find
     * @return the value in the first object. if not found, try the second one. Otherwise, null.
     */
    private static String readValueStringFrom(Json data1, Json data2, String key) {
        String value = null;
        if (data1 != null && data1.has(key)) {
            value = data1.get(key).asString();
        } else if (data2 != null && data2.has(key)) {
            value = data2.get(key).asString();
        }
        return value;
    }

    public static SNMPv3Connection fromScannerJson(Json connectionJson) {
        try {
            String uuid = null;
            if (connectionJson.has("uuid")) {
                uuid = connectionJson.get("uuid").asString();
            }
            int port = connectionJson.get(SNMPv3ConnectionField.PORT.scanName()).asInteger();
            String username = connectionJson.get(SNMPv3ConnectionField.USERNAME.scanName()).asString();
            String password = connectionJson.get(SNMPv3ConnectionField.PASSWORD.scanName()).asString();
            String passphrase = connectionJson.get(SNMPv3ConnectionField.PASSPHRASE.scanName()).asString();
            SNMPv3AuhtenticationAlgorithm authenticationAlgo = SNMPv3AuhtenticationAlgorithm.fromScanName(connectionJson.get(SNMPv3ConnectionField.AUTHENTICATION_ALGORITHM.scanName()).asString());
            SNMPv3PrivacyAlgorithm privacyAlgo = SNMPv3PrivacyAlgorithm.fromScanName(connectionJson.get(SNMPv3ConnectionField.PRIVACY_ALGORITHM.scanName()).asString());
            String name = connectionJson.get("name").asString();
            return new SNMPv3Connection(name, uuid, port, null, username, password, passphrase, authenticationAlgo, privacyAlgo, false);
        } catch (Throwable e) {
            return null;
        }
    }

    @Override
    public Json serializeForScanner() {
        Json result = super.serializeForScanner();
        result.set("user", this.username);
        result.set("pass", this.password);
        result.set("privPassPhrase", this.passphrase);
        result.set("securityLevel", this.securityLevel);
        result.set("authProtocolName", this.authenticationAlgorithm.scanName());
        result.set("privProtocolName", this.privacyAlgorithm.scanName());
        result.set("port", this.port);
        result.set("isKeepPassword", this.isKeepPassword);
        result.set("isKeepSnmpPrivacyKey", this.isKeepSnmpPrivacyKey);

        return result;
    }

}
