package com.csl.intercom.cslscan.models;

import com.csl.interfaces.models.IDbapiSerializable;
import com.ucsl.json.Json;

import java.util.List;
import java.util.Objects;

import static com.ucsl.json.JsonUtil.getValueBooleanOrNull;
import static com.ucsl.json.JsonUtil.getValueStringOrNull;

public class ExternalConnectionInfoTemplate implements IDbapiSerializable {
    public static final String KEY = "key";
    public static final String NAME_EN = "name_en";
    public static final String NAME_FR = "name_fr";
    public static final String REQUIRED = "required";
    public static final String SECRET = "secret";
    public static final String TYPE = "type";
    public static final String QUERY_PROTOCOL = "queryProtocol";
    public static final String QUERY_PROTOCOL_ID = "queryProtocolId";
    public static final String NAME = "name";
    public static final String FIELDS = "fields";
    private String connectionName;
    private String queryProtocol;
    private int queryProtocolId;
    private List<Field> connectionFields;

    public static ExternalConnectionInfoTemplate fromScannerJson(Json json) {
        ExternalConnectionInfoTemplate template = new ExternalConnectionInfoTemplate();
        if (json.has(NAME) && json.get(NAME).isString()) {
            template.connectionName = json.get(NAME).asString();
        } else {
            return null;
        }

        if (json.has(QUERY_PROTOCOL) && json.get(QUERY_PROTOCOL).isString()) {
            template.queryProtocol = json.get(QUERY_PROTOCOL).asString();
        } else {
            return null;
        }

        if (json.has(QUERY_PROTOCOL_ID) && json.get(QUERY_PROTOCOL_ID).isNumber()) {
            template.queryProtocolId = json.get(QUERY_PROTOCOL_ID).asInteger();
        } else {
            return null;
        }

        if (json.has(FIELDS) && json.get(FIELDS).isArray()) {
            template.connectionFields = json.get(FIELDS).asJsonList().stream()
                    .map(Field::fromScannerJson)
                    .filter(Objects::nonNull)
                    .toList();
        } else {
            return null;
        }

        return template;
    }

    @Override
    public Json serializeForDbapi() {
        Json fieldsJson = Json.array(this.connectionFields.stream().map(Field::serializeForDbapi).toArray());
        return Json.object(
                NAME, connectionName,
                QUERY_PROTOCOL, queryProtocol,
                QUERY_PROTOCOL_ID, queryProtocolId,
                FIELDS, fieldsJson
        );
    }

    public static class Field implements IDbapiSerializable {
        private final String nameEn;
        private String nameFr;
        String key;
        private boolean isRequired;
        private boolean isSecret;
        private Type type;

        public Field(String nameEn, String nameFr, String key, boolean isRequired, boolean isSecret, Type type) {
            this.nameEn = nameEn;
            this.nameFr = nameFr;
            this.key = key;
            this.isRequired = isRequired;
            this.isSecret = isSecret;
            this.type = type;
        }

        public static Field fromScannerJson(Json json) {
            String nameFr = getValueStringOrNull(json, NAME_FR);
            String nameEn = getValueStringOrNull(json, NAME_EN);
            String key = getValueStringOrNull(json, KEY);
            Boolean isRequired = getValueBooleanOrNull(json, REQUIRED);
            Boolean isSecret = getValueBooleanOrNull(json, SECRET);
            String type = getValueStringOrNull(json, TYPE);

            if (nameFr == null || nameEn == null || key == null || isRequired == null || isSecret == null || type == null) {
                return null;
            }

            return new Field(nameEn, nameFr, key, isRequired, isSecret, Type.valueOf(type));
        }

        @Override
        public Json serializeForDbapi() {
            return Json.object(
                    KEY, key,
                    NAME_EN, nameEn,
                    NAME_FR, nameFr,
                    REQUIRED, isRequired,
                    SECRET, isSecret,
                    TYPE, type.toString()
            );
        }

        public Type getType() {
            return type;
        }
    }

    public enum Type {
        ANY("ANY"),
        STRING("STRING"),
        INTEGER("INTEGER"),
        FLOAT("FLOAT"),
        BOOLEAN("BOOLEAN"),
        ;

        private final String name;

        Type(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }
}
