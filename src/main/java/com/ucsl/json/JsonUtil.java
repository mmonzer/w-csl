package com.ucsl.json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.csl.autocrypt.enums.AutocryptConstants.LIST_DELIMITER;

public class JsonUtil {

    public static void setElement(Json j, String s, Json value) throws JsonException {

        setElement(j, s, value, false);
    }

    public static void setElement(Json j, String s, Json value, boolean createMissingProps) throws JsonException {

        JsonSelector js = new JsonSelector().parse(s);

        if (js.size() == 0) throw new JsonException("Invalid selector:" + s);

        setElement(j, js.get(0), value, createMissingProps);
    }

    public static void setElement(Json j, JsonSelectorElement jse, Json value, boolean createMissingProps) throws JsonException {

        //System.out.println("j="+j);

        if (jse.isIndex()) {
            if (!j.isArray()) throw new JsonException("Expecting array for " + jse.getParent().getName());
            int len = j.asJsonList().size();
            if (jse.getIndex() < len) {
                if (jse.getChild() == null) {
                    j.set(jse.getIndex(), value);
                    return;
                }

                setElement(j.at(jse.getIndex()), jse.getChild(), value, createMissingProps);
            } else
                throw new JsonException("Index out of bounds " + jse.getParent().getName() + " index:" + jse.getIndex());
        } else if (jse.isStarSelector()) {
            if (!jse.getParent().isArray()) throw new JsonException("Expecting array for " + jse.getParent().getName());
            if (jse.getChild() == null) {
                j.add(value);
            } else {
                Json jj;
                if (jse.getChild().isIndex() || jse.getChild().isStarSelector()) {
                    jj = Json.array();
                } else {
                    jj = Json.object();
                }
                j.add(jj);
                setElement(jj, jse.getChild(), value, createMissingProps);
            }
        } else {
            if (j.isArray()) {
                throw new JsonException("Index expected :" + jse.getField());
            }
            if (jse.getChild() == null) {
                j.set(jse.getField(), value);
            } else {
                //System.out.println("Field="+jse.getField());
                if (j.has(jse.getField())) {
                    setElement(j.get(jse.getField()), jse.getChild(), value, createMissingProps);
                } else {
                    if (!createMissingProps)
                        throw new JsonException("Invalid prop  <" + jse.getField() + "> in " + jse.getParent().getName());

                    Json jj;
                    if (jse.getChild().isIndex() || jse.getChild().isStarSelector()) {
                        jj = Json.array();
                    } else {
                        jj = Json.object();
                    }
                    j.set(jse.getField(), jj);
                    setElement(jj, jse.getChild(), value, createMissingProps);
                }
            }
        }
    }

    public static Json getElement(Json j, String s) throws JsonException {
        JsonSelector js = new JsonSelector().parse(s);

        if (js.size() == 0) throw new JsonException("Invalid selector:" + s);

        return getElement(j, js.get(0));
    }

    public static Json getElement(Json j, JsonSelectorElement jse) throws JsonException {

        //System.out.println("j="+j);

        if (jse.isIndex()) {
            if (!j.isArray()) throw new JsonException("Expecting array for " + jse.getParent().getName());
            int len = j.asJsonList().size();
            if (jse.getIndex() < len) {
                if (jse.getChild() == null) {
                    //	j.set(jse.getIndex(), value);
                    return j.at(jse.index);
                }

                return getElement(j.at(jse.getIndex()), jse.getChild());
            }
            throw new JsonException("Index out of bounds " + jse.getParent().getName() + " index:" + jse.getIndex());
        } else if (jse.isStarSelector()) {
            throw new JsonException("Invalid index value " + jse.getParent().getName() + " *");
        } else {
            if (jse.getChild() == null) {
                return j.get(jse.getField());
            } else {
                //System.out.println("Field="+jse.getField());
                if (j.has(jse.getField())) {
                    return getElement(j.get(jse.getField()), jse.getChild());
                } else {
                    throw new JsonException("Invalid prop name " + jse.getField() + " in " + jse.getParent().getName());
                }
            }
        }
    }

    public static String getStringFromJson(Json j, String propName, String defaultValue) {
        //Json v=j.get(propName);
        Json v = findChild(j, propName);
        if (v == null) return defaultValue;
        return v.asString();
    }

    public static Boolean getBooleanFromJson(Json j, String propName, Boolean defaultValue) {
        //Json v=j.get(propName);
        Json v = findChild(j, propName);
        if (v == null) return defaultValue;
        return v.asBoolean();
    }

    public static Long getLongFromJson(Json j, String propName, long defaultValue) {
        //Json v=j.get(propName);
        Json v = findChild(j, propName);
        if (v == null) return defaultValue;
        return v.asLong();
    }

    public static Integer getIntFromJson(Json j, String propName, Integer defaultValue) {
        //Json v=j.get(propName);
        Json v = findChild(j, propName);
        if (v == null) return defaultValue;
        return v.asInteger();
    }

    public static Double getDoubleFromJson(Json j, String propName, Double defaultValue) {
        //Json v=j.get(propName);
        Json v = findChild(j, propName);
        if (v == null) return defaultValue;
        return v.asDouble();
    }

    public static Json getJson(Json j, String propName) {
        Json v = findChild(j, propName);
        return v;
    }

    public static Json findChild(Json j, String path) {

        if (j == null) return null;

        path = path.replace(".", "/");

        String[] paths = path.split("/");
        for (int i = 0; i < paths.length; i++) {
            j = j.get(paths[i]);
            if (j == null) return null;
        }
        return j;
    }

    public static String prettyPrint(Json j) {
        return prettyPrint2(j);
    }

    public static Json concat(Json p1, Json p2) {
        // TODO Auto-generated method stub
        Json result = Json.object();

        for (String key : p1.asJsonMap().keySet()) {
            result.set(key, p1.get(key));
        }


        for (String key : p2.asJsonMap().keySet()) {
            result.set(key, p2.get(key));
        }

        return result;
    }

    public static String prettyPrint2(Json j) {
        return object2str(j, "", "", false);
    }

    public static boolean compare(Json j1, Json j2) {
        String s1 = object2str(j1, "", "", false);
        String s2 = object2str(j2, "", "", false);

        return s1.compareTo(s2) == 0;
    }

    public static String object2str(Json j, String decal, String name, boolean addComma) {

        String result = "";
        String quote = "\"";
        if (j.isArray()) {
            int n = j.asJsonList().size();
            int i = 1;
            if (!name.isEmpty())
                result = result + decal + name + ": [";
            else
                result = decal + "[";
            if (n == 0) {
                if (addComma) return result + "],\n";
                else return result + "]\n";
            }
            result = result + "\n";

            String se = ",\n";
            for (Json je : j.asJsonList()) {
                if (je.isObject() || je.isArray()) {
                    boolean add = (i != n);
                    result = result + object2str(je, decal + "  ", "", add);
                } else {
                    if (i == n) se = "\n";
                    result = result + decal + "  " + je.toString() + se;
                }
                i++;
            }
            if (addComma) result = result + decal + "],\n";
            else result = result + decal + "]\n";
        } else if (j.isObject()) {
            result = "";
            Map<String, Json> map = j.asJsonMap();
            int n = map.size();
            int i = 1;
            String se = ",\n";

            if (!name.isEmpty())
                result = result + decal + name + ": {";
            else
                result = decal + "{";

            if (n == 0) {
                if (addComma) return result + "},\n";
                else return result + "}\n";
            }
            result = result + "\n";


            for (Map.Entry<String, Json> entry : map.entrySet()) {
                String key = entry.getKey();
                Json je = entry.getValue();
                if (je.isObject() || je.isArray()) {
                    boolean add = (i != n);
                    result = result + object2str(je, decal + "  ", quote + key + quote, add);
                } else {

                    if (i == n) se = "\n";
                    result = result + decal + "  " + quote + key + quote + ":" + je.toString() + se;
                }
                i++;
            }
            if (addComma) result = result + decal + "},\n";
            else result = result + decal + "}\n";
        } else result = j.toString();
        return result;
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
     * @param objDest   the json object destination where the new value is insert (overwritten if already exists)
     * @param key       the key inside the json obj
     */
    public static String transferValueString(Json objOrigin, Json objDest, String key) throws IllegalArgumentException {
        String value = getValueString(objOrigin, key);
        if (objOrigin.has(key)) {
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
        if (objOrigin.has(key)) {
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
    public static Integer transferValueIntegerOrNull(Json objOrigin, Json objDest, String key) throws IllegalArgumentException {
        Integer value = getValueIntegerOrNull(objOrigin, key);
        if (objOrigin.has(key)) {
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
        if (obj.has(key) && obj.get(key).isString()) {
            String str = obj.get(key).asString();
            obj.delAt(key);
            return str;
        } else if (obj.has(key) && obj.get(key).isNull()) {
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
    public static Boolean extractValueBooleanOrNull(Json obj, String key) {
        if (obj.has(key) && obj.get(key).isBoolean()) {
            Boolean bool = obj.get(key).asBoolean();
            obj.delAt(key);
            return bool;
        } else if (obj.has(key) && obj.get(key).isNull()) {
            obj.delAt(key);
            return null;
        } else {
            return null;
        }
    }

    /**
     * Checks if a key exists in a json and return its value if it is a String
     *
     * @param obj the json object to check
     * @param key the key inside the json obj
     */
    public static String getValueStringOrNull(Json obj, String key) {
        return getValueStringOrDefault(obj, key, null);
    }

    /**
     * Checks if a key exists in a json and return its value if it is a String
     *
     * @param obj the json object to check
     * @param key the key inside the json obj
     */
    public static String getValueStringOrDefault(Json obj, String key, String defaultValue) {
        if (obj.has(key) && !obj.get(key).isNull() && obj.get(key).isString()) {
            return obj.get(key).isNull()?null:obj.get(key).asString();
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
    public static boolean getValueBooleanOrDefault(Json obj, String key, boolean defaultValue) {
        if (obj.has(key) && obj.get(key).isBoolean()) {
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
     * Checks if a key exists in a json and return its value if it is a Integer
     *
     * @param obj the json object to check
     * @param key the key inside the json obj
     */
    public static Integer getValueIntegerOrNull(Json obj, String key) {
        if (obj.has(key) && obj.get(key).isNumber()) {
            return obj.get(key).asInteger();
        } else {
            return null;
        }
    }

    /**
     * Checks if a key exists in a json and return its value if it is a boolean
     *
     * @param obj the json object to check
     * @param key the key inside the json obj
     */
    public static Boolean getValueBooleanOrNull(Json obj, String key) {
        if (obj.has(key) && obj.get(key).isBoolean()) {
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
    public static List<String> getValueListStrOrNull(Json obj, String key) {
        if (obj.has(key) && obj.get(key).isArray()) {
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

    public static String jsonListToString(Json array, String delimiter) {
        if (array == null) {
            return null;
        }
        if (array.isArray()) {
            if (array.asJsonList().isEmpty()) {
                return "";
            }
            String str = "";
            for (Json e : array.asJsonList()) {
                str += ',' + (e.isString() ? e.asString() : e.toString());
            }
            return str.substring(1);
        }
        return array.toString();
    }

    public static void jsonListToStringListAtJson(Json obj, String key) {
        jsonListToStringListAtJson(obj, key, LIST_DELIMITER);
    }

    public static void jsonListToStringListAtJson(Json obj, String key, String delimiter) {
        if (obj != null && obj.has(key) && !obj.get(key).isNull() && obj.get(key).isArray()) {
            List<Json> array = obj.get(key).asJsonList();
            String str = "";
            if (!array.isEmpty()) {
                for (Json e : array) {
                    str += delimiter + (e.isString() ? e.asString() : e.toString());
                }
                obj.set(key, str.substring(1));
            } else {
                obj.set(key, str);
            }
        }
    }

    /**
     * JsonArray to byteArray
     * @param obj array to convert in bytearray
     */
    public static byte[] jsonListToByteArray(Json obj) throws IllegalArgumentException {
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
