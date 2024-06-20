package com.csl.intercom.cslscan.models;

import com.csl.interfaces.models.IDbapiSerializable;
import com.ucsl.json.Json;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ExternalConnectionInfoTemplate implements IDbapiSerializable {
    private String name;
    private String queryProtocol;
    private int queryProtocolId;
    private List<Field> fields;

    public static ExternalConnectionInfoTemplate fromScannerJson(Json json) {
        ExternalConnectionInfoTemplate template = new ExternalConnectionInfoTemplate();
        if (json.has("name") && json.get("name").isString()) {
            template.name = json.get("name").asString();
        } else {
            return null;
        }

        if (json.has("queryProtocol") && json.get("queryProtocol").isString()) {
            template.queryProtocol = json.get("queryProtocol").asString();
        } else {
            return null;
        }

        if (json.has("queryProtocolId") && json.get("queryProtocolId").isNumber()) {
            template.queryProtocolId = json.get("queryProtocolId").asInteger();
        } else {
            return null;
        }

        if (json.has("fields") && json.get("fields").isArray()) {
            template.fields = json.get("fields").asJsonList().stream()
                    .map(Field::fromScannerJson)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } else {
            return null;
        }

        return template;
    }

    @Override
    public Json serializeForDbapi() {
        Json fieldsJson = Json.array(this.fields.stream().map(Field::serializeForDbapi).toArray());
        return Json.object(
                "name", name,
                "queryProtocol", queryProtocol,
                "queryProtocolId", queryProtocolId,
                "fields", fieldsJson
        );
    }

    public static class Field implements IDbapiSerializable {
        private String name_en;
        private String name_fr;
        String key;
        private boolean isRequired;
        private boolean isSecret;
        private Type type;

        public Field(String name_en, String name_fr, String key, boolean isRequired, boolean isSecret, Type type) {
            this.name_en = name_en;
            this.name_fr = name_fr;
            this.key = key;
            this.isRequired = isRequired;
            this.isSecret = isSecret;
            this.type = type;
        }

        public static Field fromScannerJson(Json json) {
            String name_fr;
            if (json.has("name_fr") && json.get("name_fr").isString()) {
                name_fr = json.get("name_fr").asString();
            } else {
                return null;
            }

            String name_en;
            if (json.has("name_en") && json.get("name_en").isString()) {
                name_en = json.get("name_en").asString();
            } else {
                return null;
            }

            String key;
            if (json.has("key") && json.get("key").isString()) {
                key = json.get("key").asString();
            } else {
                return null;
            }

            boolean isRequired;
            if (json.has("required") && json.get("required").isBoolean()) {
                isRequired = json.get("required").asBoolean();
            } else {
                return null;
            }

            boolean isSecret;
            if (json.has("secret") && json.get("secret").isBoolean()) {
                isSecret = json.get("secret").asBoolean();
            } else {
                return null;
            }

            Type type;
            if (json.has("type") && json.get("type").isString()) {
                type = Type.valueOf(json.get("type").asString());
            } else {
                return null;
            }

            return new Field(name_en, name_fr, key, isRequired, isSecret, type);
        }

        @Override
        public Json serializeForDbapi() {
            return Json.object(
                "name_en", name_en,
                "name_fr", name_fr,
                "required", isRequired,
                "secret", isSecret,
                "type", type.toString()
            );
        }

        public String getName_en() {
            return name_en;
        }

        public String getName_fr() {
            return name_fr;
        }

        public boolean isRequired() {
            return isRequired;
        }

        public boolean isSecret() {
            return isSecret;
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
