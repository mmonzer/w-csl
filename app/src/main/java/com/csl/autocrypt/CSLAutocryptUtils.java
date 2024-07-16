package com.csl.autocrypt;

import com.csl.autocrypt.enums.AutocryptConstants;
import com.ucsl.json.Json;
import main.services.JsonApiResponse;
import org.jetbrains.annotations.NotNull;

import static com.csl.autocrypt.enums.AutocryptConstants.Common;

public class CSLAutocryptUtils {

    /**
     * Reformats the AutoCrypt error to resend only the user-friendly error
     *
     * @param response raw response
     * @return reformated response
     */
    public static @NotNull JsonApiResponse reformatAutoCryptError(JsonApiResponse response) {
        String errorMessage = Json.read(response.getError().getDetails().get("content").toString()).toString();
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

    /**
     * Converts the ttl in seconds to hours in format "xxxh".
     */
    public static void convertTTLSecondsToStrHours(Json obj) {
        if (!obj.isNull() && obj.has(Common.TTL) && obj.get(Common.TTL).isNumber()) {
            obj.set(Common.TTL, obj.get(Common.TTL).asInteger() / 3600 + "h");
        }
    }
}
