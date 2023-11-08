package com.csl.intercom.cslscan.models;

import com.ucsl.json.Json;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Model to reprensent a variable for an HTTP API connection.
 * Needed because representations are different between DB-API and scanner.
 */
public class HttpApiVariable {
    private Json value;
    private Map<String, HttpApiVariable> children;

    /**
     * Create a {@link HttpApiVariable} from the JSON object received from DB-API.
     * The expected format is:
     * <code>value</code>
     * or
     * <code>{"child1": value, "child2": {...}}</code>
     *
     * @param json The JSON object received from DB-API.
     * @return A {@link HttpApiVariable} deserialized.
     */
    public static HttpApiVariable fromDbapiJson(Json json) {
        HttpApiVariable httpApiVariable = new HttpApiVariable();
        if (json.isObject()) {
            httpApiVariable.value = null;
            httpApiVariable.children = json.asJsonMap().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> HttpApiVariable.fromDbapiJson(entry.getValue())));
        } else {
            httpApiVariable.value = json;
            httpApiVariable.children = null;
        }
        return httpApiVariable;
    }

    /**
     * Create a {@link HttpApiVariable} from the JSON object received from the scanner.
     * The expected format is:
     * <code>
     *     {
     *         "value": ...
     *     }
     * </code>
     *
     * @param json The JSON object received from the scanner.
     * @return A {@link HttpApiVariable} deserialized.
     */
    public static HttpApiVariable fromScannerJson(Json json) {
        HttpApiVariable httpApiVariable = new HttpApiVariable();
        Json value = json.get("value");
        if (value.isObject()) {
            httpApiVariable.value = null;
            httpApiVariable.children = value.asJsonMap().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> HttpApiVariable.fromScannerJson(entry.getValue())));
        } else {
            httpApiVariable.value = value;
            httpApiVariable.children = null;
        }
        return httpApiVariable;
    }

    public Json serializeForScanner() {
        if (this.children == null) {
            return Json.object("value", this.value);
        } else {
            Json childrenSerialized = Json.object();
            this.children.forEach((name, child) -> childrenSerialized.set(name, child.serializeForScanner()));
            return Json.object("value", childrenSerialized);
        }
    }

    public Json serializeForDbapi() {
        if (this.children == null) {
            return this.value;
        } else {
            Json childrenSerialized = Json.object();
            this.children.forEach((name, child) -> childrenSerialized.set(name, child.serializeForDbapi()));
            return childrenSerialized;
        }
    }
}
