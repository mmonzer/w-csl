package com.ucsl;

import static org.junit.jupiter.api.Assertions.*;

import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import org.junit.jupiter.api.Test;

public class JsonUtilTest {
    // region getValueBooleanOrDefault
    @Test
    public void testGetValueBooleanOrDefaultWhenKeyExistsAndIsBoolean() {
        Json mockJson = Json.object();
        mockJson.set("isActive", true);

        boolean result = JsonUtil.getValueBooleanOrDefault(mockJson, "isActive", false);
        assertTrue(result, "Should return the boolean value when key exists and is boolean");
    }

    @Test
    public void testGetValueBooleanOrDefaultWhenKeyExistsAndIsFalseBoolean() {
        Json mockJson = Json.object();
        mockJson.set("isActive", false);

        boolean result = JsonUtil.getValueBooleanOrDefault(mockJson, "isActive", true);
        assertFalse(result, "Should return the boolean value when key exists and is boolean");
    }

    @Test
    public void testGetValueBooleanOrDefaultWhenKeyDoesNotExist() {
        Json mockJson = Json.object();

        boolean result = JsonUtil.getValueBooleanOrDefault(mockJson, "isActive", true);
        assertTrue(result, "Should return the default value when key does not exist");
    }

    @Test
    public void testGetValueBooleanOrDefaultWhenKeyExistsButNotBoolean() {
        Json mockJson = Json.object();
        mockJson.set("isActive", "notABoolean");

        boolean result = JsonUtil.getValueBooleanOrDefault(mockJson, "isActive", true);
        assertTrue(result, "Should return the default value when key exists but is not boolean");
        result = JsonUtil.getValueBooleanOrDefault(mockJson, "isActive", false);
        assertFalse(result, "Should return the default value when key exists but is not boolean");
    }

    @Test
    public void testGetValueBooleanOrDefaultWhenKeyExistsButIsNull() {
        Json mockJson = Json.object();
        mockJson.set("isActive", null);

        boolean result = JsonUtil.getValueBooleanOrDefault(mockJson, "isActive", false);
        assertFalse(result, "Should return the default value when key exists but is null");
    }

    @Test
    public void testGetValueBooleanOrDefaultWhenKeyIsNull() {
        boolean result = JsonUtil.getValueBooleanOrDefault(Json.object(), null, false);
        assertFalse(result, "Should return the default value when key is null");
        result = JsonUtil.getValueBooleanOrDefault(Json.object(), null, true);
        assertTrue(result, "Should return the default value when key is null");
    }

    // endregion getValueBooleanOrDefault

    // region getValueStringOrDefault
    @Test
    public void testGetValueStringOrDefaultWhenKeyExistsAndIsString() {
        Json mockJson = Json.object();
        mockJson.set("isActive", "a");

        String result = JsonUtil.getValueStringOrDefault(mockJson, "isActive",  "x");
        assertEquals("a", result, "Should return the string value when key exists and is string");
    }

    @Test
    public void testGetValueStringOrDefaultWhenKeyDoesNotExist() {
        Json mockJson = Json.object();

        String result = JsonUtil.getValueStringOrDefault(mockJson, "isActive", "w");
        assertEquals("w", result, "Should return the default value when key does not exist");
    }

    @Test
    public void testGetValueStringOrDefaultWhenKeyExistsButIsBoolean() {
        Json mockJson = Json.object();
        mockJson.set("isActive", true);

        String result = JsonUtil.getValueStringOrDefault(mockJson, "isActive", "a");
        assertEquals("a", result,  "Should return the default value when key exists but is boolean");
    }

    @Test
    public void testGetValueStringOrDefaultWhenKeyExistsButIsNumber() {
        Json mockJson = Json.object();
        mockJson.set("isActive", 1);

        String result = JsonUtil.getValueStringOrDefault(mockJson, "isActive", "a");
        assertEquals("a", result,  "Should return the default value when key exists but is a number");
    }

    @Test
    public void testGetValueStringOrDefaultWhenKeyExistsButIsList() {
        Json mockJson = Json.object();
        mockJson.set("isActive", Json.array());

        String result = JsonUtil.getValueStringOrDefault(mockJson, "isActive", "a");
        assertEquals("a", result,  "Should return the default value when key exists but is a JSON list");
    }

    @Test
    public void testGetValueStringOrDefaultWhenKeyExistsButIsNull() {
        Json mockJson = Json.object();
        mockJson.set("isActive", null);

        String result = JsonUtil.getValueStringOrDefault(mockJson, "isActive", "a");
        assertEquals("a", result, "Should return the default value when key exists but is null");
    }

    @Test
    public void testGetValueStringOrDefaultWhenKeyIsNull() {
        String result = JsonUtil.getValueStringOrDefault(Json.object(), null, "a");
        assertEquals("a", result, "Should return the default value when key is null");
    }

    // endregion getValueStringOrDefault

    // region getValueIntegerOrDefault
    @Test
    public void testGetValueIntegerOrDefaultWhenKeyExistsAndIsInteger() {
        Json mockJson = Json.object();
        mockJson.set("isActive", 1);

        Integer result = JsonUtil.getValueIntegerOrDefault(mockJson, "isActive",  2);
        assertEquals(1, result, "Should return the integer value when key exists and is number");
    }

    @Test
    public void testGetValueIntegerOrDefaultWhenKeyDoesNotExist() {
        Json mockJson = Json.object();

        Integer result = JsonUtil.getValueIntegerOrDefault(mockJson, "isActive", 1);
        assertEquals(1, result, "Should return the default value when key does not exist");
    }

    @Test
    public void testGetValueIntegerOrDefaultWhenKeyExistsButIsBoolean() {
        Json mockJson = Json.object();
        mockJson.set("isActive", true);

        Integer result = JsonUtil.getValueIntegerOrDefault(mockJson, "isActive", 1);
        assertEquals(1, result,  "Should return the default value when key exists but is boolean");
    }

    @Test
    public void testGetValueIntegerOrDefaultWhenKeyExistsButIsString() {
        Json mockJson = Json.object();
        mockJson.set("isActive", "q");

        Integer result = JsonUtil.getValueIntegerOrDefault(mockJson, "isActive", 1);
        assertEquals(1, result,  "Should return the default value when key exists but is a number");
    }

    @Test
    public void testGetValueIntegerOrDefaultWhenKeyExistsButIsNumberInStringFormat() {
        Json mockJson = Json.object();
        mockJson.set("isActive", "2");

        Integer result = JsonUtil.getValueIntegerOrDefault(mockJson, "isActive", 1);
        assertEquals(1, result,  "Should return the default value when key exists but is a number");
    }

    @Test
    public void testGetValueIntegerOrDefaultWhenKeyExistsButIsList() {
        Json mockJson = Json.object();
        mockJson.set("isActive", Json.array());

        Integer result = JsonUtil.getValueIntegerOrDefault(mockJson, "isActive", 1);
        assertEquals(1, result,  "Should return the default value when key exists but is a JSON list");
    }

    @Test
    public void testGetValueIntegerOrDefaultWhenKeyExistsButIsNull() {
        Json mockJson = Json.object();
        mockJson.set("isActive", null);

        Integer result = JsonUtil.getValueIntegerOrDefault(mockJson, "isActive", 1);
        assertEquals(1, result, "Should return the default value when key exists but is null");
    }

    @Test
    public void testGetValueIntegerOrDefaultWhenKeyIsNull() {
        Integer result = JsonUtil.getValueIntegerOrDefault(Json.object(), null, 1);
        assertEquals(1, result, "Should return the default value when key is null");
    }

    // endregion getValueIntegerOrDefault
}
