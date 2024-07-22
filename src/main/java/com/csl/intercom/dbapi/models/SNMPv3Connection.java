package com.csl.intercom.dbapi.models;

import com.csl.intercom.dbapi.enums.SNMPv3AuhtenticationAlgorithm;
import com.csl.intercom.dbapi.enums.SNMPv3ConnectionField;
import com.csl.intercom.dbapi.enums.SNMPv3PrivacyAlgorithm;
import com.csl.intercom.dbapi.enums.StaticConnectionProtocol;
import com.ucsl.json.Json;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Model to represent a SNMPv3 connection.
 */
public class SNMPv3Connection extends Connection {
    private final int port;
    @Getter
    private final String username;
    @Getter
    private final String password;
    @Getter
    private final String passphrase;
    private final SNMPv3AuhtenticationAlgorithm authenticationAlgorithm;
    private final SNMPv3PrivacyAlgorithm privacyAlgorithm;
    private final String securityLevel;

    protected SNMPv3Connection(String id, int port, List<String> devices, String username, String password, String passphrase, SNMPv3AuhtenticationAlgorithm authenticationAlgorithm, SNMPv3PrivacyAlgorithm privacyAlgorithm, Boolean isSimulated) {
        super(id, devices, StaticConnectionProtocol.SNMPv3, isSimulated);
        this.port = port;
        this.username = username;
        this.password = password;
        this.passphrase = passphrase;
        this.authenticationAlgorithm = authenticationAlgorithm;
        this.privacyAlgorithm = privacyAlgorithm;

        String authString = this.authenticationAlgorithm == null ? "noAuth" : "auth";
        String privString = this.privacyAlgorithm == null ? "NoPriv" : "Priv";
        this.securityLevel = authString + privString;
    }

    /**
     * Parse the JSON serialization received from DB-API.
     *
     * @param connectionJson The serialized connection as handed by DB-API.
     * @return An instance of {@link SNMPv3Connection} if the parsing was successful, null otherwise.
     */
    public static SNMPv3Connection fromJson(Json connectionJson) {
        try {
            String id = String.valueOf(connectionJson.get("id").asInteger());
            int port = connectionJson.get(SNMPv3ConnectionField.PORT.dbapiName()).asInteger();
            String username = connectionJson.get(SNMPv3ConnectionField.USERNAME.dbapiName()).asString();
            String password = null;
            if (connectionJson.has(SNMPv3ConnectionField.PASSWORD.dbapiName())) {
                password = connectionJson.get(SNMPv3ConnectionField.PASSWORD.dbapiName()).asString();
            }
            Json otherData = connectionJson.get("read_only_other_data");
            String passphrase = null;
            if (otherData.has(SNMPv3ConnectionField.PASSPHRASE.dbapiName())) {
                passphrase = otherData.get(SNMPv3ConnectionField.PASSPHRASE.dbapiName()).asString();
            }
            SNMPv3AuhtenticationAlgorithm authenticationAlgo = SNMPv3AuhtenticationAlgorithm.fromDbapiName(otherData.get(SNMPv3ConnectionField.AUTHENTICATION_ALGORITHM.dbapiName()).asString());
            SNMPv3PrivacyAlgorithm privacyAlgo = SNMPv3PrivacyAlgorithm.fromDbapiName(otherData.get(SNMPv3ConnectionField.PRIVACY_ALGORITHM.dbapiName()).asString());

            List<String> devices = new ArrayList<>();
            for (Json device : connectionJson.get("connected_devices").asJsonList()) {
                devices.add(device.asString());
            }
            Boolean isSimulated = connectionJson.get("is_simulated").asBoolean();

            return new SNMPv3Connection(id, port, devices, username, password, passphrase, authenticationAlgo, privacyAlgo, isSimulated);
        } catch (Throwable e) {
            return null;
        }
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

            return new SNMPv3Connection(uuid, port, null, username, password, passphrase, authenticationAlgo, privacyAlgo, false);
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

        return result;
    }

}
