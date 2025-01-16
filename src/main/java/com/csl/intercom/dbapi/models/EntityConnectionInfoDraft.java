package com.csl.intercom.dbapi.models;

import com.csl.logger.CSLApplicativeLogger;
import lombok.Getter;
import lombok.Setter;
import com.ucsl.json.Json;

import java.time.OffsetDateTime;
import java.util.UUID;

public class EntityConnectionInfoDraft {
    public static final String USERNAME_STR = "username";
    public static final String PASSWORD_STR = "password";
    public static final String SNMP_COMMUNITY = "snmpCommunity";
    public static final String SNMP_PRIVACY_KEY = "snmpPrivacyKey";
    public static final String SNMP_AUTHENTICATION_ALGORITHM = "snmpAuthenticationAlgorithm";
    public static final String SNMP_PRIVACY_ALGORITHM = "snmpPrivacyAlgorithm";
    public static final String SSH_KEY = "sshKey";
    public static final String INPUTS_STR = "inputs";
    public static final String DISCOVERY_PROTOCOL_NAME_RELATED_TO_HTTP_CNX = "discoveryProtocolNameRelatedToHttpCnx";
    public static final String PROTOCOL_STR = "protocol";
    public static final String OTHER_DATA_DRAFT = "other_data_draft";
    public static final String IS_SIMULATED = "isSimulated";
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
    @Getter @Setter
    boolean isSimulated = false;

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
    private static final CSLApplicativeLogger logger = CSLApplicativeLogger.getLogger(EntityConnectionInfoDraft.class);

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
    public EntityConnectionInfoDraft(String uuid, String port, String name, String userName, String password, String discoveryProtocol, String snmpCommunity, String snmpPrivacyKey, String snmpAuthenticationAlgorithm, String snmpPrivacyAlgorithm, String sshKey, boolean isSimulated ,boolean isKeepPassword, boolean isKeepSshKey, boolean isKeepPassphrase, boolean isKeepSnmpPrivacyKey) {
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
        this.isSimulated = isSimulated;
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
                PROTOCOL_STR, this.protocol,
                "port", this.port,
                "name", this.name,
                USERNAME_STR, this.username,
                PASSWORD_STR, this.password,
                SNMP_COMMUNITY, this.snmpCommunity,
                SNMP_PRIVACY_KEY, this.snmpPrivacyKey,
                SNMP_AUTHENTICATION_ALGORITHM, this.snmpAuthenticationAlgorithm,
                SNMP_PRIVACY_ALGORITHM, this.snmpPrivacyAlgorithm,
                SSH_KEY, this.sshKey,
                IS_SIMULATED, this.isSimulated,
                INPUTS_STR, this.inputs,
                DISCOVERY_PROTOCOL_NAME_RELATED_TO_HTTP_CNX, this.discoveryProtocolNameRelatedToHttpCnx,
                "isKeepPassword", this.isKeepPassword,
                "isKeepSshKey", this.isKeepSshKey,
                "isKeepPassphrase", this.isKeepPassphrase,
                "isKeepSnmpPrivacyKey", this.isKeepSnmpPrivacyKey
        );
    }

    public Json serializeForDbapi() {
        return Json.object(
                "uuid", this.uuid,
                PROTOCOL_STR, this.protocol,
                "port", this.port,
                "name", this.name,
                USERNAME_STR, this.username,
                PASSWORD_STR, this.password,
                SNMP_COMMUNITY, this.snmpCommunity,
                SNMP_PRIVACY_KEY, this.snmpPrivacyKey,
                SNMP_AUTHENTICATION_ALGORITHM, this.snmpAuthenticationAlgorithm,
                SNMP_PRIVACY_ALGORITHM, this.snmpPrivacyAlgorithm,
                SSH_KEY, this.sshKey,
                IS_SIMULATED, this.isSimulated,
                INPUTS_STR, this.inputs,
                DISCOVERY_PROTOCOL_NAME_RELATED_TO_HTTP_CNX, this.discoveryProtocolNameRelatedToHttpCnx
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
        if ( connectionDraftJson.get(OTHER_DATA_DRAFT) !=null && connectionDraftJson.get(OTHER_DATA_DRAFT).has("snmp_community"))
            snmpCommunity = connectionDraftJson.get(OTHER_DATA_DRAFT).get("snmp_community").asString();

        String snmpPrivacyKey = "";
        if ( connectionDraftJson.get(OTHER_DATA_DRAFT) !=null && connectionDraftJson.get(OTHER_DATA_DRAFT).has("snmp_privacy_key"))
            snmpPrivacyKey = connectionDraftJson.get(OTHER_DATA_DRAFT).get("snmp_privacy_key").asString();

        String snmpAuthenticationAlgorithm = "";
        if ( connectionDraftJson.get(OTHER_DATA_DRAFT) !=null && connectionDraftJson.get(OTHER_DATA_DRAFT).has("snmp_authentication_algorithm"))
            snmpAuthenticationAlgorithm = connectionDraftJson.get(OTHER_DATA_DRAFT).get("snmp_authentication_algorithm").asString();

        String snmpPrivacyAlgorithm = "";
        if ( connectionDraftJson.get(OTHER_DATA_DRAFT) !=null && connectionDraftJson.get(OTHER_DATA_DRAFT).has("snmp_privacy_algorithm"))
            snmpPrivacyAlgorithm = connectionDraftJson.get(OTHER_DATA_DRAFT).get("snmp_privacy_algorithm").asString();

        String sshKey = "";
        if ( connectionDraftJson.get(OTHER_DATA_DRAFT) !=null && connectionDraftJson.get(OTHER_DATA_DRAFT).has("ssh_key"))
            sshKey = connectionDraftJson.get(OTHER_DATA_DRAFT).get("ssh_key").asString();

        boolean isSimulated = false;
        if (connectionDraftJson.has(IS_SIMULATED))
            isSimulated = connectionDraftJson.get(IS_SIMULATED).asBoolean();

        return new EntityConnectionInfoDraft(
                connectionDraftJson.get("mongo_entity_id").asString(),
                connectionDraftJson.get("port_number").asString(),
                connectionDraftJson.get("name").asString(),
                connectionDraftJson.get(USERNAME_STR).asString(),
                connectionDraftJson.get(PASSWORD_STR)==null ? "" : connectionDraftJson.get(PASSWORD_STR).asString(),
                connectionDraftJson.get("discovery_protocol_draft").asString(),
                snmpCommunity,
                snmpPrivacyKey,
                snmpAuthenticationAlgorithm,
                snmpPrivacyAlgorithm,
                sshKey,
                isSimulated,
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
        if (connectionDraftJson.get(PROTOCOL_STR) != null) {
            protocol = connectionDraftJson.get(PROTOCOL_STR).asString();
        }
        entityConnectionInfoDraft.setProtocol(protocol);

        String port = null;
        if (connectionDraftJson.get("port") != null) {
            port = connectionDraftJson.get("port").asString();
        }
        entityConnectionInfoDraft.setPort(port);

        String username = null;
        if (connectionDraftJson.get(USERNAME_STR) != null) {
            username = connectionDraftJson.get(USERNAME_STR).asString();
        }
        entityConnectionInfoDraft.setUsername(username);

        String password = null;
        if (connectionDraftJson.get(PASSWORD_STR) != null) {
            password = connectionDraftJson.get(PASSWORD_STR).asString();
        }
        entityConnectionInfoDraft.setPassword(password);

        String snmpCommunity = null;
        if (connectionDraftJson.get(SNMP_COMMUNITY) != null) {
            snmpCommunity = connectionDraftJson.get(SNMP_COMMUNITY).asString();
        }
        entityConnectionInfoDraft.setSnmpCommunity(snmpCommunity);

        String snmpPrivacyKey = null;
        if (connectionDraftJson.get(SNMP_PRIVACY_KEY) != null) {
            snmpPrivacyKey = connectionDraftJson.get(SNMP_PRIVACY_KEY).asString();
        }
        entityConnectionInfoDraft.setSnmpPrivacyKey(snmpPrivacyKey);

        String snmpAuthenticationAlgorithm = null;
        if (connectionDraftJson.get(SNMP_AUTHENTICATION_ALGORITHM) != null) {
            snmpAuthenticationAlgorithm = connectionDraftJson.get(SNMP_AUTHENTICATION_ALGORITHM).asString();
        }
        entityConnectionInfoDraft.setSnmpAuthenticationAlgorithm(snmpAuthenticationAlgorithm);

        String snmpPrivacyAlgorithm = null;
        if (connectionDraftJson.get(SNMP_PRIVACY_ALGORITHM) != null) {
            snmpPrivacyAlgorithm = connectionDraftJson.get(SNMP_PRIVACY_ALGORITHM).asString();
        }
        entityConnectionInfoDraft.setSnmpPrivacyAlgorithm(snmpPrivacyAlgorithm);

        String sshKey = null;
        if (connectionDraftJson.get(SSH_KEY) != null) {
            sshKey = connectionDraftJson.get(SSH_KEY).asString();
        }
        entityConnectionInfoDraft.setSshKey(sshKey);
        boolean isSimulated = false;
        if (connectionDraftJson.get(IS_SIMULATED) != null) {
            isSimulated = connectionDraftJson.get(IS_SIMULATED).asBoolean();
        }
        entityConnectionInfoDraft.setSimulated(isSimulated);
        // manage inputs
        Json cnxInputFotCnxRelatedToHttp = Json.object();
        if (connectionDraftJson.get(INPUTS_STR) != null) {
            cnxInputFotCnxRelatedToHttp = connectionDraftJson.get(INPUTS_STR);
        }
        entityConnectionInfoDraft.setInputs(cnxInputFotCnxRelatedToHttp);
        String discoveryProtocolNameRelatedToHttpCnx = null;
        if (connectionDraftJson.get(DISCOVERY_PROTOCOL_NAME_RELATED_TO_HTTP_CNX) != null) {
            discoveryProtocolNameRelatedToHttpCnx = connectionDraftJson.get(DISCOVERY_PROTOCOL_NAME_RELATED_TO_HTTP_CNX).asString();
        }
        entityConnectionInfoDraft.setDiscoveryProtocolNameRelatedToHttpCnx(discoveryProtocolNameRelatedToHttpCnx);

        return entityConnectionInfoDraft;
    }
}