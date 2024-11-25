package com.ucsl;

import static org.junit.jupiter.api.Assertions.*;

import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class JsonUtilTest {
    static final String KEY = "isActive";
    static final String STRING_VALUE = "a";
    Json mockJson;

    @BeforeEach
    void getJson() {
        mockJson = Json.object();
    }

    // region hasExistingKeyAndNotNull
    @Test
    void testHasExistingKeyAndNotNullWhenKeyExists() {
        mockJson.set(KEY, true);

        boolean result = JsonUtil.hasExistingKeyAndNotNull(mockJson, KEY);
        assertTrue(result, "Should return true because the field exists");
    }
    @Test
    void testHasExistingKeyAndNotNullWhenKeyIsNull() {

        boolean result = JsonUtil.hasExistingKeyAndNotNull(mockJson, null);
        assertFalse(result, "Should return false when key is null");
    }
    @Test
    void testHasExistingKeyAndNotNullWhenObjIsNull() {
        boolean result = JsonUtil.hasExistingKeyAndNotNull(null, "key");
        assertFalse(result, "Should return false when object is null");
    }
    @Test
    void testHasExistingKeyAndNotNullWhenValueIsNull() {
        mockJson.set(KEY, null);

        boolean result = JsonUtil.hasExistingKeyAndNotNull(mockJson, KEY);
        assertFalse(result, "Should return false when the value is null");
    }

    @Test
    void testHasExistingKeyAndNotNullWhenValueIsJsonNull() {
        mockJson.set(KEY, Json.nil());

        boolean result = JsonUtil.hasExistingKeyAndNotNull(mockJson, KEY);
        assertFalse(result, "Should return false when the value is Json type null");
    }
    // endregion hasExistingKeyAndNotNull

    // region getValueBooleanOrDefault
    @Test
    void testGetValueBooleanOrDefaultWhenKeyExistsAndIsBoolean() {
        mockJson.set(KEY, true);

        boolean result = JsonUtil.getValueBooleanOrDefault(mockJson, KEY, false);
        assertTrue(result, "Should return the boolean value when key exists and is boolean");
    }

    @Test
    void testGetValueBooleanOrDefaultWhenKeyExistsAndIsFalseBoolean() {
        mockJson.set(KEY, false);

        boolean result = JsonUtil.getValueBooleanOrDefault(mockJson, KEY, true);
        assertFalse(result, "Should return the boolean value when key exists and is boolean");
    }

    @Test
    void testGetValueBooleanOrDefaultWhenKeyDoesNotExist() {

        boolean result = JsonUtil.getValueBooleanOrDefault(mockJson, KEY, true);
        assertTrue(result, "Should return the default value when key does not exist");
    }

    @Test
    void testGetValueBooleanOrDefaultWhenKeyExistsButNotBoolean() {
        mockJson.set(KEY, "notABoolean");

        boolean result = JsonUtil.getValueBooleanOrDefault(mockJson, KEY, true);
        assertTrue(result, "Should return the default value when key exists but is not boolean");
        result = JsonUtil.getValueBooleanOrDefault(mockJson, KEY, false);
        assertFalse(result, "Should return the default value when key exists but is not boolean");
    }

    @Test
    void testGetValueBooleanOrDefaultWhenKeyExistsButIsNull() {
        mockJson.set(KEY, null);

        boolean result = JsonUtil.getValueBooleanOrDefault(mockJson, KEY, false);
        assertFalse(result, "Should return the default value when key exists but is null");
    }

    @Test
    void testGetValueBooleanOrDefaultWhenKeyIsNull() {
        boolean result = JsonUtil.getValueBooleanOrDefault(Json.object(), null, false);
        assertFalse(result, "Should return the default value when key is null");
        result = JsonUtil.getValueBooleanOrDefault(Json.object(), null, true);
        assertTrue(result, "Should return the default value when key is null");
    }

    // endregion getValueBooleanOrDefault

    // region getValueStringOrDefault
    @Test
    void testGetValueStringOrDefaultWhenKeyExistsAndIsString() {
        mockJson.set(KEY, STRING_VALUE);

        String result = JsonUtil.getValueStringOrDefault(mockJson, KEY,  "x");
        assertEquals(STRING_VALUE, result, "Should return the string value when key exists and is string");
    }

    @Test
    void testGetValueStringOrDefaultWhenKeyDoesNotExist() {
        String result = JsonUtil.getValueStringOrDefault(mockJson, KEY, "w");
        assertEquals("w", result, "Should return the default value when key does not exist");
    }

    @Test
    void testGetValueStringOrDefaultWhenKeyExistsButIsBoolean() {
        mockJson.set(KEY, true);

        String result = JsonUtil.getValueStringOrDefault(mockJson, KEY, STRING_VALUE);
        assertEquals(STRING_VALUE, result,  "Should return the default value when key exists but is boolean");
    }

    @Test
    void testGetValueStringOrDefaultWhenKeyExistsButIsNumber() {
        mockJson.set(KEY, 1);

        String result = JsonUtil.getValueStringOrDefault(mockJson, KEY, STRING_VALUE);
        assertEquals(STRING_VALUE, result,  "Should return the default value when key exists but is a number");
    }

    @Test
    void testGetValueStringOrDefaultWhenKeyExistsButIsList() {
        mockJson.set(KEY, Json.array());

        String result = JsonUtil.getValueStringOrDefault(mockJson, KEY, STRING_VALUE);
        assertEquals(STRING_VALUE, result,  "Should return the default value when key exists but is a JSON list");
    }

    @Test
    void testGetValueStringOrDefaultWhenKeyExistsButIsNull() {
        mockJson.set(KEY, null);

        String result = JsonUtil.getValueStringOrDefault(mockJson, KEY, STRING_VALUE);
        assertEquals(STRING_VALUE, result, "Should return the default value when key exists but is null");
    }

    @Test
    void testGetValueStringOrDefaultWhenKeyIsNull() {
        String result = JsonUtil.getValueStringOrDefault(Json.object(), null, STRING_VALUE);
        assertEquals(STRING_VALUE, result, "Should return the default value when key is null");
    }

    // endregion getValueStringOrDefault

    // region getValueIntegerOrDefault
    @Test
    void testGetValueIntegerOrDefaultWhenKeyExistsAndIsInteger() {
        mockJson.set(KEY, 1);

        Integer result = JsonUtil.getValueIntegerOrDefault(mockJson, KEY,  2);
        assertEquals(1, result, "Should return the integer value when key exists and is number");
    }

    @Test
    void testGetValueIntegerOrDefaultWhenKeyDoesNotExist() {

        Integer result = JsonUtil.getValueIntegerOrDefault(mockJson, KEY, 1);
        assertEquals(1, result, "Should return the default value when key does not exist");
    }

    @Test
    void testGetValueIntegerOrDefaultWhenKeyExistsButIsBoolean() {
        mockJson.set(KEY, true);

        Integer result = JsonUtil.getValueIntegerOrDefault(mockJson, KEY, 1);
        assertEquals(1, result,  "Should return the default value when key exists but is boolean");
    }

    @ParameterizedTest
    @CsvSource({
            "q, 1, Should return the default value when key exists but is not a number",
            "2, 1, Should return the default value when key exists but is a number in string format",
    })
    void testGetValueIntegerOrDefaultWhenValueIsNotNumber(String value, int defaultValue, String message) {
        mockJson.set(KEY, value);

        Integer result = JsonUtil.getValueIntegerOrDefault(mockJson, KEY, defaultValue);
        assertEquals(defaultValue, result,  message);
    }

    @Test
    void testGetValueIntegerOrDefaultWhenKeyExistsButIsList() {
        mockJson.set(KEY, Json.array());

        Integer result = JsonUtil.getValueIntegerOrDefault(mockJson, KEY, 1);
        assertEquals(1, result,  "Should return the default value when key exists but is a JSON list");
    }

    @Test
    void testGetValueIntegerOrDefaultWhenKeyExistsButIsNull() {
        mockJson.set(KEY, null);

        Integer result = JsonUtil.getValueIntegerOrDefault(mockJson, KEY, 1);
        assertEquals(1, result, "Should return the default value when key exists but is null");
    }

    @Test
    void testGetValueIntegerOrDefaultWhenKeyIsNull() {
        Integer result = JsonUtil.getValueIntegerOrDefault(Json.object(), null, 1);
        assertEquals(1, result, "Should return the default value when key is null");
    }

    // endregion getValueIntegerOrDefault

    // region getValueIntegerOrNull
    @Test
    void testGetValueIntegerOrNullWhenKeyExistsAndIsInteger() {
        mockJson.set(KEY, 1);

        Integer result = JsonUtil.getValueIntegerOrNull(mockJson, KEY);
        assertEquals(1, result, "Should return the integer value when key exists and is number");
    }

    @Test
    void testGetValueIntegerOrNullWhenKeyDoesNotExist() {

        Integer result = JsonUtil.getValueIntegerOrNull(mockJson, KEY);
        assertNull(result, "Should return the default value when key does not exist");
    }

    @Test
    void testGetValueIntegerOrNullWhenKeyExistsButIsBoolean() {
        mockJson.set(KEY, true);

        Integer result = JsonUtil.getValueIntegerOrNull(mockJson, KEY);
        assertNull(result,  "Should return the default value when key exists but is boolean");
    }

    @ParameterizedTest
    @CsvSource({
            "q, Should return the default value when key exists but is a number",
            "2, Should return the default value when key exists but is a number in string format",
    })
    void testGetValueIntegerOrNullWhenResultIsNull(String value, String message) {
        mockJson.set(KEY, value);

        Integer result = JsonUtil.getValueIntegerOrNull(mockJson, KEY);
        assertNull(result,  message);
    }

    @Test
    void testGetValueIntegerOrNullWhenKeyExistsButIsList() {
        mockJson.set(KEY, Json.array());

        Integer result = JsonUtil.getValueIntegerOrNull(mockJson, KEY);
        assertNull(result,  "Should return the default value when key exists but is a JSON list");
    }

    @Test
    void testGetValueIntegerOrNullWhenKeyExistsButIsNull() {
        mockJson.set(KEY, null);

        Integer result = JsonUtil.getValueIntegerOrNull(mockJson, KEY);
        assertNull(result, "Should return the default value when key exists but is null");
    }

    @Test
    void testGetValueIntegerOrNullWhenKeyIsNull() {
        Integer result = JsonUtil.getValueIntegerOrNull(Json.object(), null);
        assertNull(result, "Should return the default value when key is null");
    }

    // endregion getValueIntegerOrNull

    // region getValueStringOrNull
    @Test
    void testGetValueStringOrNullWhenKeyExistsAndIsString() {
        mockJson.set(KEY, STRING_VALUE);

        String result = JsonUtil.getValueStringOrNull(mockJson, KEY);
        assertEquals(STRING_VALUE, result, "Should return the integer value when key exists and is string");
    }

    @Test
    void testGetValueStringOrNullWhenKeyDoesNotExist() {

        String result = JsonUtil.getValueStringOrNull(mockJson, KEY);
        assertNull(result, "Should return the default value when key does not exist");
    }

    @Test
    void testGetValueStringOrNullWhenKeyExistsButIsBoolean() {
        mockJson.set(KEY, true);

        String result = JsonUtil.getValueStringOrNull(mockJson, KEY);
        assertNull(result,  "Should return the default value when key exists but is boolean");
    }

    @Test
    void testGetValueStringOrNullWhenKeyExistsButIsNumber() {
        mockJson.set(KEY, 1);

        String result = JsonUtil.getValueStringOrNull(mockJson, KEY);
        assertNull(result,  "Should return the default value when key exists but is a number");
    }

    @Test
    void testGetValueStringOrNullWhenKeyExistsButIsList() {
        mockJson.set(KEY, Json.array());

        String result = JsonUtil.getValueStringOrNull(mockJson, KEY);
        assertNull(result,  "Should return the default value when key exists but is a JSON list");
    }

    @Test
    void testGetValueStringOrNullWhenKeyExistsButIsNull() {
        mockJson.set(KEY, null);

        String result = JsonUtil.getValueStringOrNull(mockJson, KEY);
        assertNull(result, "Should return the default value when key exists but is null");
    }

    @Test
    void testGetValueStringOrNullWhenKeyIsNull() {
        String result = JsonUtil.getValueStringOrNull(Json.object(), null);
        assertNull(result, "Should return the default value when key is null");
    }

    // endregion getValueStringOrNull

    // region getValueBooleanOrNull
    @Test
    void testGetValueBooleanOrNullWhenKeyExistsAndIsBoolean() {
        mockJson.set(KEY, true);

        Boolean result = JsonUtil.getValueBooleanOrNull(mockJson, KEY);
        assertTrue( result, "Should return the integer value when key exists and is string");
    }

    @Test
    void testGetValueBooleanOrNullWhenKeyDoesNotExist() {

        Boolean result = JsonUtil.getValueBooleanOrNull(mockJson, KEY);
        assertNull(result, "Should return the default value when key does not exist");
    }

    @Test
    void testGetValueBooleanOrNullWhenKeyExistsButIsString() {
        mockJson.set(KEY, STRING_VALUE);

        Boolean result = JsonUtil.getValueBooleanOrNull(mockJson, KEY);
        assertNull(result,  "Should return the default value when key exists but is boolean");
    }

    @Test
    void testGetValueBooleanOrNullWhenKeyExistsButIsNumber() {
        mockJson.set(KEY, 1);

        Boolean result = JsonUtil.getValueBooleanOrNull(mockJson, KEY);
        assertNull(result,  "Should return the default value when key exists but is a number");
    }

    @Test
    void testGetValueBooleanOrNullWhenKeyExistsButIsList() {
        mockJson.set(KEY, Json.array());

        Boolean result = JsonUtil.getValueBooleanOrNull(mockJson, KEY);
        assertNull(result,  "Should return the default value when key exists but is a JSON list");
    }

    @Test
    void testGetValueBooleanOrNullWhenKeyExistsButIsNull() {
        mockJson.set(KEY, null);

        Boolean result = JsonUtil.getValueBooleanOrNull(mockJson, KEY);
        assertNull(result, "Should return the default value when key exists but is null");
    }

    @Test
    void testGetValueBooleanOrNullWhenKeyIsNull() {
        Boolean result = JsonUtil.getValueBooleanOrNull(Json.object(), null);
        assertNull(result, "Should return the default value when key is null");
    }

    // endregion getValueBooleanOrNull

    // region getValueInteger
    @Test
    void testGetValueIntegerWhenKeyExists() {
        mockJson.set(KEY, 1);

        Integer result = JsonUtil.getValueInteger(mockJson, KEY);
        assertEquals( 1, result, "Should return the integer value when key exists and is integer.");
    }
    @Test
    void testGetValueIntegerWhenKeyDoesNotExist() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            JsonUtil.getValueInteger(mockJson, KEY);
        });
        assertEquals( KEY, exception.getMessage(), "Should throw exception when key doe snot exists.");
    }
    // endregion getValueInteger
}
