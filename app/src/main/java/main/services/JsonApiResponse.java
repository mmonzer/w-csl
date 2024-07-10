package main.services;

import com.ucsl.json.Json;

/**
 * Class to harmonize API responses with a common format.
 */
public class JsonApiResponse {
    /**
     * Subclass to hold errors.
     */
    public static class Error {
        private String reason;
        private Json details;

        public Error(String reason, Json details) {
            this.reason = reason;
            this.details = details;
        }

        public String getReason() {
            return reason;
        }

        public Json getDetails() {
            return details;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public void setDetails(Json details) {
            this.details = details;
        }

        public Json toJson() {
            Json result = Json.object(
                    "reason", this.reason
            );
            if (this.details != null) {
                result.set("details", this.details);
            }
            return result;
        }
    }
    private Json result;
    private Json extra;  // Extra information, that is not included in the serialisation.
    private Error error;

    private JsonApiResponse(Json result, Json extra, Error error) {
        this.result = result;
        this.extra = extra;
        this.error = error;
    }

    /**
     * Checks if the response is a success or a failure.
     *
     * @return true if the message holds a success message, false otherwise.
     */
    public boolean isSuccess() {
        return this.error == null;
    }

    /**
     * Get the result in a response.
     *
     * @return The result of the message.
     */
    public Json getResult() {
        if (!isSuccess()) {
            return null;
        }
        return result;
    }

    /**
     * Get the extra information in a message response.
     *
     * @return The extra information contained in the message.
     */
    public Json getExtra() {
        return extra;
    }

    public Error getError() {
        return error;
    }

    /**
     * Create a {@link Json} object suitable fo sending as a response to an API call.
     *
     * @return The serialized response in a {@link Json} object.
     */
    public Json toJson() {
        boolean isSuccess = this.isSuccess();
        Json res = Json.object(
                "success", isSuccess
        );
        if (isSuccess) {
            if (this.result != null) {
                res.set("result", result);
            }
        } else {
            res.set("error", this.error.toJson());
        }
        return res;
    }

    /**
     * Create an Api response with a successful result.
     *
     * @param result The result of the message.
     * @return The newly created response.
     */
    static public JsonApiResponse result(Json result, Json extra) {
        return new JsonApiResponse(result, extra, null);
    }

    static public JsonApiResponse result(Json result) {
        return JsonApiResponse.result(result, null);
    }

    static public JsonApiResponse success() {
        return JsonApiResponse.result(null, null);
    }

    /**
     * Create an error message.
     *
     * @param reason The reason of the error.
     * @param details The details that caused the error.
     * @return The newly created error message.
     */
    static public JsonApiResponse error(String reason, Json details) {
        return new JsonApiResponse(null, null, new Error(reason, details));
    }

    /**
     * Create an error message.
     *
     * @param reason The reason of the error.
     * @return The newly created error message.
     */
    static public JsonApiResponse error(String reason) {
        return error(reason, null);
    }
}
