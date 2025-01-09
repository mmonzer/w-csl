package com.ucsl.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.csl.autocrypt.enums.AutocryptConstants.LIST_DELIMITER;

public class JsonUtil {
    private JsonUtil() {}

    public static String getStringFromJson(Json j, String propName, String defaultValue) {
        Json v = findChild(j, propName);
        if (v == null || !v.isString()) return defaultValue;
        return v.asString();
    }

    public static Boolean getBooleanFromJson(Json j, String propName, Boolean defaultValue) {
        Json v = findChild(j, propName);
        if (v == null || !v.isBoolean()) return defaultValue;
        return v.asBoolean();
    }

    public static Long getLongFromJson(Json j, String propName, long defaultValue) {
        Json v = findChild(j, propName);
        if (v == null || !v.isNumber()) return defaultValue;
        return v.asLong();
    }

    public static Integer getIntFromJson(Json j, String propName, Integer defaultValue) {
        Json v = findChild(j, propName);
        if (v == null || !v.isNumber()) return defaultValue;
        return v.asInteger();
    }

    public static Double getDoubleFromJson(Json j, String propName, Double defaultValue) {
        Json v = findChild(j, propName);
        if (v == null || !v.isNumber()) return defaultValue;
        return v.asDouble();
    }

    public static Json getJson(Json j, String propName) {
        return findChild(j, propName);
    }

    public static Json findChild(Json j, String path) {

        if (j == null) return null;

        path = path.replace(".", "/");

        String[] paths = path.split("/");
        for (String s : paths) {
            j = j.get(s);
            if (j == null) return null;
        }
        return j;
    }

    public static String prettyPrint(Json j) {
        return json2str(j);
    }

    public static String json2str(Json json) {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = json.toString();
        try {
            JsonNode jsonObject = objectMapper.readTree(jsonString);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
        } catch (Exception e) {
            return jsonString;
        }
    }


    /**
     * Checks if a key exists in a json if its value is a String and returns the value
     *
     * @param obj the json object to check
     * @param key the key inside the json obj
     * @return the value at that key
     * @throws IllegalArgumentException if argument missing of wrong format
     */
    public static String getValueString(Json obj, String key) throws IllegalArgumentException {
        if (hasExistingKeyAndNotNull(obj, key) && obj.get(key).isString()) {
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
     * @param objDest   the json object destination where the new value is insert (overwritten if already exists)
     * @param key       the key inside the json obj
     */
    public static String transferValueString(@Nullable Json objOrigin, Json objDest, @NotNull String key) throws IllegalArgumentException {
        String value = getValueString(objOrigin, key);
        if (objOrigin!=null && objOrigin.has(key)) {
            objOrigin.delAt(key);
        }
        objDest.set(key, value);
        return value;
    }

    /**
     * Transfers the value of the first object to the second one if key exists.
     *
     * @param objOrigin the json object origin to get the value to move
     * @param objDest   the json object destination where the new value is insert (overwritten if already exists)
     * @param keys       the key inside the json obj
     */
    public static void transferValueStringOrNull(Json objOrigin, Json objDest, String... keys) throws IllegalArgumentException {
        for (String key : keys) {
            transferValueStringOrNull(objOrigin, objDest, key);
        }
    }

    /**
     * Transfers the value of the first object to the second one if key exists.
     *
     * @param objOrigin the json object origin to get the value to move
     * @param objDest   the json object destination where the new value is insert (overwritten if already exists)
     * @param key       the key inside the json obj
     */
    public static String transferValueStringOrNull(Json objOrigin, Json objDest, String key) throws IllegalArgumentException {
        String value = getValueStringOrNull(objOrigin, key);
        if (key !=null && objOrigin.has(key)) {
            objOrigin.delAt(key);
        }
        objDest.set(key, value);
        return value;
    }

    /**
     * Copy the value of the first object to the second one if key exists.
     *
     * @param objOrigin the json object origin to get the value to move
     * @param objDest   the json object destination where the new value is insert (overwritten if already exists)
     * @param keys       list of keys
     */
    public static void copyValueOrNull(Json objOrigin, Json objDest, String... keys) throws IllegalArgumentException {
        for (String key : keys){objDest.set(key, objOrigin.get(key));}
    }

    /**
     * Transfers the value of the first object to the second one if key exists.
     *
     * @param objOrigin the json object origin to get the value to move
     * @param objDest   the json object destination where the new value is insert (overwritten if already exists)
     * @param key       the key inside the json obj
     */
    public static Integer transferValueIntegerOrNull(@NotNull Json objOrigin, Json objDest, String key) throws IllegalArgumentException {
        Integer value = getValueIntegerOrNull(objOrigin, key);
        if (key !=null && objOrigin.has(key)) {
            objOrigin.delAt(key);
        }
        objDest.set(key, value);
        return value;
    }

    /**
     * Checks if a key exists in a json if its value is a String
     *
     * @param obj the json object to check
     * @param key the key inside the json obj
     */
    public static String extractValueStringOrNull(Json obj, String key) {
        if (hasExistingKeyAndNotNull(obj, key)  && obj.get(key).isString()) {
            String str = obj.get(key).asString();
            obj.delAt(key);
            return str;
        } else if (obj != null && obj.has(key) && obj.get(key).isNull()) {
            obj.delAt(key);
            return null;
        } else {
            return null;
        }
    }

    /**
     * Checks if a key exists in a json if its value is a String
     *
     * @param obj the json object to check
     * @param key the key inside the json obj
     */
    public static @Nullable Boolean extractValueBooleanOrNull(Json obj, String key) {
        if (hasExistingKeyAndNotNull(obj, key)  && obj.get(key).isBoolean()) {
            Boolean bool = obj.get(key).asBoolean();
            obj.delAt(key);
            return bool;
        } else if (obj!=null && obj.has(key) && obj.get(key).isNull()) {
            obj.delAt(key);
        }
        return null;

    }

    /**
     * Checks if a key exists in a json and return its value if it is a String
     *
     * @param obj the json object to check
     * @param key the key inside the json obj
     */
    public static String getValueStringOrNull(@NotNull Json obj, String key) {
        return getValueStringOrDefault(obj, key, null);
    }

    /**
     * Checks if a key exists in a json and return its value if it is a String
     *
     * @param obj the json object to check
     * @param key the key inside the json obj
     */
    public static String getValueStringOrDefault(@NotNull Json obj, @Nullable String key, @Nullable String defaultValue) {
        if (hasExistingKeyAndNotNull(obj, key)  && obj.get(key).isString()) {
            return obj.get(key).asString();
        } else {
            return defaultValue;
        }
    }

    /**
     * Checks if a key exists in a json and return its value if it is a String
     *
     * @param obj the json object to check
     * @param key the key inside the json obj
     */
    public static boolean getValueBooleanOrDefault(@NotNull Json obj, @Nullable String key, boolean defaultValue) {
        if (hasExistingKeyAndNotNull(obj, key)  && obj.get(key).isBoolean()) {
            return obj.get(key).asBoolean();
        } else {
            return defaultValue;
        }
    }

    /**
     * Checks if a key exists in a json if its value is a Integer
     *
     * @param obj the json object to check
     * @param key the key inside the json obj
     */
    public static Integer getValueInteger(@NotNull Json obj, @Nullable String key) throws IllegalArgumentException {
        if (hasExistingKeyAndNotNull(obj, key) && obj.get(key).isNumber()) {
            return obj.get(key).asInteger();
        } else {
            throw new IllegalArgumentException(key);
        }
    }

    /**
     * Checks whether the json object and key are not null and whether the key has non null value
     * @param obj json object
     * @param key key value
     * @return whether there is a non-null value
     */
    public static boolean hasExistingKeyAndNotNull(@Nullable Json obj, @Nullable String key) {
        return obj != null && key != null && obj.has(key) && !obj.get(key).isNull();
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
     * Checks if a key exists in a json and return its value if it is a Integer
     *
     * @param obj the json object to check
     * @param key the key inside the json obj
     */
    public static Integer getValueIntegerOrNull(@NotNull Json obj, @Nullable String key) {
        return getValueIntegerOrDefault(obj, key, null);
    }

    /**
     * Checks if a key exists in a json and return its value if it is a Integer
     *
     * @param obj the json object to check
     * @param key the key inside the json obj
     */
    public static Integer getValueIntegerOrDefault(@NotNull Json obj, @Nullable String key, @Nullable Integer defaultValue) {
        if (hasExistingKeyAndNotNull(obj, key) && obj.get(key).isNumber()) {
            return obj.get(key).asInteger();
        } else {
            return defaultValue;
        }
    }

    /**
     * Checks if a key exists in a json and return its value if it is a boolean
     *
     * @param obj the json object to check
     * @param key the key inside the json obj
     */
    public static @Nullable Boolean getValueBooleanOrNull(@NotNull Json obj, @Nullable String key) {
        if (hasExistingKeyAndNotNull(obj, key) && obj.get(key).isBoolean()) {
            return obj.get(key).asBoolean();
        } else {
            return null;
        }
    }

    /**
     * Checks if a key exists in a json and return its value if it is a list of strings
     *
     * @param obj the json object to check
     * @param key the key inside the json obj
     */
    public static @Nullable List<String> getValueListStrOrNull(Json obj, String key) {
        if (hasExistingKeyAndNotNull(obj, key)  && obj.get(key).isArray()) {
            List<String> list = new ArrayList<>();
            for (Json e : obj.get(key).asJsonList()) {
                if (e.isString()) {
                    list.add(e.asString());
                }
            }
            return list;
        } else {
            return null;
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

    /**
     * If the field has a non-empty array, it extracts the first string and replaces the array
     *
     * @param obj obj to replace array by first element
     * @param key key to replace
     */
    public static void replaceArrayByFirstElementIfExists(Json obj, String key) {
        if (obj.has(key) && obj.get(key) != null && obj.get(key).isArray() && !obj.get(key).asJsonList().isEmpty()) {
            obj.set(key, obj.get(key).asJsonList().get(0));
        }
    }

    /**
     * Drop keys if exists
     *
     * @param obj
     * @param keys
     */
    public static void drop(Json obj, String... keys) {
        if (obj != null) {
            for (String key : keys) {
                if (obj.has(key)) {
                    obj.delAt(key);
                }
            }
        }
    }

    /**
     * Copy keys if exists
     *
     * @param objOrigin
     * @param objDest
     * @param keys
     */
    public static void copyTo(Json objOrigin, Json objDest, String... keys) {
        if (objDest == null) {
            objDest = Json.object();
        }
        if (objOrigin != null) {
            for (String key : keys) {
                if (objOrigin.has(key)) {
                    objDest.set(key, objOrigin.get(key));
                    objOrigin.delAt(key);
                }
            }
        }
    }

    public static void jsonListToStringListAtJson(Json obj, String key) {
        jsonListToStringListAtJson(obj, key, LIST_DELIMITER);
    }

    public static void jsonListToStringListAtJson(Json obj, String key, String delimiter) {
        if (obj != null && obj.has(key) && !obj.get(key).isNull() && obj.get(key).isArray()) {
            List<Json> array = obj.get(key).asJsonList();
            StringBuilder str = new StringBuilder();
            if (!array.isEmpty()) {
                for (Json e : array) {
                    str.append(delimiter).append(e.isString() ? e.asString() : e.toString());
                }
                obj.set(key, str.substring(1));
            } else {
                obj.set(key, str.toString());
            }
        }
    }

    /**
     * JsonArray to byteArray
     * @param obj array to convert in bytearray
     */
    public static byte[] jsonListToByteArray(@NotNull Json obj) throws IllegalArgumentException {
        if (!obj.isArray()) {
            throw new IllegalArgumentException("Not array");
        }

        List<Json> list = obj.asJsonList();
        byte[] byteArray = new byte[list.size()];

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).isNull() || !list.get(i).isNumber() ) {
                throw new IllegalArgumentException("Element is not a number");
            }
            byteArray[i] = list.get(i).asByte();
        }
        return byteArray;
    }


}
