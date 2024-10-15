package com.csl.intercom.dbapi.models;

import com.csl.logger.CustomLogger;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import com.ucsl.json.Json;
import main.services.DiscoveryServices;

import java.time.OffsetDateTime;
import java.util.UUID;

public class EntityConnectionInfoDraft {
    @Getter
    @Setter
    private String uuid;
    @Getter
    @Setter
    String port;
    @Getter @Setter
    String name;
    @Getter @Setter
    String username;
    @Getter @Setter
    String password;
    @Getter @Setter
    String protocol;
    @Getter @Setter
    String snmpCommunity;
    @Getter @Setter
    String snmpPrivacyKey;
    @Getter @Setter
    String snmpAuthenticationAlgorithm;
    @Getter @Setter
    String snmpPrivacyAlgorithm;
    @Getter @Setter
    String sshKey;

    boolean isKeepPassword;
    boolean isKeepSshKey;
    boolean isKeepPassphrase;
    boolean isKeepSnmpPrivacyKey;

    // region for cnx inputs
    @Getter @Setter
    Json inputs; // for cnx inputs related to http cnx
    @Getter @Setter
    String discoveryProtocolNameRelatedToHttpCnx;

    @Getter @Setter
    OffsetDateTime createdAt;
    @Getter @Setter
    OffsetDateTime updatedAt;
    private static final CustomLogger logger = CustomLogger.getLogger(DiscoveryServices.class);

    public EntityConnectionInfoDraft(String uuid, String port, String name, String userName, String password, String discoveryProtocol) {
        this.uuid = uuid;
        this.port = port;
        this.name = name;
        this.username = userName;
        this.password = password;
        this.protocol = discoveryProtocol;
    }

    public EntityConnectionInfoDraft(String port, String name, String userName, String password, String discoveryProtocol, String snmpCommunity, String snmpPrivacyKey, String snmpAuthenticationAlgorithm, String snmpPrivacyAlgorithm, String sshKey) {
        this.port = port;
        this.name = name;
        this.username = userName;
        this.password = password;
        this.protocol = discoveryProtocol;
        this.snmpCommunity = snmpCommunity;
        this.snmpPrivacyKey = snmpPrivacyKey;
        this.snmpAuthenticationAlgorithm = snmpAuthenticationAlgorithm;
        this.snmpPrivacyAlgorithm = snmpPrivacyAlgorithm;
        this.sshKey = sshKey;
    }

    public EntityConnectionInfoDraft(String uuid, String port, String name, String userName, String password, String discoveryProtocol, String snmpCommunity, String snmpPrivacyKey, String snmpAuthenticationAlgorithm, String snmpPrivacyAlgorithm, String sshKey) {
        this.uuid = uuid;
        this.port = port;
        this.name = name;
        this.username = userName;
        this.password = password;
        this.protocol = discoveryProtocol;
        this.snmpCommunity = snmpCommunity;
        this.snmpPrivacyKey = snmpPrivacyKey;
        this.snmpAuthenticationAlgorithm = snmpAuthenticationAlgorithm;
        this.snmpPrivacyAlgorithm = snmpPrivacyAlgorithm;
        this.sshKey = sshKey;
    }
    public EntityConnectionInfoDraft(String uuid, String port, String name, String userName, String password, String discoveryProtocol, String snmpCommunity, String snmpPrivacyKey, String snmpAuthenticationAlgorithm, String snmpPrivacyAlgorithm, String sshKey, boolean isKeepPassword, boolean isKeepSshKey, boolean isKeepPassphrase, boolean isKeepSnmpPrivacyKey) {
        this.uuid = uuid;
        this.port = port;
        this.name = name;
        this.username = userName;
        this.password = password;
        this.protocol = discoveryProtocol;
        this.snmpCommunity = snmpCommunity;
        this.snmpPrivacyKey = snmpPrivacyKey;
        this.snmpAuthenticationAlgorithm = snmpAuthenticationAlgorithm;
        this.snmpPrivacyAlgorithm = snmpPrivacyAlgorithm;
        this.sshKey = sshKey;
        this.isKeepPassword = isKeepPassword;
        this.isKeepSshKey = isKeepSshKey;
        this.isKeepPassphrase = isKeepPassphrase;
        this.isKeepSnmpPrivacyKey = isKeepSnmpPrivacyKey;
    }
    public EntityConnectionInfoDraft() {

    }

    public Json serializeForScanner() {
        return Json.object(
                "uuid", this.uuid,
                "protocol", this.protocol,
                "port", this.port,
                "name", this.name,
                "username", this.username,
                "password", this.password,
                "snmpCommunity", this.snmpCommunity,
                "snmpPrivacyKey", this.snmpPrivacyKey,
                "snmpAuthenticationAlgorithm", this.snmpAuthenticationAlgorithm,
                "snmpPrivacyAlgorithm", this.snmpPrivacyAlgorithm,
                "sshKey", this.sshKey,
                "inputs", this.inputs,
                "discoveryProtocolNameRelatedToHttpCnx", this.discoveryProtocolNameRelatedToHttpCnx,
                "isKeepPassword", this.isKeepPassword,
                "isKeepSshKey", this.isKeepSshKey,
                "isKeepPassphrase", this.isKeepPassphrase,
                "isKeepSnmpPrivacyKey", this.isKeepSnmpPrivacyKey
        );
    }

    public Json serializeForDbapi() {
        return Json.object(
                "uuid", this.uuid,
                "protocol", this.protocol,
                "port", this.port,
                "name", this.name,
                "username", this.username,
                "password", this.password,
                "snmpCommunity", this.snmpCommunity,
                "snmpPrivacyKey", this.snmpPrivacyKey,
                "snmpAuthenticationAlgorithm", this.snmpAuthenticationAlgorithm,
                "snmpPrivacyAlgorithm", this.snmpPrivacyAlgorithm,
                "sshKey", this.sshKey,
                "inputs", this.inputs,
                "discoveryProtocolNameRelatedToHttpCnx", this.discoveryProtocolNameRelatedToHttpCnx
        );
    }
    public static EntityConnectionInfoDraft fromHMIJson(Json connectionDraftJson) {
        logger.info("*************fromHMIJson: connectionDraftJson:**********" + connectionDraftJson.toString());
        boolean is_keep_password = true;
        if (connectionDraftJson.has("is_keep_password"))
            is_keep_password = connectionDraftJson.get("is_keep_password").asBoolean();

        boolean is_keep_ssh_key =  true;
        if (connectionDraftJson.has("is_keep_ssh_key"))
            is_keep_ssh_key = connectionDraftJson.get("is_keep_ssh_key").asBoolean();

        boolean is_keep_passphrase = true;
        if (connectionDraftJson.has("is_keep_passphrase"))
            is_keep_passphrase = connectionDraftJson.get("is_keep_passphrase").asBoolean();

        boolean is_keep_snmp_privacy_key= true;
        if (connectionDraftJson.has("is_keep_snmp_privacy_key"))
            is_keep_snmp_privacy_key = connectionDraftJson.get("is_keep_snmp_privacy_key").asBoolean();

        String snmpCommunity = "";
        if ( connectionDraftJson.get("other_data_draft") !=null && connectionDraftJson.get("other_data_draft").has("snmp_community"))
            snmpCommunity = connectionDraftJson.get("other_data_draft").get("snmp_community").asString();

        String snmpPrivacyKey = "";
        if ( connectionDraftJson.get("other_data_draft") !=null && connectionDraftJson.get("other_data_draft").has("snmp_privacy_key"))
            snmpPrivacyKey = connectionDraftJson.get("other_data_draft").get("snmp_privacy_key").asString();

        String snmpAuthenticationAlgorithm = "";
        if ( connectionDraftJson.get("other_data_draft") !=null && connectionDraftJson.get("other_data_draft").has("snmp_authentication_algorithm"))
            snmpAuthenticationAlgorithm = connectionDraftJson.get("other_data_draft").get("snmp_authentication_algorithm").asString();

        String snmpPrivacyAlgorithm = "";
        if ( connectionDraftJson.get("other_data_draft") !=null && connectionDraftJson.get("other_data_draft").has("snmp_privacy_algorithm"))
            snmpPrivacyAlgorithm = connectionDraftJson.get("other_data_draft").get("snmp_privacy_algorithm").asString();

        String sshKey = "";
        if ( connectionDraftJson.get("other_data_draft") !=null && connectionDraftJson.get("other_data_draft").has("ssh_key"))
            sshKey = connectionDraftJson.get("other_data_draft").get("ssh_key").asString();

        return new EntityConnectionInfoDraft(
                connectionDraftJson.get("mongo_entity_id").asString(),
                connectionDraftJson.get("port_number").asString(),
                connectionDraftJson.get("name").asString(),
                connectionDraftJson.get("username").asString(),
                connectionDraftJson.get("password")==null ? "" : connectionDraftJson.get("password").asString(),
                connectionDraftJson.get("discovery_protocol_draft").asString(),
                snmpCommunity,
                snmpPrivacyKey,
                snmpAuthenticationAlgorithm,
                snmpPrivacyAlgorithm,
                sshKey,
                is_keep_password,
                is_keep_ssh_key,
                is_keep_passphrase,
                is_keep_snmp_privacy_key
        );
    }

    public static EntityConnectionInfoDraft fromHMIUploadingFile(Json connectionDraftJson) {
        EntityConnectionInfoDraft entityConnectionInfoDraft = new EntityConnectionInfoDraft();
        entityConnectionInfoDraft.setUuid(UUID.randomUUID().toString());

        String name = null;
        if (connectionDraftJson.get("name") != null) {
            name = connectionDraftJson.get("name").asString();
        }
        entityConnectionInfoDraft.setName(name);

        String protocol = null;
        if (connectionDraftJson.get("protocol") != null) {
            protocol = connectionDraftJson.get("protocol").asString();
        }
        entityConnectionInfoDraft.setProtocol(protocol);

        String port = null;
        if (connectionDraftJson.get("port") != null) {
            port = connectionDraftJson.get("port").asString();
        }
        entityConnectionInfoDraft.setPort(port);

        String username = null;
        if (connectionDraftJson.get("username") != null) {
            username = connectionDraftJson.get("username").asString();
        }
        entityConnectionInfoDraft.setUsername(username);

        String password = null;
        if (connectionDraftJson.get("password") != null) {
            password = connectionDraftJson.get("password").asString();
        }
        entityConnectionInfoDraft.setPassword(password);

        String snmpCommunity = null;
        if (connectionDraftJson.get("snmpCommunity") != null) {
            snmpCommunity = connectionDraftJson.get("snmpCommunity").asString();
        }
        entityConnectionInfoDraft.setSnmpCommunity(snmpCommunity);

        String snmpPrivacyKey = null;
        if (connectionDraftJson.get("snmpPrivacyKey") != null) {
            snmpPrivacyKey = connectionDraftJson.get("snmpPrivacyKey").asString();
        }
        entityConnectionInfoDraft.setSnmpPrivacyKey(snmpPrivacyKey);

        String snmpAuthenticationAlgorithm = null;
        if (connectionDraftJson.get("snmpAuthenticationAlgorithm") != null) {
            snmpAuthenticationAlgorithm = connectionDraftJson.get("snmpAuthenticationAlgorithm").asString();
        }
        entityConnectionInfoDraft.setSnmpAuthenticationAlgorithm(snmpAuthenticationAlgorithm);

        String snmpPrivacyAlgorithm = null;
        if (connectionDraftJson.get("snmpPrivacyAlgorithm") != null) {
            snmpPrivacyAlgorithm = connectionDraftJson.get("snmpPrivacyAlgorithm").asString();
        }
        entityConnectionInfoDraft.setSnmpPrivacyAlgorithm(snmpPrivacyAlgorithm);

        String sshKey = null;
        if (connectionDraftJson.get("sshKey") != null) {
            sshKey = connectionDraftJson.get("sshKey").asString();
        }
        entityConnectionInfoDraft.setSshKey(sshKey);

        // manage inputs
        Json cnxInputFotCnxRelatedToHttp = Json.object();
        if (connectionDraftJson.get("inputs") != null) {
            cnxInputFotCnxRelatedToHttp = connectionDraftJson.get("inputs");
        }
        entityConnectionInfoDraft.setInputs(cnxInputFotCnxRelatedToHttp);
        String discoveryProtocolNameRelatedToHttpCnx = null;
        if (connectionDraftJson.get("discoveryProtocolNameRelatedToHttpCnx") != null) {
            discoveryProtocolNameRelatedToHttpCnx = connectionDraftJson.get("discoveryProtocolNameRelatedToHttpCnx").asString();
        }
        entityConnectionInfoDraft.setDiscoveryProtocolNameRelatedToHttpCnx(discoveryProtocolNameRelatedToHttpCnx);

        return entityConnectionInfoDraft;
    }
}