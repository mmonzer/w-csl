package com.csl.autocrypt.outils;

import com.ucsl.json.Json;

import static com.csl.autocrypt.enums.AutocryptConstants.COUNTRY;

/**
 * Json helper
 */
public class JsonHelper {
    // TODO : migrate tto right folder

    /**
     * Checks if a key exists in a json if its value is a String and returns the value
     *
     * @param obj the json object to check
     * @param key the key inside the json obj
     * @return the value at that key
     * @throws IllegalArgumentException if argument missing of wrong format
     */
    public static String getValueString(Json obj, String key) throws IllegalArgumentException {
        if (obj.has(key) && obj.get(key).isString()) {
            return obj.get(key).asString();
        } else {
            throw new IllegalArgumentException(key);
        }
    }

    /**
     * Checks if a key exists in a json if its value is a String and returns the value. It also removes the key from the object
     *
     * @param obj the json object to check
     * @param key the key inside the json obj
     * @return the value at that key
     * @throws IllegalArgumentException if argument missing of wrong format
     */
    public static String extractValueString(Json obj, String key) throws IllegalArgumentException {
        String value = getValueString(obj, key);
        obj.delAt(key);
        return value;
    }

    /**
     * Moves the value of one object to another if exists.
     *
     * @param objOrigin the json object origin to get the value to move
     * @param objDest the json object destination where the new value is insert (overwritten if already exists)
     * @param key the key inside the json obj
     */
    public static String transferValueString(Json objOrigin, Json objDest, String key) throws IllegalArgumentException {
        String value = getValueString(objOrigin, key);
        objOrigin.delAt(key);
        objDest.set(key, value);
        return value;
    }

    /**
     * Checks if a key exists in a json if its value is a String
     *
     * @param obj the json object to check
     * @param key the key inside the json obj
     */
    public static String getValueStringOrNull(Json obj, String key) {
        if (obj.has(key) && obj.get(key).isString()) {
            return obj.get(key).asString();
        } else {
            return null;
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
     * Checks if a key exists in a json if its value is a Integer and returns the value. It also removes the key from the object
     *
     * @param obj the json object to check
     * @param key the key inside the json obj
     * @return the value at that key
     * @throws IllegalArgumentException if argument missing of wrong format
     */
    public static Integer extractValueInteger(Json obj, String key) throws IllegalArgumentException {
        Integer value = getValueInteger(obj, key);
        obj.delAt(key);
        return value;
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


    /**
     * If the field has a non-empty array, it extracts the first string and replaces the array
     * @param obj obj to replace array by first element
     * @param key key to replace
     */
    public static void replaceArrayByFirstElementIfExists(Json obj, String key) {
        if (obj.has(key) && obj.get(key).isArray() && !obj.get(key).asJsonList().isEmpty()) {
            obj.set(key, obj.get(key).asJsonList().get(0));
        }
    }
}
