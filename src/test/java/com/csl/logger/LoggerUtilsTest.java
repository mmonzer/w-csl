package com.csl.logger;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LoggerUtilsTest {
    @Test
    void hideNullPassword() {
        String password = null;
        String expectedOutput = "null";
        assertEquals(expectedOutput, LoggerUtils.hide(password));
    }
    @Test
    void hideShortPassword() {
        String password = "pa";
        String expectedOutput = "pa******";
        assertEquals(expectedOutput, LoggerUtils.hide(password));
    }
    @Test
    void hideEmptyPassword() {
        String password = "";
        String expectedOutput = "********";
        assertEquals(expectedOutput, LoggerUtils.hide(password));
    }
    @Test
    void hideLongPassword() {
        String password = "password123";
        String expectedOutput = "pas*****";
        assertEquals(expectedOutput, LoggerUtils.hide(password));
    }
}