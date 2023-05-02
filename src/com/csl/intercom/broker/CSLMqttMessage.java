package com.csl.intercom.broker;

import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;

public class CSLMqttMessage {
    /**
     * The error codes available to MQTT messages.
     */
    public enum Error {
        NONE(0),
        ERROR(1);

        private final int value;

        Error(int value) {
            this.value = value;
        }

        /**
         * Get the error code.
         *
         * @return The error code.
         */
        public int getValue() {
            return value;
        }

        /**
         * Check if we have an actual error.
         *
         * @return true if it represents an actual error, false otherwise.
         */
        public boolean isError() {
            return this != NONE;
        }

        /**
         * Get an {@link Error} from its code. For parsing purposes.
         *
         * @param value The error code.
         * @return The {@link Error} containing the given error code. If the error code does not exist, defaults to NONE.
         */
        public static Error fromValue(int value) {
            for (Error e : Error.values()) {
                if (e.getValue() == value) {
                    return e;
                }
            }
            return NONE;
        }
    }

    private String message;
    private Error error;
    private String errorMsg;

    /**
     * Create an empty {@link CSLMqttMessage}.
     */
    public CSLMqttMessage() {
        message = null;
        error = Error.NONE;
        errorMsg = null;
    }

    /**
     * Create an MQTT message with a specified message string.
     * @param message The message {@link String} to fill the new {@link CSLMqttMessage} with.
     * @return A new {@link CSLMqttMessage} with the specified message {@link String}.
     */
    public static CSLMqttMessage message(String message) {
        return new CSLMqttMessage().setMessage(message);
    }

    /**
     * Create an MQTT message with a specified error.
     * @param error The message {@link Error} to fill the new {@link CSLMqttMessage} with.
     * @return A new {@link CSLMqttMessage} with the specified {@link Error}.
     */
    public static CSLMqttMessage error(Error error) {
        return new CSLMqttMessage().setError(error);
    }

    /**
     * Create an MQTT message with a specified error and error message.
     * @param error The message {@link Error} to fill the new {@link CSLMqttMessage} with.
     * @param message The message's error message.
     * @return A new {@link CSLMqttMessage} with the specified {@link Error} and error message.
     */
    public static CSLMqttMessage error(Error error, String message) {
        return new CSLMqttMessage().setError(error).setErrorMessage(message);
    }

    /**
     * Parse a message as received through MQTT.
     * @param message The message to parse, in {@link Json} format.
     * @return The read created {@link CSLMqttMessage}.
     */
    public static CSLMqttMessage parse(Json message) {
        CSLMqttMessage result = new CSLMqttMessage()
                .setError(Error.fromValue(JsonUtil.getIntFromJson(message, "error", 0)))
                .setErrorMessage(JsonUtil.getStringFromJson(message, "error_message", null));
        if (message.has("results")) {
            Json results = message.get("results");
            if (results.isString()) {
                result.setMessage(results.asString());
            } else {
                result.setMessage(results.toString());
            }
        }
        return result;
    }

    /**
     * Parse a message as received through MQTT.
     * @param message The message to parse, in a {@link String} with JSON format.
     * @return The read created {@link CSLMqttMessage}.
     */
    public static CSLMqttMessage parse(String message) {
        return parse(Json.read(message));
    }

    public CSLMqttMessage setMessage(String message) {
        this.message = message;
        return this;
    }

    public CSLMqttMessage setError(Error error) {
        this.error = error;
        return this;
    }

    public CSLMqttMessage setErrorMessage(String errorMsg) {
        this.errorMsg = errorMsg;
        return this;
    }

    public boolean isError() {
        return error.isError();
    }

    public String getResults() {
        return message;
    }

    public String getErrorMessage() {
        return errorMsg;
    }

    @Override
    public String toString() {
        if (error.isError()) {
            Json result = Json.object("error", error.getValue());
            if (errorMsg != null && !errorMsg.equals("")) {
                result.set("error_message", errorMsg);
            }
            return result.asString();
        } else {
            return Json.object("results", message).toString();
        }
    }
}
