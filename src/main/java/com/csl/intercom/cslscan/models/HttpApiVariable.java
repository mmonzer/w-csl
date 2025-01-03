package com.csl.intercom.cslscan.models;

import com.csl.interfaces.models.IDbapiSerializable;
import com.csl.interfaces.models.IScannerSerializable;
import com.csl.util.ListUtils;
import com.ucsl.json.Json;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Model to reprensent a variable for an HTTP API connection.
 * Needed because representations are different between DB-API and scanner.
 */
public class HttpApiVariable implements IScannerSerializable, IDbapiSerializable {
    public static final String VALUE = "value";
    private Json value;
    private Map<String, HttpApiVariable> childrenMap;
    private List<HttpApiVariable> childrenList;

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
            httpApiVariable.childrenMap = json.asJsonMap().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> HttpApiVariable.fromDbapiJson(entry.getValue())));
            httpApiVariable.childrenList = null;
        } else if (json.isArray()) {
            httpApiVariable.value = null;
            httpApiVariable.childrenMap = null;
            httpApiVariable.childrenList = ListUtils.map(json.asJsonList(), HttpApiVariable::fromDbapiJson);
        } else {
            httpApiVariable.value = json;
            httpApiVariable.childrenMap = null;
            httpApiVariable.childrenList = null;
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
        Json value = json.get(VALUE);
        if (value.isObject()) {
            httpApiVariable.value = null;
            httpApiVariable.childrenMap = value.asJsonMap().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> HttpApiVariable.fromScannerJson(entry.getValue())));
            httpApiVariable.childrenList = null;
        } else if (value.isArray()) {
            httpApiVariable.value = null;
            httpApiVariable.childrenMap = null;
            httpApiVariable.childrenList = ListUtils.map(value.asJsonList(), HttpApiVariable::fromScannerJson);
        } else {
            httpApiVariable.value = value;
            httpApiVariable.childrenMap = null;
            httpApiVariable.childrenList = null;
        }
        return httpApiVariable;
    }

    public Json serializeForScanner() {
        if (this.childrenMap != null) {
            Json childrenSerialized = Json.object();
            this.childrenMap.forEach((name, child) -> childrenSerialized.set(name, child.serializeForScanner()));
            return Json.object(VALUE, childrenSerialized);
        } else if (this.childrenList != null) {
            Json childrenSerialized = Json.array();
            this.childrenList.forEach(child -> childrenSerialized.add(child.serializeForScanner()));
            return Json.object(VALUE, childrenSerialized);
        } else {
            return Json.object(VALUE, this.value);
        }
    }

    public Json serializeForDbapi() {
        if (this.childrenMap != null) {
            Json childrenSerialized = Json.object();
            this.childrenMap.forEach((name, child) -> childrenSerialized.set(name, child.serializeForDbapi()));
            return childrenSerialized;
        } else if (this.childrenList != null) {
            Json childrenSerialized = Json.array();
            this.childrenList.forEach(child -> childrenSerialized.add(child.serializeForDbapi()));
            return childrenSerialized;
        } else {
            return this.value;
        }
    }
}
