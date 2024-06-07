package com.csl.autocrypt;

import com.ucsl.json.Json;
import main.services.JsonApiResponse;
import org.jetbrains.annotations.NotNull;

public class CSLAutocryptUtils {

    /**
     * Reformats the AutoCrypt error to resend only the user-friendly error
     *
     * @param response raw response
     * @return reformated response
     */
    public static @NotNull JsonApiResponse reformatAutoCryptError(JsonApiResponse response) {
        String errorMessage = Json.read(response.getError().getDetails().asJsonMap().get("content").asString()).get("message").asString();
        return JsonApiResponse.error(errorMessage);
    }

    /**
     * Reformats the AutoCrypt response depending on the possible error
     *
     * @param response raw response
     * @return reformated response
     */
    public static JsonApiResponse cleanApiResponse(JsonApiResponse response) {
        if (response.isSuccess()) {
            return response;
        } else {
            try {
                return reformatAutoCryptError(response);
            } catch (Exception ignored) {
                return response;
            }
        }
    }
}
