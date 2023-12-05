package com.csl.intercom.cslscan.models;

import com.csl.intercom.cslscan.enums.EntityHttpConnectionInputField;
import com.csl.interfaces.models.IDbapiSerializable;
import com.csl.interfaces.models.IScannerSerializable;
import com.ucsl.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Collectors;

public class EntityHttpConnectionInput implements IScannerSerializable, IDbapiSerializable {
    private static final Logger logger = LoggerFactory.getLogger(EntityHttpConnectionInput.class);
    private String key;
    private String defaultValue;
    private boolean isSecret;
    private Map<Language, String> description;
    private Map<Language, String> tooltip;
    private Map<Language, String> placeholder;
    private Map<Language, String> display;

    private static Map<Language, String> parseLanguageMapFromDbapi(Json jsonMap) {
        return jsonMap.asJsonMap().entrySet().stream()
                .collect(
                        Collectors.toMap(
                                entry -> Language.fromDbapiName(entry.getKey()),
                                entry -> entry.getValue().asString()
                        )
                );
    }

    private static Map<Language, String> parseLanguageMapFromScanner(Json jsonMap) {
        return jsonMap.asJsonMap().entrySet().stream()
                .collect(
                        Collectors.toMap(
                                entry -> Language.fromScanName(entry.getKey()),
                                entry -> entry.getValue().asString()
                        )
                );
    }

    public static EntityHttpConnectionInput fromDbapiJson(Json json) {
        EntityHttpConnectionInput entityHttpConnectionInput = new EntityHttpConnectionInput();
        if (json.has(EntityHttpConnectionInputField.KEY.dbapiName()) && json.get(EntityHttpConnectionInputField.KEY.dbapiName()).isString()) {
            entityHttpConnectionInput.key = json.get(EntityHttpConnectionInputField.KEY.dbapiName()).asString();
        }
        if (json.has(EntityHttpConnectionInputField.DEFAULT_VALUE.dbapiName()) && json.get(EntityHttpConnectionInputField.DEFAULT_VALUE.dbapiName()).isString()) {
            entityHttpConnectionInput.defaultValue = json.get(EntityHttpConnectionInputField.DEFAULT_VALUE.dbapiName()).asString();
        }
        if (json.has(EntityHttpConnectionInputField.DESCRIPTION.dbapiName()) && json.get(EntityHttpConnectionInputField.DESCRIPTION.dbapiName()).isObject()) {
            entityHttpConnectionInput.description = parseLanguageMapFromDbapi(json.get(EntityHttpConnectionInputField.DESCRIPTION.dbapiName()))
                    .entrySet().stream().filter(entry -> entry.getKey() != null).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        if (json.has(EntityHttpConnectionInputField.TOOLTIP.dbapiName()) && json.get(EntityHttpConnectionInputField.TOOLTIP.dbapiName()).isObject()) {
            entityHttpConnectionInput.tooltip = parseLanguageMapFromDbapi(json.get(EntityHttpConnectionInputField.TOOLTIP.dbapiName()))
                    .entrySet().stream().filter(entry -> entry.getKey() != null).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        if (json.has(EntityHttpConnectionInputField.PLACEHOLDER.dbapiName()) && json.get(EntityHttpConnectionInputField.PLACEHOLDER.dbapiName()).isObject()) {
            entityHttpConnectionInput.placeholder = parseLanguageMapFromDbapi(json.get(EntityHttpConnectionInputField.PLACEHOLDER.dbapiName()))
                    .entrySet().stream().filter(entry -> entry.getKey() != null).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        if (json.has(EntityHttpConnectionInputField.DISPLAY.dbapiName()) && json.get(EntityHttpConnectionInputField.DISPLAY.dbapiName()).isObject()) {
            entityHttpConnectionInput.display = parseLanguageMapFromDbapi(json.get(EntityHttpConnectionInputField.DISPLAY.dbapiName()))
                    .entrySet().stream().filter(entry -> entry.getKey() != null).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        if (json.has(EntityHttpConnectionInputField.SECRET.dbapiName()) && json.get(EntityHttpConnectionInputField.SECRET.dbapiName()).isBoolean()) {
             entityHttpConnectionInput.isSecret = json.get(EntityHttpConnectionInputField.SECRET.dbapiName()).asBoolean();
        } else {
            entityHttpConnectionInput.isSecret = false;
        }
        return entityHttpConnectionInput;
    }

    public static EntityHttpConnectionInput fromScannerJson(Json json) {
        EntityHttpConnectionInput entityHttpConnectionInput = new EntityHttpConnectionInput();
        if (json.has(EntityHttpConnectionInputField.KEY.scanName()) && json.get(EntityHttpConnectionInputField.KEY.scanName()).isString()) {
            entityHttpConnectionInput.key = json.get(EntityHttpConnectionInputField.KEY.scanName()).asString();
        }
        if (json.has(EntityHttpConnectionInputField.DEFAULT_VALUE.scanName()) && json.get(EntityHttpConnectionInputField.DEFAULT_VALUE.scanName()).isString()) {
            entityHttpConnectionInput.defaultValue = json.get(EntityHttpConnectionInputField.DEFAULT_VALUE.scanName()).asString();
        }
        if (json.has(EntityHttpConnectionInputField.DESCRIPTION.scanName()) && json.get(EntityHttpConnectionInputField.DESCRIPTION.scanName()).isObject()) {
            entityHttpConnectionInput.description = parseLanguageMapFromScanner(json.get(EntityHttpConnectionInputField.DESCRIPTION.scanName()));
        }
        if (json.has(EntityHttpConnectionInputField.TOOLTIP.scanName()) && json.get(EntityHttpConnectionInputField.TOOLTIP.scanName()).isObject()) {
            entityHttpConnectionInput.tooltip = parseLanguageMapFromScanner(json.get(EntityHttpConnectionInputField.TOOLTIP.scanName()));
        }
        if (json.has(EntityHttpConnectionInputField.PLACEHOLDER.scanName()) && json.get(EntityHttpConnectionInputField.PLACEHOLDER.scanName()).isObject()) {
            entityHttpConnectionInput.placeholder = parseLanguageMapFromScanner(json.get(EntityHttpConnectionInputField.PLACEHOLDER.scanName()));
        }
        if (json.has(EntityHttpConnectionInputField.DISPLAY.scanName()) && json.get(EntityHttpConnectionInputField.DISPLAY.scanName()).isObject()) {
            entityHttpConnectionInput.display = parseLanguageMapFromScanner(json.get(EntityHttpConnectionInputField.DISPLAY.scanName()));
        }
        if (json.has(EntityHttpConnectionInputField.SECRET.scanName()) && json.get(EntityHttpConnectionInputField.SECRET.scanName()).isBoolean()) {
            entityHttpConnectionInput.isSecret = json.get(EntityHttpConnectionInputField.SECRET.scanName()).asBoolean();
        } else {
            entityHttpConnectionInput.isSecret = false;
        }
        return entityHttpConnectionInput;
    }

    public Json serializeForDbapi() {
        Json serialized = Json.object();
        if (key != null) {
            serialized.set(EntityHttpConnectionInputField.KEY.dbapiName(), key);
        }
        if (defaultValue != null) {
            serialized.set(EntityHttpConnectionInputField.DEFAULT_VALUE.dbapiName(), defaultValue);
        }
        if (description != null) {
            Json descriptionSerialized = Json.object();
            description.forEach((language, value) -> descriptionSerialized.set(language.dbapiName(), value));
            serialized.set(EntityHttpConnectionInputField.DESCRIPTION.dbapiName(), descriptionSerialized);
        }
        if (tooltip != null) {
            Json tooltipSerialized = Json.object();
            tooltip.forEach((language, value) -> tooltipSerialized.set(language.dbapiName(), value));
            serialized.set(EntityHttpConnectionInputField.TOOLTIP.dbapiName(), tooltipSerialized);
        }
        if (placeholder != null) {
            Json placeholderSerialized = Json.object();
            placeholder.forEach((language, value) -> placeholderSerialized.set(language.dbapiName(), value));
            serialized.set(EntityHttpConnectionInputField.PLACEHOLDER.dbapiName(), placeholderSerialized);
        }
        if (display != null) {
            Json displaySerialized = Json.object();
            display.forEach((language, value) -> displaySerialized.set(language.dbapiName(), value));
            serialized.set(EntityHttpConnectionInputField.DISPLAY.dbapiName(), displaySerialized);
        }
        serialized.set(EntityHttpConnectionInputField.SECRET.dbapiName(), isSecret);
        return serialized;
    }

    public Json serializeForScanner() {
        Json serialized = Json.object();
        if (key != null) {
            serialized.set(EntityHttpConnectionInputField.KEY.scanName(), key);
        }
        if (defaultValue != null) {
            serialized.set(EntityHttpConnectionInputField.DEFAULT_VALUE.scanName(), defaultValue);
        }
        if (description != null) {
            Json descriptionSerialized = Json.object();
            description.forEach((language, value) -> descriptionSerialized.set(language.scanName(), value));
            serialized.set(EntityHttpConnectionInputField.DESCRIPTION.scanName(), descriptionSerialized);
        }
        if (tooltip != null) {
            Json tooltipSerialized = Json.object();
            tooltip.forEach((language, value) -> tooltipSerialized.set(language.scanName(), value));
            serialized.set(EntityHttpConnectionInputField.TOOLTIP.scanName(), tooltipSerialized);
        }
        if (placeholder != null) {
            Json placeholderSerialized = Json.object();
            placeholder.forEach((language, value) -> placeholderSerialized.set(language.scanName(), value));
            serialized.set(EntityHttpConnectionInputField.PLACEHOLDER.scanName(), placeholderSerialized);
        }
        if (display != null) {
            Json displaySerialized = Json.object();
            display.forEach((language, value) -> displaySerialized.set(language.scanName(), value));
            serialized.set(EntityHttpConnectionInputField.DISPLAY.scanName(), displaySerialized);
        }
        serialized.set(EntityHttpConnectionInputField.SECRET.scanName(), isSecret);
        return serialized;
    }

    public enum Language {
        EN("en", "EN"),
        FR("fr", "FR");

        private String dbapiName;
        private String scanName;

        private Language(String dbapiName, String scanName) {
            this.dbapiName = dbapiName;
            this.scanName = scanName;
        }

        public String dbapiName() {
            return dbapiName;
        }

        public String scanName() {
            return scanName;
        }

        public static Language fromDbapiName(String dbapiName) {
            for (Language language : Language.values()) {
                if (language.dbapiName().equals(dbapiName)) {
                    return language;
                }
            }
            return null;
        }

        public static Language fromScanName(String scanName) {
            for (Language language : Language.values()) {
                if (language.scanName().equals(scanName)) {
                    return language;
                }
            }
            return null;
        }
    }
}
