package com.csl.autocrypt.outils;

import com.ucsl.json.Json;

/**
 * Json helper
 */
public class JsonHelper {
    // TODO : migrate tto right folder

    /**
     * Checks if a key exists in a json if its value is a String
     *
     * @param obj the json object to check
     * @param key the key inside the json obj
     */
    public static String getValueString(Json obj, String key) throws IllegalArgumentException {
        if (obj.has(key) && obj.get(key).isString()) {
            return obj.get(key).asString();
        } else {
            throw new IllegalArgumentException(key);
        }
    }

    /**
     * Checks if a key exists in a json if its value is a Integer
     *
     * @param obj the json object to check
     * @param key the key inside the json obj
     */
    public static Integer getValueInteger(Json obj, String key) throws IllegalArgumentException {
        if (obj.has(key) && obj.get(key).isNumber()) {
            return obj.get(key).asInteger();
        } else {
            throw new IllegalArgumentException(key);
        }
    }

    /**
     * Merges two json objects into a new object json
     *
     * @param obj1 the json object to merge (priority)
     * @param obj2 the json object to merge
     */
    public static Json mergerJson(Json obj1, Json obj2) {
        Json obj = Json.object();
        if (obj2 != null) {
            for (String key : obj2.asJsonMap().keySet()) {
                obj.set(key, obj2.get(key));
            }
        }
        if (obj1 != null) {
            for (String key : obj1.asJsonMap().keySet()) {
                obj.set(key, obj1.get(key));
            }
        }
        return obj;
    }
}
