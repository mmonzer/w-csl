package com.csl.intercom.dbapi.models;

import com.csl.intercom.dbapi.enums.*;
import com.ucsl.json.Json;

import java.util.ArrayList;
import java.util.List;

/**
 * Model to represent a SNMPv3 connection.
 */
public class SNMPv3Connection extends Connection {
    private int port;
    private String username;
    private String password;
    private String passphrase;
    private SNMPv3AuhtenticationAlgorithm authenticationAlgorithm;
    private SNMPv3PrivacyAlgorithm privacyAlgorithm;
    private String securityLevel;

    protected SNMPv3Connection(int id, int port, List<String> devices, String username, String password, String passphrase, SNMPv3AuhtenticationAlgorithm authenticationAlgorithm, SNMPv3PrivacyAlgorithm privacyAlgorithm) {
        super(id, devices, ConnectionProtocol.SNMPv3);
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
            int id = connectionJson.get("id").asInteger();
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
            for (Json device: connectionJson.get("connected_devices").asJsonList()) {
                devices.add(device.asString());
            }

            return new SNMPv3Connection(id, port, devices, username, password, passphrase, authenticationAlgo, privacyAlgo);
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
