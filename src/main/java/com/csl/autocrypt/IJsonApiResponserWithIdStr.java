package com.csl.autocrypt;

import com.ucsl.json.Json;
import main.services.JsonApiResponse;

public interface IJsonApiResponserWithIdStr {
    public JsonApiResponse apply(String id, String name, Json callback);
}
