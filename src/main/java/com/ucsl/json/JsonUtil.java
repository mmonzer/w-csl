package com.ucsl.json;

import java.util.Map;

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
}
