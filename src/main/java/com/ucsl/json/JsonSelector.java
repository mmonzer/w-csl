package com.ucsl.json;

import java.util.ArrayList;

public class JsonSelector extends ArrayList<JsonSelectorElement> {

    public JsonSelectorElement root = new JsonSelectorElement("root").setRoot(true);

    public JsonSelector() {

    }

    public JsonSelector add(String s) {

        JsonSelectorElement parent = getCurrentParent();

        if (s.trim().compareTo("*") == 0) parent.setArray(true);

        JsonSelectorElement jse = new JsonSelectorElement(s);
        this.add(jse);
        jse.setParent(parent);

        return this;
    }

    public JsonSelectorElement getCurrentParent() {
        if (size() == 0)
            return root;
        else {
            int n = size() - 1;
            return get(n);
        }
    }

    public JsonSelector add(int i) {
        JsonSelectorElement parent = getCurrentParent();

        parent.setArray(true);
        JsonSelectorElement jse = new JsonSelectorElement(i);
        this.add(jse);
        jse.setParent(parent);

        return this;
    }

    public JsonSelector add(double d) {
        this.add((int) d);
        return this;
    }

    public JsonSelector parse(String s) {

        String[] tokens = s.split("\\.");
        for (String string : tokens) {
            String token = string.trim();
            if (isNumeric(token)) {
                this.add((getIntegerValue(string)));
            } else {
                this.add(string);
            }
        }

        return this;
    }

    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            int d = Integer.parseInt(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    public static int getIntegerValue(String strNum) {
        if (strNum == null) {
            return -1;
        }
        try {
            int d = Integer.parseInt(strNum);
            return d;
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }

    public String toString() {
        String s = "";
        for (JsonSelectorElement js : this) {
            if (!s.isEmpty()) s = s + ".";
            s = s + js;
        }
        return s;
    }
}
