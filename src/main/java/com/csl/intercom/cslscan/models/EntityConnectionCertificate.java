package com.csl.intercom.cslscan.models;

import com.csl.intercom.cslscan.enums.EntityConnectionCertificateField;
import com.csl.interfaces.models.IDbapiSerializable;
import com.csl.interfaces.models.IScannerSerializable;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityConnectionCertificate implements IScannerSerializable, IDbapiSerializable {
    private static final Logger logger = LoggerFactory.getLogger(EntityConnectionCertificate.class);
    @Setter
    @Getter
    private String uuid;
    private String content;
    private String certificateFileName;
    private String updatedAt;

    public static EntityConnectionCertificate fromDbapiJson(Json json) {
        EntityConnectionCertificate entityConnectionCertificate = new EntityConnectionCertificate();
        try {
            entityConnectionCertificate.uuid = JsonUtil.getStringFromJson(json, EntityConnectionCertificateField.UUID.dbapiName(), null);
            entityConnectionCertificate.content = JsonUtil.getStringFromJson(json, EntityConnectionCertificateField.CONTENT.dbapiName(), null);
            entityConnectionCertificate.certificateFileName = JsonUtil.getStringFromJson(json, EntityConnectionCertificateField.FILENAME.dbapiName(), null);

            return entityConnectionCertificate;
        } catch (Throwable e) {
            logger.warn("Failed to parse EntityCertificateConnection from dbapi json", e);
            return null;
        }
    }

    public static EntityConnectionCertificate fromScannerJson(Json json) {
        EntityConnectionCertificate entityConnectionCertificate = new EntityConnectionCertificate();
        try {
            entityConnectionCertificate.uuid = json.get(EntityConnectionCertificateField.UUID.scanName()).asString();
            entityConnectionCertificate.content = json.get(EntityConnectionCertificateField.CONTENT.scanName()).asString();
            entityConnectionCertificate.certificateFileName = json.get(EntityConnectionCertificateField.FILENAME.scanName()).asString();

            return entityConnectionCertificate;
        } catch (Throwable e) {
            logger.warn("Failed to parse EntityHttpConnection from scanner json", e);
            return null;
        }
    }

    public Json serializeForScanner() {
        return Json.object(
                EntityConnectionCertificateField.UUID.scanName(), uuid,
                EntityConnectionCertificateField.CONTENT.scanName(), content,
                EntityConnectionCertificateField.FILENAME.scanName(), certificateFileName,
                EntityConnectionCertificateField.UPDATED_AT.scanName(), updatedAt
        );
    }

    public Json serializeForDbapi() {
        return Json.object(
                EntityConnectionCertificateField.UUID.dbapiName(), uuid,
                EntityConnectionCertificateField.CONTENT.dbapiName(), content,
                EntityConnectionCertificateField.FILENAME.dbapiName(), certificateFileName,
                EntityConnectionCertificateField.UPDATED_AT.dbapiName(), updatedAt
        );
    }
}