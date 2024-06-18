package main.services;

import com.ucsl.json.Json;
import lombok.Getter;
import lombok.Setter;

/**
 * Class to harmonize API responses with a common format.
 */
@Getter
@Setter
public class JsonApiResponse {
    /**
     * Subclass to hold errors.
     */
    @Setter
    @Getter
    public static class Error {
        private String reason;
        @Getter
        private Json details;

        public Error(String reason, Json details) {
            this.reason = reason;
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
    private final Json result;
    private Json extra;
    private final Error error;

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
    @Override
    public String toString() {
        return this.toJson().toString();
    }
}
